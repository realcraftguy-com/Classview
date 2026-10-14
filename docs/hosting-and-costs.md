# Hosting, Kosten und Testbetrieb

Stand: Phase 1, Revision 3. Gehostet wird alles von der **Host-App auf dem Galaxy S21 FE 5G zuhause**
(Vodafone-Anschluss, FritzBox) – ohne Root und ohne Mietserver. iPad und Tablet in der Schule erreichen es
übers Internet unter einer festen, kostenlosen DuckDNS-Adresse. Details zur Technik: `architecture.md`
§4.1, §5 und §10.

---

## 1. Was läuft wo

| Ort | Gerät | App | Aufgabe |
|---|---|---|---|
| Zuhause | **Galaxy S21 FE 5G** (One UI 8) | **Host-APK** („ClassroomView Host“) | Server (REST + WebSocket), liefert die Lehrer-Website aus, speichert Klassen und Profilbilder, hält DuckDNS-Adresse und Zertifikat aktuell |
| Zuhause | FritzBox | – | leitet Port 443 von außen an das S21 FE (Port 8443) weiter |
| Schule | **Xiaomi Pad 7** | **Schüler-APK** („ClassroomView“) | Profil, Beitritt, Freigabe, sendet das Video direkt ans iPad |
| Schule | **iPad der Lehrerin** | keine App – **Safari** bzw. Home-Bildschirm-Verknüpfung | Lehrer-Oberfläche, empfängt das Video |

Datenwege:

- **Steuerdaten** (Beitritt, Liste, Anfragen, WebRTC-Signalisierung, Profilbilder): Schule → Internet →
  FritzBox → S21 FE. Wenige KB/s.
- **Video**: nur innerhalb des Schul-WLANs, Tablet → iPad. Belastet deinen Heimanschluss nicht.

---

## 2. Kosten

| Posten | Kosten |
|---|---|
| Server | **0 €** – das S21 FE |
| Adresse (DuckDNS-Subdomain) | **0 €** |
| Zertifikat (Let's Encrypt) | **0 €** |
| Strom fürs S21 FE im Dauerbetrieb | wenige Euro pro Jahr |
| Datenvolumen Heimanschluss | vernachlässigbar (kein Video) |
| GitHub Actions zum Bauen der APKs | 0 € im Rahmen der Freiminuten |

---

## 3. Voraussetzung: Ist dein Anschluss von außen erreichbar?

Das muss **vor Phase 3** geklärt sein (siehe `architecture.md` §5.1):

1. *FritzBox → Internet → Online-Monitor*: „DS-Lite“ oder normale IPv4-Adresse?
   - normale IPv4 → passt.
   - DS-Lite → weiter mit 2.
2. Auf dem Pad 7 **im Schul-WLAN** `test-ipv6.com` öffnen.
   - IPv6 funktioniert → passt (Freigabe läuft dann über IPv6).
   - kein IPv6 → Ausweichmöglichkeiten in `architecture.md` §5.5.

---

## 4. Einrichtung (Kurzfassung, ausführlich ab Phase 3)

1. **DuckDNS:** auf duckdns.org anmelden, Subdomain anlegen (z. B. `meineklasse`), Token kopieren.
2. **S21 FE:** Host-APK installieren (aus GitHub), öffnen, Assistent folgen: Subdomain + Token eintragen,
   Benachrichtigungen erlauben, Akku-Ausnahme bestätigen, Samsung-Einstellungen laut Hinweis
   (`architecture.md` §10). Dauerhaft ans Ladegerät.
3. **FritzBox:** Portfreigabe extern 443 → S21 FE Port 8443 (TCP), S21 FE immer gleiche IP
   (`architecture.md` §5.1).
4. Die Host-App holt das Zertifikat und zeigt „Von außen erreichbar“ an, sobald alles passt.
5. **Lehrerin:** bekommt den **Einladungslink** aus der Host-App (z. B. per Nachricht). Öffnet ihn in Safari
   → *Teilen → Zum Home-Bildschirm*. Mehr muss sie nicht einrichten.
6. **Pad 7:** Schüler-APK installieren, HyperOS-Hinweise abarbeiten (`architecture.md` §9.3), Profil anlegen,
   Code eingeben.

APKs: GitHub Actions baut bei jedem Push beide APKs. Du lädst sie auf dem jeweiligen Gerät herunter und
erlaubst einmalig „Installation aus unbekannten Quellen“ für Browser bzw. Dateimanager.

---

## 5. Entwickeln und Testen ohne Handy

Der Server ist ein reines Kotlin/JVM-Modul und läuft auch auf einem PC:

- `./gradlew :backend:server:run` startet ihn lokal, im Debug-Modus ohne TLS.
- Web-App per `npm run dev` (Vite) mit Weiterleitung von `/api` und `/ws` auf den Server.
- Die automatischen Server-Tests laufen auf dem PC und in GitHub Actions.
- Die Schüler-App im Debug-Build kann eine andere Server-Adresse (auch `http://`) nutzen.

---

## 6. Typische Stolpersteine

| Problem | Ursache / Lösung |
|---|---|
| In der Schule „Server nicht erreichbar“ | S21 FE aus, nicht im WLAN oder schläft → Ladegerät, Samsung-Akku-Einstellungen; Internet zuhause gestört; Portfreigabe fehlt |
| Funktionierte, plötzlich nicht mehr | Vodafone hat die IP gewechselt und DuckDNS ist noch nicht aktualisiert → Host-App meldet alle 5 min; Status in der Host-App prüfen |
| Funktioniert zuhause, aber nicht in der Schule | DS-Lite und Schul-WLAN ohne IPv6 → `architecture.md` §5.5 |
| Zertifikatsfehler in Safari | Zertifikat abgelaufen (Erneuerung fehlgeschlagen) → Status in der Host-App; DuckDNS-Token prüfen |
| Anfragen kommen am Tablet nicht an | HyperOS hat die Schüler-App beendet → Autostart, „Keine Einschränkungen“, App sperren |
| Kein Bild, obwohl „Freigabe läuft“ | iPad und Tablet nicht im selben Schul-WLAN, oder Safari findet ohne STUN keinen Weg (Prüfpunkt Phase 7) |
