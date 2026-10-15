// Package tsbridge bettet einen Tailscale-Knoten (tsnet) in die Host-App ein und macht den lokalen
// Ktor-Server über Tailscale Funnel öffentlich erreichbar.
//
// Ablauf: tsnet meldet sich mit dem Auth-Key an, ListenFunnel(":443") liefert einen Listener, dessen
// Verbindungen bereits TLS-entschlüsselt sind (Zertifikat für <name>.<tailnet>.ts.net holt tsnet selbst).
// Ein Reverse-Proxy reicht jede Anfrage – auch WebSockets – an http://127.0.0.1:<port> weiter und setzt
// X-Forwarded-For auf die echte Absender-IP.
//
// Wird per gomobile als .aar gebaut; die exportierten Funktionen sind von Kotlin aus aufrufbar
// (Paket go `tsbridge` → Klasse `tsbridge.Tsbridge`).
package tsbridge

import (
	"context"
	"crypto/tls"
	"encoding/json"
	"errors"
	"fmt"
	"net"
	"net/http"
	"net/http/httputil"
	"net/netip"
	"net/url"
	"strings"
	"sync"
	"time"

	_ "tailscale.com/feature/androidbin" // Netzwerkschnittstellen, DNS und CA-Zertifikate unter Android
	"tailscale.com/ipn"
	"tailscale.com/tsnet"
)

type state struct {
	State  string `json:"state"`            // stopped, starting, running, error
	Domain string `json:"domain,omitempty"` // z. B. classview.tail4b2c1.ts.net
	Error  string `json:"error,omitempty"`
}

var (
	mu      sync.Mutex
	srv     *tsnet.Server
	httpSrv *http.Server
	current = state{State: "stopped"}
	logs    = newRing(300)
)

type srcKey struct{}

// Start startet Tailscale und Funnel im Hintergrund und kehrt sofort zurück. Den Fortschritt liefert
// Status(). authKey wird nur bei der ersten Anmeldung gebraucht; danach liegt der Knotenschlüssel in
// stateDir.
func Start(stateDir, authKey, hostname string, targetPort int) error {
	mu.Lock()
	defer mu.Unlock()
	if srv != nil {
		return errors.New("läuft bereits")
	}
	target, err := url.Parse(fmt.Sprintf("http://127.0.0.1:%d", targetPort))
	if err != nil {
		return err
	}
	s := &tsnet.Server{
		Dir:      stateDir,
		Hostname: hostname,
		AuthKey:  strings.TrimSpace(authKey),
		Logf:     logs.logf,
		UserLogf: logs.logf,
	}
	srv = s
	current = state{State: "starting"}
	go run(s, target)
	return nil
}

func run(s *tsnet.Server, target *url.URL) {
	ln, err := s.ListenFunnel("tcp", ":443", tsnet.FunnelOnly())
	if err != nil {
		setState(state{State: "error", Error: explain(err)})
		return
	}
	domain := ""
	if d := s.CertDomains(); len(d) > 0 {
		domain = d[0]
	}

	proxy := &httputil.ReverseProxy{
		Rewrite: func(pr *httputil.ProxyRequest) {
			pr.SetURL(target)
			pr.Out.Host = pr.In.Host
			pr.SetXForwarded() // nutzt pr.In.RemoteAddr, den wir unten auf die echte Absender-IP setzen
		},
		ErrorHandler: func(w http.ResponseWriter, r *http.Request, err error) {
			logs.logf("proxy: %v", err)
			http.Error(w, "Server in der Host-App nicht erreichbar", http.StatusBadGateway)
		},
	}
	handler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if src, ok := r.Context().Value(srcKey{}).(netip.AddrPort); ok && src.IsValid() {
			r.RemoteAddr = src.String()
		}
		proxy.ServeHTTP(w, r)
	})
	hs := &http.Server{
		Handler:           handler,
		ReadHeaderTimeout: 15 * time.Second,
		ConnContext: func(ctx context.Context, c net.Conn) context.Context {
			if tc, ok := c.(*tls.Conn); ok {
				if fc, ok := tc.NetConn().(*ipn.FunnelConn); ok {
					return context.WithValue(ctx, srcKey{}, fc.Src)
				}
			}
			return ctx
		},
	}
	mu.Lock()
	if srv != s { // inzwischen gestoppt
		mu.Unlock()
		ln.Close()
		return
	}
	httpSrv = hs
	current = state{State: "running", Domain: domain}
	mu.Unlock()
	logs.logf("Funnel aktiv: https://%s → %s", domain, target)

	if err := hs.Serve(ln); err != nil && !errors.Is(err, http.ErrServerClosed) {
		setState(state{State: "error", Domain: domain, Error: err.Error()})
	}
}

// Stop beendet Funnel und den Tailscale-Knoten. Die Anmeldung bleibt in stateDir erhalten.
func Stop() {
	mu.Lock()
	s, hs := srv, httpSrv
	srv, httpSrv = nil, nil
	current = state{State: "stopped"}
	mu.Unlock()
	if hs != nil {
		ctx, cancel := context.WithTimeout(context.Background(), 3*time.Second)
		hs.Shutdown(ctx)
		cancel()
	}
	if s != nil {
		s.Close()
	}
}

// Status liefert den Zustand als JSON, z. B. {"state":"running","domain":"classview.tail….ts.net"}.
func Status() string {
	mu.Lock()
	defer mu.Unlock()
	b, _ := json.Marshal(current)
	return string(b)
}

// Logs liefert die letzten Logzeilen (für die Diagnose-Ansicht der Host-App). Enthält keine Schlüssel.
func Logs() string {
	return logs.String()
}

func setState(st state) {
	mu.Lock()
	defer mu.Unlock()
	if srv != nil {
		current = st
	}
	if st.Error != "" {
		logs.logf("Fehler: %s", st.Error)
	}
}

// explain übersetzt die häufigsten Einrichtungsfehler in verständliche Hinweise.
func explain(err error) string {
	msg := err.Error()
	switch {
	case strings.Contains(msg, "HTTPS must be enabled"):
		return "HTTPS-Zertifikate sind im Tailscale-Adminbereich nicht eingeschaltet (DNS → HTTPS Certificates)."
	case strings.Contains(strings.ToLower(msg), "funnel"):
		return "Funnel ist für dieses Gerät nicht erlaubt. Im Tailscale-Adminbereich Funnel aktivieren. (" + msg + ")"
	case strings.Contains(msg, "invalid key") || strings.Contains(msg, "authkey"):
		return "Der Auth-Key ist ungültig oder abgelaufen. Bitte einen neuen erzeugen. (" + msg + ")"
	}
	return msg
}

// ring ist ein kleiner Ringpuffer für Logzeilen.
type ring struct {
	mu    sync.Mutex
	lines []string
	max   int
}

func newRing(max int) *ring { return &ring{max: max} }

func (r *ring) logf(format string, args ...any) {
	line := time.Now().Format("15:04:05 ") + fmt.Sprintf(format, args...)
	if strings.Contains(line, "tskey-") { // Auth-Keys nie protokollieren
		return
	}
	r.mu.Lock()
	defer r.mu.Unlock()
	r.lines = append(r.lines, line)
	if len(r.lines) > r.max {
		r.lines = r.lines[len(r.lines)-r.max:]
	}
}

func (r *ring) String() string {
	r.mu.Lock()
	defer r.mu.Unlock()
	return strings.Join(r.lines, "\n")
}
