# Sicherheit

Stand: Phase 1, Revision 3 (Host-App auf dem Galaxy S21 FE zuhause, aus dem Internet erreichbar). Die Checkliste am Ende wird in jeder Phase fortgeschrieben und in Phase 10
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
| **DuckDNS-Token** | von DuckDNS | – | App-privater Speicher des S21 FE | Adresse ändern: Wer es hat, kann `…duckdns.org` auf einen eigenen Server zeigen lassen |
| **TLS-Schlüssel** (Let's Encrypt) | auf dem S21 FE erzeugt | – | App-privater Speicher, verlässt das Gerät nie | Server-Identität |

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
| Rate-Limits beim Beitritt | pro IP: 10 Versuche/min, 30 fehlgeschlagene/h → danach 1 h gesperrt; pro Code: 30 Beitritte/min; pro Klasse max. 40 Geräte; global: Fehlversuche überwacht. Klassen-Erstellung pro IP begrenzt. (Werte vorläufig, in Phase 3 festgelegt.) Hinweis: Zähler liegen im Speicher und beginnen nach Neustart neu. |
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
| Transport | nur HTTPS/WSS mit Let's-Encrypt-Zertifikat für die DuckDNS-Adresse (`architecture.md` §5.2), normale Zertifikatsprüfung auf allen Geräten. Klartext-HTTP nur im Debug-Modus auf dem PC. Release-App erlaubt keinen Klartext-Verkehr. |
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

- Durch die Portfreigabe ist der Server auf dem S21 FE **für jeden im Internet erreichbar**. Das ist nötig,
  damit die Schule ihn erreicht. Die Adresse steht zudem in öffentlichen Zertifikatsprotokollen
  (Certificate Transparency) und wird von Scannern schnell gefunden.
- Ohne Code, Token oder Einladungsschlüssel kann ein Fremder nur die Startseite laden und Beitrittscodes
  raten (rate-limitiert, siehe §2).
- Freigegeben wird **nur ein Port zu nur einem Gerät**. Andere Geräte in deinem Heimnetz sind nicht
  erreichbar. Die Host-App läuft in Androids App-Sandbox; eine Sicherheitslücke im Server würde zuerst nur
  die Daten dieser App betreffen.
- Empfehlung: Auf dem S21 FE möglichst keine sensiblen privaten Daten, Bildschirmsperre, Updates
  installieren. Wenn ClassroomView nicht gebraucht wird (Ferien), Server in der Host-App stoppen.
- Daten liegen nur auf dem S21 FE. Geht es verloren oder wird die App gelöscht, sind alle Klassen weg
  (Backups sind absichtlich deaktiviert, damit Tokens nicht in Cloud-Backups landen).
- Fällt das S21 FE oder das Internet zuhause aus, funktioniert ClassroomView in der Schule nicht.

### 4.10 Datenschutz

Es werden Namen und ggf. Fotos von (oft minderjährigen) Schülern verarbeitet. Für den Einsatz in einer
Schule sind die dortigen Datenschutzvorgaben (DSGVO, Vorgaben des Schulträgers, ggf. Einwilligungen) zu
beachten. Das ist keine Rechtsberatung. Technisch empfehlenswert: Vornamen oder Spitznamen statt voller
Namen, eingebaute Avatare statt Fotos, kurze Aufbewahrungsfristen. Positiv: Alle Daten liegen bei dir zuhause auf dem S21 FE; DuckDNS erfährt nur deine IP-Adresse, Let's Encrypt nur die Adresse `…duckdns.org`.

---

## 5. Sicherheits-Checkliste

Wird ab Phase 3 abgehakt.

**Server (Host-App)**
- [ ] Tokens und Codes mit `SecureRandom`
- [ ] Nur Hashes von Tokens in der Datendatei
- [ ] Code-Alphabet ohne 0/O, 1/I/L; Eingabe normalisiert (Großschreibung, Trennzeichen entfernt)
- [ ] Rate-Limits Beitritt (IP, Code), Klassen-Erstellung (IP), Anfragen (Gerät)
- [ ] Jede Lehrer-Aktion prüft Token ↔ Klasse ↔ Gerät
- [ ] Statusübergänge nur aus `pending`, Endzustände unveränderlich
- [ ] `rtc.*` nur zwischen den zwei Teilnehmern einer offenen Sitzung
- [ ] Aufräumen beim Start (offene Anfragen/Sitzungen beenden)
- [ ] Bild-Upload: Größe, Format per Dekodierung, Maße, Neukodierung, Metadaten entfernt
- [ ] Schema-Validierung aller Eingaben, WS-Nachrichtengröße begrenzt
- [ ] Keine Geheimnisse in Logs
- [ ] Alle Zustandsänderungen über einen Mutex serialisiert, Datei atomar geschrieben

**Web**
- [ ] CSP, `Referrer-Policy: no-referrer`
- [ ] Recovery-Token nur im Fragment, nach Einlösen aus der Adresszeile entfernt
- [ ] Kein `getUserMedia`, Peer-Verbindung `recvonly`
- [ ] Profilbilder nur per authentifiziertem `fetch`

**Android**
- [ ] Keine Kamera-/Mikrofon-Berechtigung im Manifest
- [ ] Systemdialog bei jeder Freigabe, Intent nie wiederverwendet
- [ ] Laufende Benachrichtigung während der Freigabe mit „Beenden“
- [ ] `classId`-Prüfung eingehender Anfragen
- [ ] Release-Build ohne Klartext-Verkehr
- [ ] Backups deaktiviert
- [ ] `TODO:` optional Token mit Android Keystore verschlüsseln

**Host-App / Betrieb**
- [ ] TLS-Schlüssel und DuckDNS-Token nur im App-privaten Speicher, nie geloggt
- [ ] Zertifikat wird automatisch erneuert, Host-App warnt vor Ablauf
- [ ] FritzBox: **nur** Port 443 → S21 FE:8443 freigegeben, keine weiteren Freigaben, kein „Exposed Host“
- [ ] Klassen anlegen nur mit Host-Einladungsschlüssel
- [ ] Rate-Limits für alle unauthentifizierten Endpunkte (Internet!)
- [ ] Datendatei und Profilbilder nur im App-privaten Speicher, Backups deaktiviert
- [ ] S21 FE mit Bildschirmsperre und aktuellen Sicherheitsupdates
