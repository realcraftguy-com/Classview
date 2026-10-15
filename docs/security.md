# Sicherheit

Stand: Phase 1, Revision 4 (Host-App auf dem Galaxy S21 FE zuhause, öffentlich erreichbar über Tailscale Funnel). Die Checkliste am Ende wird in jeder Phase fortgeschrieben und in Phase 10
abgeschlossen.

---

## 1. Was geschützt wird

| Schutzgut | Warum |
|---|---|
| Bildschirminhalt der Schüler | höchstes Gut; darf nur mit doppelter Zustimmung und nur live übertragen werden |
| Namen und Profilbilder | personenbezogene Daten, oft von Minderjährigen |
| Lehrer-Token, Wiederherstellungslink | wer sie hat, steuert die Klasse |
| Geräte-Token | wer es hat, tritt als dieser Schüler auf |

**Beteiligte und Angreifer:** Lehrerin; Schüler der Klasse; Außenstehende, die einen Code kennen oder raten;
**jeder im Internet** (der Server ist öffentlich erreichbar, automatische Scanner finden ihn schnell); jemand
im selben WLAN; wer das Host-Handy (S21 FE) in der Hand hat.

---

## 2. Identitäten und Geheimnisse

| Geheimnis | Erzeugung | Speicherort Client | Speicherort Server | Berechtigt zu |
|---|---|---|---|---|
| **Klassencode** | 6 Zeichen aus 31 Zeichen (`23456789ABCDEFGHJKMNPQRSTUVWXYZ`), `SecureRandom` | – (wird abgeschrieben) | Klartext (muss nachschlagbar sein) | **nur Beitreten**. Nie Zugriff auf Bildschirm, Liste oder Bilder. |
| **Lehrer-Token** | 32 Byte `SecureRandom`, base64url | `localStorage` der Web-App | SHA-256-Hash | Klasse verwalten, Freigaben anfragen, Schüler entfernen, Code erneuern, Profilbilder abrufen |
| **Wiederherstellungs-Token** | 32 Byte CSPRNG | im Link `https://…/#/recover/<token>` und lokal in der Web-App | SHA-256-Hash | ein **neues** Lehrer-Token für ein weiteres Gerät erzeugen |
| **Geräte-ID** | UUIDv4 | DataStore | Klartext | keine – nur Zuordnung |
| **Geräte-Token** | 32 Byte CSPRNG | DataStore (App-privat, nicht im Backup) | SHA-256-Hash | als dieses Gerät verbinden, eigenes Profil ändern, auf Anfragen antworten |
| **Host-Einladungsschlüssel** | 32 Byte `SecureRandom`, in der Host-App erneuerbar | im Einladungslink (Fragment) und `localStorage` der Lehrer-Web-App | SHA-256-Hash | **Klassen anlegen** – sonst könnte jeder im Internet Klassen auf deinem Server anlegen |
| **Tailscale-Auth-Key** | im Tailscale-Adminbereich erzeugt, einmalig in die Host-App kopiert | – | nach der Anmeldung nicht mehr nötig; Knotenschlüssel im App-privaten Speicher | ein Gerät in deinem Tailnet anmelden – Auth-Key nach Gebrauch im Adminbereich widerrufen bzw. Einmal-Key verwenden |
| **TLS-Schlüssel** (von `tsnet` verwaltet) | auf dem S21 FE erzeugt | – | App-privater Speicher, verlässt das Gerät nie | Server-Identität für `classview.….ts.net` |

Der Wiederherstellungs-Token steht im **URL-Fragment** (`#…`). Fragmente werden von Browsern nicht an den
Server gesendet, landen also weder in Server- noch in Proxy-Logs und nicht im `Referer`.

Codes: 31⁶ ≈ 887 Mio. Kombinationen. Bei z. B. 100 aktiven Klassen trifft ein zufälliger Versuch mit
Wahrscheinlichkeit ≈ 1 : 8,9 Mio. Zusammen mit den Rate-Limits ist Durchprobieren praktisch aussichtslos.

---

## 3. Regeln und wie sie umgesetzt werden

| Regel | Umsetzung |
|---|---|
| Code nur zum Beitreten | Code-Endpunkt `POST /api/join` liefert nur ein eigenes Geräte-Token; alle anderen Endpunkte verlangen Lehrer- oder Geräte-Token |
| Klassen anlegen nur mit Einladung | `POST /api/classes` verlangt den Host-Einladungsschlüssel; ohne ihn sieht ein Fremder nur die Startseite |
| Code widerrufbar, erneuerbar, optional ablaufend | „Code erneuern“ (alter sofort ungültig), „Beitritt pausieren“, Ablaufzeit wählbar; bereits beigetretene Geräte verbinden sich mit ihrem Token und sind nicht betroffen |
| Rate-Limits beim Beitritt | Werte in `backend/server/.../RateLimiter.kt` (`Limits`): fehlgeschlagene Beitritte **30 je IP in 10 min** (nicht strenger, weil alle Geräte einer Schule sich eine öffentliche IP teilen und Tippfehler zusammenzählen), Beitrittsversuche 60 je IP und 60 je Code pro Minute (eine ganze Klasse tritt oft gleichzeitig bei), global 300 Fehlversuche pro Minute, max. 40 Geräte je Klasse; Klassen anlegen 10 je IP und Stunde; Wiederherstellung 10 Fehlversuche je IP in 10 min; Profilbilder 10 je Gerät und Minute. Bei 31⁶ Codes sind 30 Versuche je 10 Minuten aussichtslos. Antwort 429 mit `Retry-After`. Zähler liegen im Speicher und beginnen nach Neustart neu. |
| Anfrage nur an genau ein Gerät, serverseitig geprüft | `share.request` prüft: Lehrer-Token gültig und nicht widerrufen → gehört zu `classId` der WS-Verbindung → `deviceId` gehört zu dieser Klasse, ist nicht entfernt, ist online. Nachricht geht nur an die eine WS-Verbindung dieses Geräts. |
| App prüft Zugehörigkeit | App verwirft `share.requested`, wenn `classId` nicht der aktuell verbundenen Klasse entspricht oder `expiresAt` vorbei ist |
| Ablehnen ist endgültig | Endzustände sind unveränderlich (bedingtes `UPDATE … WHERE status='pending'`); App merkt sich beantwortete `requestId`s und reagiert auf keine zweite Nachricht dazu |
| Abbruch im Systemdialog = Ablehnung | `RESULT_CANCELED` → `share.deny {system_dialog_cancelled}` |
| Kein Spam an Schüler | max. eine offene Anfrage pro Gerät; nach einer Ablehnung 10 s Pause, bevor dasselbe Gerät erneut gefragt werden kann |
| Keine Aufnahme/Speicherung | Medien laufen nie durch den Server, sondern direkt und verschlüsselt (DTLS-SRTP) vom Tablet zum iPad. Keine Aufzeichnungsfunktion in Web oder App. |
| Keine Kamera, kein Mikrofon | App fordert diese Berechtigungen nicht an; Web-App ruft `getUserMedia` nie auf; WebRTC-Verbindung ist auf Lehrerseite `recvonly` |
| Systemabfrage nicht umgehen | jede Sitzung startet mit `createScreenCaptureIntent()`; Ergebnis-Intent wird nie gespeichert oder wiederverwendet; laufende Benachrichtigung + Android-Statusleisten-Hinweis während der Aufnahme |
| Profilbilder geschützt | nur über `GET …/avatar` mit Lehrer-Token derselben Klasse; keine öffentlichen oder erratbaren URLs; Gerät verkleinert auf ≤ 512 px; Server: max. 1 MB, nur JPEG/PNG/WebP (per Dekodierung geprüft, nicht per Dateiendung), max. 512 × 512, wird neu kodiert und von Metadaten (EXIF/GPS) befreit |
| Stream endet sofort | siehe `architecture.md` §6.3 und §7 |
| Transport | nur HTTPS/WSS; TLS endet auf dem S21 FE (Tailscale Funnel leitet nur den verschlüsselten Strom weiter, `architecture.md` §5.1); normale Zertifikatsprüfung auf allen Geräten. Ktor lauscht nur auf `127.0.0.1`. Klartext-HTTP nur im Debug-Modus auf dem PC. Release-App erlaubt keinen Klartext-Verkehr. |
| Header | `Content-Security-Policy` (nur eigene Skripte, `connect-src 'self'`), `Referrer-Policy: no-referrer`, `X-Content-Type-Options: nosniff`, `frame-ancestors 'none'`, HSTS |
| Logs | Tokens, Codes-Fehlversuche mit vollem Code und Bilder werden nicht geloggt; IPs nur gekürzt bzw. nur für Rate-Limits im Speicher |
| Eingaben | alle REST-Bodies und WS-Nachrichten schema-validiert, Längenbegrenzungen für Namen, max. WS-Nachrichtengröße (z. B. 64 KB; SDP passt hinein) |
| Datensparsamkeit | inaktive Klassen werden nach z. B. 30 Tagen samt Geräten und Bildern gelöscht; „Klasse löschen“ entfernt alles sofort; Sitzungsprotokoll enthält nur Zeiten und Status |

---

## 4. Grenzen eines Systems ohne Benutzerkonten

Diese Punkte sind **bewusste Einschränkungen**, keine Fehler. Sie sollten allen Beteiligten bekannt sein.

### 4.1 Wiederherstellungslink gelangt in falsche Hände

Wer den Link hat, ist für das System **die Lehrerin**. Er kann:

- die Schülerliste mit Namen und Profilbildern sehen,
- Schüler entfernen, den Code erneuern, die Klasse löschen,
- **Freigaben anfragen**.

Er kann **nicht**: einen Bildschirm ohne Zustimmung des Schülers sehen. Der Schüler sieht im Dialog aber
nur Klassen- und Lehrkraftnamen – eine Anfrage vom Angreifer sieht genauso aus wie eine echte.

Gegenmaßnahmen: Link wie ein Passwort behandeln (nicht in Gruppen-Chats posten); „Wiederherstellungslink
erneuern“ macht den alten sofort ungültig; Liste der angemeldeten Lehrer-Geräte mit „Abmelden“;
Schüler sollten nur Anfragen zustimmen, die im Unterricht erwartet werden.

### 4.2 Lehrer-Gerät verloren oder entsperrt in fremden Händen

Das Lehrer-Token liegt im Browser-Speicher. Wer das entsperrte iPad hat, hat die Klasse. Abhilfe: von einem
anderen Gerät per Wiederherstellungslink anmelden, dann das verlorene Gerät abmelden und den Link erneuern.

### 4.3 Token und Link beide verloren

Es gibt kein Konto und keine E-Mail-Wiederherstellung. Die Klasse ist dann nicht mehr verwaltbar und muss
neu erstellt werden; die Schüler treten mit dem neuen Code bei. Die alte Klasse wird nach der
Inaktivitätsfrist automatisch gelöscht.

### 4.4 Safari löscht lokale Daten

Safari (Intelligent Tracking Prevention) kann skriptbaren Speicher einer Website löschen, wenn sie
**7 Tage** nicht aktiv genutzt wurde. Für als Home-Bildschirm-App installierte Web-Apps gilt das nach
Apples Angaben nicht. Empfehlung: Web-App zum Home-Bildschirm hinzufügen **und** den Wiederherstellungslink
sicher aufbewahren (z. B. im Passwort-Manager). Achtung: Safari-Tab und Home-Bildschirm-App haben getrennten
Speicher.

### 4.5 Jeder mit dem Code kann beitreten – unter beliebigem Namen

Es gibt keine Identitätsprüfung. Ein Außenstehender mit dem Code kann als „Max“ beitreten, auch wenn es
schon einen Max gibt. Gegenmaßnahmen: Beitritte erscheinen live; Code nur im Raum zeigen; nach dem Beitritt
aller Schüler **„Beitritt pausieren“** oder Code erneuern; Schüler entfernen; Klassengröße begrenzt.
Doppelte Namen werden in der Web-App markiert.

### 4.6 Geräte-Token wird kopiert

Auf einem gerooteten Gerät oder über ein Backup könnte jemand das Geräte-Token auslesen und sich als dieser
Schüler ausgeben (Name, Bild, und bei einer Anfrage eigenen Bildschirminhalt senden). Zugriff auf Bildschirme
anderer erhält er dadurch nicht. Backups sind deshalb deaktiviert; Verschlüsselung des Tokens mit dem
Android Keystore ist ein optionaler Zusatz (`TODO` für Phase 4/10).

### 4.7 Kein Prüfungs- oder Überwachungswerkzeug

Der Schüler entscheidet, **ob** und **was** er teilt (ganzer Bildschirm oder nur eine App). Die Lehrerin
sieht, was der Schüler freigibt – das System kann nicht garantieren, dass es „der echte Bildschirm“ ist.
Für Prüfungsaufsicht ist es ausdrücklich nicht geeignet.

### 4.8 Vertrauen in das Host-Handy

Der Server läuft auf dem S21 FE. Wer dieses Handy kontrolliert, kontrolliert das System: Er kann alle
Klassen, Namen und Profilbilder einsehen und – weil der Server die WebRTC-Signalisierung inkl. der
DTLS-Fingerabdrücke vermittelt – sich mit einer manipulierten Host-App sogar als Mittelsmann in einen
Stream einschleusen. Gegenüber dem WLAN und anderen Geräten ist das Video verschlüsselt, gegenüber einem
bösartigen Host nicht.

In deinem Aufbau gehört das Host-Handy dir selbst (also der Schülerseite). Für ein Privatprojekt ist das in
Ordnung; in einer echten Schule sollte der Host bei der Lehrkraft oder der Schule liegen. Das S21 FE sollte
eine Bildschirmsperre haben und aktuell gehalten werden.

### 4.9 Server zuhause, offen im Internet

- Über Tailscale Funnel ist der Server **für jeden im Internet erreichbar** – das ist nötig, damit die
  Schule ihn erreicht. Die Adresse steht in öffentlichen Zertifikatsprotokollen und wird von Scannern
  gefunden.
- Ohne Code, Token oder Einladungsschlüssel kann ein Fremder nur die Startseite laden und Beitrittscodes
  raten (rate-limitiert, siehe §3; Achtung: alle Geräte der Schule teilen sich eine IP, siehe
  `architecture.md` §5.2).
- Erreichbar ist **nur dieser eine Dienst**, nicht das Handy selbst und nicht dein Heimnetz. Die Host-App
  läuft in Androids App-Sandbox; eine Sicherheitslücke im Server würde zuerst nur die Daten dieser App
  betreffen.
- **Abhängigkeit von Tailscale:** Wer dein Tailscale-Konto übernimmt, könnte den Knoten umleiten →
  Zwei-Faktor-Anmeldung beim Konto (Google/GitHub) einschalten. Tailscale selbst kann die Inhalte nicht
  lesen, da TLS auf dem Handy endet.
- Empfehlung: Auf dem S21 FE möglichst keine sensiblen privaten Daten, Bildschirmsperre, Updates
  installieren. Wenn ClassroomView nicht gebraucht wird (Ferien), Server in der Host-App stoppen.
- Daten liegen nur auf dem S21 FE. Geht es verloren oder wird die App gelöscht, sind alle Klassen weg
  (Backups sind absichtlich deaktiviert, damit Tokens nicht in Cloud-Backups landen).
- Fällt das S21 FE, das Internet zuhause oder Tailscale aus, funktioniert ClassroomView in der Schule nicht.

### 4.10 Debug-Signaturschlüssel im Repository

Damit neue Versionen (auch aus GitHub Actions) ohne Datenverlust über die alte installiert werden können,
liegt ein **Debug**-Signaturschlüssel in `android/debug.keystore`. Wer Zugriff auf das Repository hat, könnte
damit eine veränderte App signieren, die Android als Update akzeptiert – sie müsste dir aber erst
untergeschoben werden. Installiere APKs daher nur aus deinem eigenen Repository. `TODO:` In Phase 10
Release-Builds mit einem Schlüssel signieren, der als GitHub-Secret hinterlegt ist.

### 4.11 Datenschutz

Es werden Namen und ggf. Fotos von (oft minderjährigen) Schülern verarbeitet. Für den Einsatz in einer
Schule sind die dortigen Datenschutzvorgaben (DSGVO, Vorgaben des Schulträgers, ggf. Einwilligungen) zu
beachten. Das ist keine Rechtsberatung. Technisch empfehlenswert: Vornamen oder Spitznamen statt voller
Namen, eingebaute Avatare statt Fotos, kurze Aufbewahrungsfristen. Positiv: Alle Daten liegen bei dir zuhause auf dem S21 FE. Tailscale sieht nur Verbindungsdaten (IP-Adressen, Zeitpunkte, Datenmenge), keine Inhalte.

---

## 5. Sicherheits-Checkliste

Abgehakt ist, was umgesetzt und getestet ist.

**Server (Host-App)**
- [x] Tokens und Codes mit `SecureRandom` *(Phase 3)*
- [x] Nur Hashes von Tokens in der Datendatei *(Phase 3)*
- [x] Code-Alphabet ohne 0/O, 1/I/L; Eingabe normalisiert (Großschreibung, Trennzeichen entfernt) *(Phase 3)*
- [x] Rate-Limits Beitritt (IP, Code), Klassen-Erstellung (IP), Wiederherstellung *(Phase 3)*
- [ ] Rate-Limit für Freigabe-Anfragen je Gerät *(Phase 6)*
- [x] Jede Lehrer-Aktion prüft Token ↔ Klasse ↔ Gerät *(Phase 3)*
- [ ] Statusübergänge nur aus `pending`, Endzustände unveränderlich
- [ ] `rtc.*` nur zwischen den zwei Teilnehmern einer offenen Sitzung
- [ ] Aufräumen beim Start (offene Anfragen/Sitzungen beenden)
- [x] Bild-Upload: Größe, Format per Dekodierung, Maße, Neukodierung, Metadaten entfernt *(Phase 3)*
- [x] Schema-Validierung aller REST-Eingaben, Längenbegrenzungen *(Phase 3)*
- [ ] WS-Nachrichten validiert, Größe begrenzt *(Phase 5)*
- [x] Keine Geheimnisse in Logs *(Phase 3)*
- [x] Alle Zustandsänderungen über einen Mutex serialisiert, Datei atomar geschrieben *(Phase 3)*

**Web**
- [x] CSP, `Referrer-Policy: no-referrer` *(Phase 3)*
- [x] Recovery-Token nur im Fragment, nach Einlösen aus der Adresszeile entfernt *(Phase 3)*
- [ ] Kein `getUserMedia`, Peer-Verbindung `recvonly`
- [x] Profilbilder nur per authentifiziertem `fetch` *(Phase 3)*

**Android**
- [ ] Keine Kamera-/Mikrofon-Berechtigung im Manifest
- [ ] Systemdialog bei jeder Freigabe, Intent nie wiederverwendet
- [ ] Laufende Benachrichtigung während der Freigabe mit „Beenden“
- [ ] `classId`-Prüfung eingehender Anfragen
- [ ] Release-Build ohne Klartext-Verkehr
- [x] Backups deaktiviert *(Phase 3)*
- [ ] `TODO:` optional Token mit Android Keystore verschlüsseln

**Host-App / Betrieb**
- [x] Tailscale-Zustand, Schlüssel und Auth-Key nur im App-privaten Speicher, nie geloggt *(Phase 3)*
- [x] Ktor nur auf `127.0.0.1`; Funnel nur für Port 443 → Reverse-Proxy → Ktor *(Phase 3)*
- [x] `X-Forwarded-For` nur vom lokalen Proxy akzeptiert *(Phase 3)*
- [ ] Auth-Key nach Einrichtung im Tailscale-Adminbereich widerrufen; Tailscale-Konto mit Zwei-Faktor-Anmeldung
- [x] Klassen anlegen nur mit Host-Einladungsschlüssel *(Phase 3)*
- [x] Rate-Limits für alle unauthentifizierten Endpunkte (Internet!) *(Phase 3)*
- [x] Datendatei und Profilbilder nur im App-privaten Speicher, Backups deaktiviert *(Phase 3)*
- [ ] S21 FE mit Bildschirmsperre und aktuellen Sicherheitsupdates
