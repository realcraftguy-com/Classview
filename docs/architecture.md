# Architektur

Stand: Phase 1, Revision 2 (nach deinem Feedback). Dieses Dokument beschreibt *was* gebaut wird und
*warum*. Konkrete Bibliotheksversionen werden erst in der Phase festgelegt, in der sie eingeführt werden,
und dann gegen npm bzw. Maven Central geprüft. Wo ich mir bei einer API oder einem Plattformverhalten nicht
sicher bin, steht das als **Zu prüfen** dabei; alle Prüfpunkte sind in §12 gesammelt.

### Änderungen gegenüber Revision 1

| Vorher | Jetzt | Grund |
|---|---|---|
| Server auf einem VPS (Node.js, Caddy, coturn) | **Host-App auf dem Galaxy S21 FE 5G**, ohne Root | dein Wunsch: kein Server, keine Kosten |
| Backend in Node.js + TypeScript | Backend in **Kotlin (Ktor)**, als reines JVM-Modul | läuft unverändert in der Host-App *und* zum Entwickeln/Testen auf dem PC; teilt die Protokoll-Klassen mit der Schüler-App |
| Erreichbar über das Internet | **nur im lokalen Netz** (WLAN oder Hotspot des S21 FE) | ein Handy ist aus dem Internet nicht erreichbar (siehe §5.4) |
| Let's-Encrypt-Zertifikat | **eigene lokale Zertifizierungsstelle** in der Host-App, einmalig aufs iPad installiert | ohne Domain kein öffentliches Zertifikat |
| coturn | entfällt; nur Fallback: kleiner STUN-Dienst in der Host-App | Geräte sehen sich im WLAN gegenseitig (deine Antwort 3) |
| SQLite | JSON-Datei + Bilddateien, atomar geschrieben | gleicher Code auf Android und PC, Datenmenge winzig |
| Optionaler Verbindungsdienst | **fest eingeplant**, mit eigenem Statusleisten-Symbol | deine Antwort 1 (zum blauen Symbol siehe §9.4) |

---

## 1. Überblick

```mermaid
flowchart LR
    subgraph NET["Gleiches WLAN oder Hotspot des S21 FE"]
        subgraph HOST["Galaxy S21 FE 5G · Host-App"]
            K["Ktor-Server<br/>HTTPS · WSS"]
            WA["Web-App-Dateien<br/>(in der APK)"]
            ST[("JSON-Datei<br/>+ Profilbilder")]
            CA["Lokale CA<br/>+ Zertifikat"]
        end
        subgraph IPAD["iPad · Safari"]
            W["Lehrer-Web-App"]
        end
        subgraph PAD["Xiaomi Pad 7 · Schüler-App"]
            A["Kotlin · Compose"]
            M["MediaProjection"]
        end
    end

    W -->|"https://192.168.x.y:8443 (REST + WSS)"| K
    A -->|"findet Host per mDNS, dann REST + WSS"| K
    K --> WA
    K --> ST
    M -->|"Bildschirm-Frames"| A
    A -.->|"WebRTC-Video, DTLS-SRTP, direkt"| W
```

**Grundprinzipien**

1. **Der Server ist die einzige Autorität** für Klassen, Mitgliedschaften, Anfragen und Sitzungen.
   Clients schicken Absichten, der Server prüft und verteilt den neuen Zustand.
2. **Video fließt nie durch den Server.** Er vermittelt nur die Signalisierung (SDP, ICE). Das Video geht
   direkt vom Tablet zum iPad, verschlüsselt. Nichts wird aufgezeichnet.
3. **Zustimmung ist zweistufig und liegt beim Schülergerät:** App-Dialog *und* Android-Systemdialog.
4. **Jede Phase ist lauffähig.** Der Server läuft ab Phase 3 auch auf dem PC, Phase 3–6 brauchen kein WebRTC.

---

## 2. Technikwahl

| Teil | Wahl | Begründung |
|---|---|---|
| Server | **Kotlin + Ktor** (Server-Framework von JetBrains), reines JVM-Modul | siehe §2.1 |
| HTTP-Engine | Ktor **Netty**-Engine mit TLS | Die CIO-Engine von Ktor kann serverseitig kein TLS. **Zu prüfen in Phase 3** (Spike gleich zu Beginn): Netty mit TLS auf Android. Fallback: CIO auf `127.0.0.1` plus kleiner TLS-Vorschalter mit Androids `SSLServerSocket` in derselben App. |
| WebSocket | Ktor-WebSockets | Browser nutzt die native `WebSocket`-API, Android **OkHttp** |
| Serialisierung | **`kotlinx.serialization`** | gemeinsames Modul `protocol` für Server und Schüler-App → beide Seiten benutzen dieselben Nachrichtenklassen |
| Datenhaltung | Zustand im Speicher, Änderungen sofort als JSON-Datei gespeichert (erst temporäre Datei, dann umbenennen = atomar); Profilbilder als Dateien | Bei ≤ 40 Schülern je Klasse sind das wenige KB. Kein Datenbanktreiber, der sich zwischen Android und PC unterscheidet. Alle Änderungen laufen nacheinander über einen Mutex, damit z. B. „Annehmen“ und „Ablaufen“ nie gleichzeitig passieren. Hinter einer Schnittstelle, später austauschbar gegen SQLite. |
| Zertifikate | **Bouncy Castle** (`bcpkix`) zum Erzeugen von CA und Server-Zertifikat | Android selbst bietet dafür keine öffentliche API |
| Bildprüfung | Android `BitmapFactory` (Host) bzw. `javax.imageio` (PC) hinter einer Schnittstelle | dekodieren, Maße prüfen, **neu kodieren** (entfernt EXIF/GPS) |
| Tests Server | JUnit 5 + Ktor `testApplication` | laufen auf dem PC und in CI, ohne Handy |
| Lehrer-Web | React + TypeScript + Vite, eigenes CSS mit CSS-Variablen | Build wird in die Host-APK gepackt |
| Web-Zustand | kleiner eigener Store (`useSyncExternalStore`) | |
| QR-Code | npm-Paket `qrcode` (Web) bzw. ZXing-Core (Host-App) | Host-App zeigt QR mit der Adresse fürs iPad |
| Android allgemein | Kotlin, Jetpack Compose, Material 3, eine Activity je App, Navigation Compose | |
| Android-Netzwerk | **OkHttp** (REST + WebSocket) | |
| Host-Suche | Android **NSD** (`NsdManager`, mDNS/DNS-SD), Dienst `_classview._tcp` | Schüler-App findet die Host-App automatisch; manuelle IP-Eingabe als Fallback |
| Android-Speicher | **DataStore** (Preferences) | |
| Bildauswahl | Photo Picker (`ActivityResultContracts.PickVisualMedia`) | keine Speicherberechtigung nötig |
| Android-WebRTC | **`io.getstream:stream-webrtc-android`** | siehe §2.2 |
| APK-Bau | Gradle; zusätzlich **GitHub Actions**, das bei jedem Push beide APKs baut | du kannst die APKs direkt aus GitHub herunterladen und installieren, ohne Android Studio |

### 2.1 Warum der Server jetzt in Kotlin ist

Node.js auf einem nicht gerooteten Handy ginge nur über Termux (keine eigene APK) oder eingebettetes
Node (`nodejs-mobile`), bei dem native Module wie SQLite- oder Bildbibliotheken für Android
nachkompiliert werden müssten. Kotlin/Ktor läuft dagegen normal in einer Android-App.

Weil das Server-Modul **keine Android-Abhängigkeiten** hat, läuft derselbe Code
- in der Host-App auf dem S21 FE,
- auf dem PC (`./gradlew :backend:server:run`) zum Entwickeln, für automatische Tests und in CI,
- später bei Bedarf auch auf einem richtigen Server (dann wieder mit Internetzugang).

### 2.2 WebRTC-Bibliothek für Android

Google veröffentlicht kein gepflegtes offizielles Android-Artefakt mehr (`org.webrtc:google-webrtc` ist
veraltet). Selbst bauen dauert Stunden und braucht viel Platz. Realistische Optionen:

| Option | Bewertung |
|---|---|
| **`io.getstream:stream-webrtc-android`** (GetStream, Apache 2.0, Maven Central) | vorkompiliertes Original-libwebrtc mit der Standard-API `org.webrtc.*` (`PeerConnectionFactory`, `ScreenCapturerAndroid`, Hardware-Encoder …), regelmäßig aktualisiert. Wir nutzen nur das Basis-Artefakt. → **Wahl** |
| `io.github.webrtc-sdk:android` (von LiveKit gepflegt) | gleiche `org.webrtc`-API, gute Alternative |

**Zu prüfen in Phase 7:** aktuelle Version; ob `ScreenCapturerAndroid` bei Größenänderung
`VirtualDisplay.resize()` nutzt oder das Display neu anlegt (ab Android 14 nur einmal pro MediaProjection
erlaubt). Notfalls eigener schlanker Capturer (VirtualDisplay → `SurfaceTextureHelper`).

### 2.3 Videoparameter (Startwerte, in Phase 7 messen)

- Codec **H.264** (Hardware-Encoder im Pad 7, Safari dekodiert hardwarebeschleunigt), Fallback VP8.
- Lange Kante max. **1920 px** (Pad 7: 3200 × 2136), 15–30 fps, `MAINTAIN_RESOLUTION` (lieber weniger
  Bilder pro Sekunde als unlesbarer Text).
- Nur Video, **kein Audio-Track** → `<video muted playsinline>` startet in Safari ohne Autoplay-Sperre.

---

## 3. Repository und Module

```
/                    Gradle-Wurzel für alles in Kotlin
├─ backend/
│  ├─ protocol/      REST-/WebSocket-Nachrichten (kotlinx.serialization), reines Kotlin
│  └─ server/        Ktor-Server, Logik, Rate-Limits, Speicher-Schnittstelle, Tests; PC-Startpunkt
├─ android/
│  ├─ app-student/   Schüler-App (Xiaomi Pad 7)            → APK 1
│  └─ app-host/      Host-App (Galaxy S21 FE), bettet server + Web-App ein → APK 2
├─ web/              Lehrer-Web-App (React + Vite); Build landet in app-host
├─ docs/
└─ .github/workflows Bau beider APKs + Tests
```

---

## 4. Komponenten im Detail

### 4.1 Host-App (Galaxy S21 FE 5G)

Bewusst schlicht – die Klasse wird im iPad verwaltet, nicht hier.

- **Ein Bildschirm:** großer Schalter „Server läuft / gestoppt“, Adresse fürs iPad als Text und QR-Code,
  Anzahl verbundener Geräte, Kurzanleitung „Zertifikat aufs iPad“, Hinweis zu Samsung-Akkueinstellungen.
- **`HostService`**: Foreground Service (Typ `specialUse`, für eine nicht über Google Play verteilte App
  unproblematisch) mit Benachrichtigung „Klassenserver läuft · 192.168.x.y“ und „Stoppen“-Aktion. Er hält
  - den Ktor-Server (Port **8443** HTTPS/WSS; Ports unter 1024 gehen ohne Root nicht),
  - einen kleinen HTTP-Port **8080** nur für das Herunterladen des CA-Zertifikats und eine Weiterleitung,
  - die mDNS-Anmeldung `_classview._tcp` (mit Kurz-Fingerabdruck des Zertifikats im TXT-Eintrag),
  - einen `PARTIAL_WAKE_LOCK` und einen WLAN-Lock, damit das Handy bei ausgeschaltetem Display erreichbar bleibt.
- **Web-App** wird beim Bauen aus `/web` erzeugt und in die APK gepackt.
- **Daten** im App-eigenen Speicher, Backup deaktiviert.

### 4.2 Server (`backend/server`)

- **REST** für Anfrage/Antwort-Fälle, **WebSocket `/ws`** für Live-Ereignisse und Signalisierung.
- Erste WS-Nachricht muss `hello` mit Token sein, sonst wird nach 5 s geschlossen (Tokens nicht in URLs).
- Zustand im Speicher, jede Änderung sofort persistiert (siehe §2).
- **Beim Start** werden offene Anfragen `expired`, offene Sitzungen `ended` (`server_restart`).
- Rate-Limits im Speicher.

### 4.3 Lehrer-Web-App (iPad, Safari)

- Von der Host-App ausgeliefert (gleicher Origin → kein CORS).
- PWA-Manifest + `apple-mobile-web-app-capable` → startet vom Home-Bildschirm ohne Safari-Leisten.
  Home-Bildschirm-App und Safari-Tab haben **getrennten Speicher**.
- Speichert pro Klasse lokal: `classId`, Name, Lehrer-Token, Wiederherstellungslink.
- Profilbilder per `fetch` mit `Authorization`-Header → Blob-URL (keine öffentlichen Bild-URLs).
- Hash-Routen: `#/`, `#/class/:id`, `#/class/:id/student/:deviceId`, `#/recover/<token>`.

### 4.4 Schüler-App (Xiaomi Pad 7)

```
MainActivity (Compose, edge-to-edge)
 ├─ Screens: Start · Profil · Beitreten · Beigetreten · Freigabe-Dialog · HyperOS-Hinweis
 ├─ ViewModels (StateFlow)
 ├─ ProfileRepository     – DataStore: Name, Avatar (Preset oder lokale Bilddatei)
 ├─ ClassRepository       – DataStore: Host-Adresse, CA-Fingerabdruck, classId, deviceId, deviceToken
 ├─ HostDiscovery         – NsdManager: sucht _classview._tcp im WLAN
 ├─ ClassConnection       – OkHttp-WebSocket, Heartbeat, Reconnect mit Backoff
 └─ ClassroomService      – EIN Foreground Service für die ganze Klassenzeit:
                             · verbunden: Typ specialUse, Notification „Mit Klasse 7b verbunden“
                             · bei Freigabe: zusätzlich Typ mediaProjection, Notification „Bildschirm
                               wird geteilt“ mit „Beenden“; hält MediaProjection, Capturer, PeerConnection
```

- Ein gemeinsamer Service statt zwei: nur **ein** Symbol in der Statusleiste, das sein Aussehen wechselt.
- `minSdk` 29, `targetSdk` 35. Backups deaktiviert.
- Profilbild wird auf dem Gerät auf max. 512 px verkleinert und als JPEG hochgeladen.
- Keine Kamera- oder Mikrofonberechtigung (QR-Scannen fällt damit weg; Beitritt nur per Code).

---

## 5. Netzwerk, Host-Suche und Verschlüsselung

### 5.1 Wie die Geräte sich finden

| Gerät | Wie es die Host-App findet |
|---|---|
| Xiaomi Pad 7 | automatisch per mDNS (`NsdManager`). Gibt es mehrere Hosts, wird der genommen, bei dem der Code passt. Fallback: IP-Adresse in den Einstellungen eintippen. Nach dem Beitritt merkt sich die App den Host (Fingerabdruck) und sucht ihn bei IP-Wechsel erneut per mDNS. |
| iPad | Safari kann nicht per mDNS suchen. Die Host-App zeigt `https://192.168.x.y:8443` als Text und QR-Code (mit der iPad-Kamera scannen öffnet Safari). Tipp: im Router dem S21 FE eine feste IP zuweisen, dann ändert sich die Adresse nie. |

### 5.2 HTTPS ohne Domain: eigene Zertifizierungsstelle

1. Beim ersten Start erzeugt die Host-App eine **eigene CA** (privater Schlüssel verlässt das Handy nie)
   und damit ein Server-Zertifikat für ihre aktuellen IP-Adressen (WLAN und ggf. Hotspot). Ändert sich die
   IP, stellt sie automatisch ein neues Server-Zertifikat aus – die CA bleibt gleich.
2. **iPad, einmalig:** `http://192.168.x.y:8080/ca` öffnen → Profil laden → *Einstellungen → Profil geladen
   → Installieren* → *Allgemein → Info → Zertifikatsvertrauenseinstellungen* → Vertrauen einschalten.
   Danach ist alles normales HTTPS ohne Warnungen.
3. **Schüler-App:** vertraut beim ersten Kontakt der CA, deren Fingerabdruck per mDNS angekündigt wurde,
   und **merkt ihn sich** („Trust on first use“). Spätere Verbindungen zu einer anderen CA werden abgelehnt.
   Host-App und Schüler-App zeigen eine kurze Server-Kennung (z. B. `K7-P4`) zum optionalen Vergleichen.
4. Server-Zertifikate erfüllen Apples Anforderungen (Subject Alternative Name mit IP, Extended Key Usage
   `serverAuth`, RSA ≥ 2048 oder ECDSA P-256, Laufzeit ≤ 1 Jahr mit automatischer Erneuerung).

Warum der Aufwand: Ohne TLS könnte jeder im selben WLAN Lehrer- und Geräte-Tokens mitlesen. (Das Video
selbst wäre durch WebRTC trotzdem verschlüsselt.) Ein **reiner HTTP-Modus** existiert nur im
Debug-Build zum Entwickeln.

### 5.3 WebRTC im lokalen Netz

Du hast bestätigt, dass sich die Geräte im WLAN gegenseitig erreichen. Dann geht das Video direkt
Tablet → iPad, ein TURN-Server ist nicht nötig. Auch über den **Hotspot des S21 FE** funktioniert das
(iPad und Tablet hängen dann beide am Handy; lokaler Verkehr verbraucht kein mobiles Datenvolumen).

**Zu prüfen in Phase 7:** Safari verrät ohne Kamera-/Mikrofonfreigabe seine lokale IP nicht als
ICE-Kandidat. Für einen reinen Empfänger sollte die Verbindung trotzdem zustande kommen (Safari prüft die
Kandidaten des Tablets, das Tablet lernt Safaris Adresse daraus). Falls nicht: Die Host-App bekommt einen
**winzigen STUN-Dienst** (UDP 3478, nur „Binding“-Antworten, wenige Dutzend Zeilen). Damit erfährt Safari
seine eigene WLAN-Adresse und kann sie anbieten.

### 5.4 Grenze: nur im lokalen Netz

Ein Handy im WLAN oder Mobilfunk ist aus dem Internet nicht erreichbar (private IP, Carrier-NAT, keine
Portfreigabe ohne Router). Alle drei Geräte müssen also **im selben Netz** sein – dasselbe WLAN oder der
Hotspot des S21 FE. Soll es später übers Internet gehen, kann derselbe Server-Code auf einen kleinen VPS
umziehen (siehe `hosting-and-costs.md` §5, ca. 5–10 €/Monat).

---

## 6. Datenmodell

```mermaid
erDiagram
    SchoolClass ||--o{ TeacherCredential : "hat Lehrer-Geräte"
    SchoolClass ||--o{ Device : "hat Schüler"
    SchoolClass ||--o{ ShareRequest : "enthält"
    Device ||--o{ ShareRequest : "erhält"
    TeacherCredential ||--o{ ShareRequest : "stellt"
    ShareRequest ||--o| StreamSession : "führt zu"
```

Im Diagramm und im Code heißt `Class` **`SchoolClass`**, weil `class` in Mermaid ein Schlüsselwort ist und
`Class` in Kotlin mit `java.lang.Class` kollidiert. Alle IDs sind zufällige UUIDv4, alle Zeitstempel UTC.
Geheimnisse werden nur als **SHA-256-Hash** gespeichert (256 Bit Zufall, daher kein langsamer
Passwort-Hash nötig).

### `SchoolClass`

| Feld | Typ | Beschreibung |
|---|---|---|
| `id` | UUID | |
| `name` | Text (1–60) | Klassenname |
| `teacherName` | Text (0–60), optional | wird dem Schüler bei Anfragen angezeigt |
| `code` | Text(6), eindeutig, nullable | aktueller Beitrittscode; `null` = kein gültiger Code |
| `codeCreatedAt`, `codeExpiresAt` | Zeitstempel, Ablauf optional | z. B. 1 h / 1 Tag / 7 Tage / nie |
| `joinPaused` | bool | Beitritt vorübergehend gesperrt |
| `maxDevices` | int | Standard 40 |
| `recoveryTokenHash` | Text | |
| `createdAt`, `lastActivityAt` | Zeitstempel | für automatisches Aufräumen |

### `TeacherCredential`

Eine Klasse kann auf mehreren Lehrer-Geräten offen sein; jedes hat ein eigenes Token und kann einzeln
abgemeldet werden.

| Feld | Typ | Beschreibung |
|---|---|---|
| `id`, `classId` | UUID | |
| `tokenHash` | Text, eindeutig | |
| `label` | Text | z. B. „iPad · Safari“ |
| `createdAt`, `lastUsedAt`, `revokedAt` | Zeitstempel | |

### `Device` (Schüler in einer Klasse)

| Feld | Typ | Beschreibung |
|---|---|---|
| `id` | UUID | Zuordnung **nur** hierüber, nie über den Namen |
| `classId` | UUID | genau eine Klasse |
| `tokenHash` | Text, eindeutig | |
| `displayName` | Text (1–40) | |
| `avatarKind` | `preset` \| `image` | |
| `avatarPreset` | Text, nullable | eingebauter Avatar (eigene Grafiken, in Web und App identisch) |
| `avatarFile` | Dateiname, nullable | zufälliger Name, neu kodiertes Bild ≤ 512 × 512 |
| `avatarVersion` | int | wird bei jeder Änderung erhöht |
| `connection` | `online` \| `offline` | |
| `lastSeenAt`, `joinedAt` | Zeitstempel | |
| `removedAt` | Zeitstempel, nullable | entfernt oder ausgetreten → Token ungültig |

### `ShareRequest`

| Feld | Typ | Beschreibung |
|---|---|---|
| `id`, `classId`, `deviceId`, `teacherCredentialId` | UUID | Ziel: genau ein Gerät |
| `status` | `pending` \| `accepted` \| `denied` \| `expired` \| `cancelled` | |
| `reason` | Text, nullable | `user_denied`, `system_dialog_cancelled`, `timeout`, `device_offline`, `teacher_cancelled` |
| `createdAt`, `expiresAt`, `respondedAt` | Zeitstempel | `expiresAt` = `createdAt` + 60 s |

### `StreamSession`

| Feld | Typ | Beschreibung |
|---|---|---|
| `id` | UUID | |
| `shareRequestId` | UUID, eindeutig | |
| `classId`, `deviceId`, `teacherCredentialId` | UUID | |
| `status` | `connecting` \| `active` \| `ended` | |
| `endReason` | Text, nullable | `student_stopped`, `teacher_stopped`, `device_disconnected`, `teacher_disconnected`, `connection_failed`, `projection_stopped`, `connect_timeout`, `device_removed`, `server_restart` |
| `createdAt`, `startedAt`, `endedAt` | Zeitstempel | |

Gespeichert werden **nur Metadaten**, nie Bild- oder Videoinhalte. Abgeschlossene Anfragen und Sitzungen
werden nach 7 Tagen gelöscht.

### Zustandsautomaten

```mermaid
stateDiagram-v2
    direction LR
    [*] --> pending: Lehrerin fragt an
    pending --> accepted: Erlauben UND Systemdialog bestätigt
    pending --> denied: Verweigern oder Systemdialog abgebrochen
    pending --> expired: 60 s ohne Antwort oder Gerät offline
    pending --> cancelled: Lehrerin bricht ab
    accepted --> [*]
    denied --> [*]
    expired --> [*]
    cancelled --> [*]
```

Alle Endzustände sind **endgültig**: Der Server ändert einen Status nur, wenn er noch `pending` ist (unter
dem Mutex geprüft). Ein `share.accept` auf eine abgelehnte oder abgelaufene Anfrage wird immer abgewiesen.

```mermaid
stateDiagram-v2
    direction LR
    [*] --> connecting: Anfrage angenommen
    connecting --> active: WebRTC verbunden, erstes Bild
    connecting --> ended: 20 s Timeout, Fehler oder Beenden
    active --> ended: Beenden oder Verbindungsverlust
    ended --> [*]
```

---

## 7. Schnittstellen

### 7.1 REST (`/api`)

| Methode & Pfad | Auth | Zweck |
|---|---|---|
| `GET /api/info` | – | Server-Kennung, Protokollversion (für Host-Suche) |
| `POST /api/classes` | – (rate-limitiert) | Klasse anlegen → `classId`, `code`, `teacherToken`, `recoveryToken` |
| `POST /api/classes/recover` | Recovery-Token im Body | neues Lehrer-Token für dieses Gerät |
| `GET /api/classes/:classId` | Lehrer | kompletter Stand |
| `PATCH /api/classes/:classId` | Lehrer | Name, Lehrkraftname, Ablaufzeit, `joinPaused` |
| `POST /api/classes/:classId/code` | Lehrer | Code erneuern (alter sofort ungültig) |
| `POST /api/classes/:classId/recovery` | Lehrer | Wiederherstellungslink erneuern |
| `GET …/teachers` · `DELETE …/teachers/:id` | Lehrer | Lehrer-Geräte anzeigen / abmelden |
| `DELETE /api/classes/:classId/devices/:deviceId` | Lehrer | Schüler entfernen |
| `GET /api/classes/:classId/devices/:deviceId/avatar` | Lehrer | Profilbild |
| `DELETE /api/classes/:classId` | Lehrer | Klasse mit allem löschen |
| `POST /api/join` | – (rate-limitiert) | Code + Name + Avatar-Preset → `deviceId`, `deviceToken`, Klassenname, Lehrkraftname |
| `PATCH /api/devices/me` | Gerät | Name / Avatar-Preset ändern |
| `PUT /api/devices/me/avatar` | Gerät | Profilbild hochladen (max. 1 MB, JPEG/PNG/WebP) |
| `DELETE /api/devices/me` | Gerät | Klasse verlassen |

Auth per `Authorization: Bearer <token>`. Lehrer-Token gehören zu genau einer Klasse, Geräte-Token zu
genau einem `Device`.

### 7.2 WebSocket-Nachrichten (`/ws`)

Umschlag: `{ "type": "...", "id"?: "...", "data": { ... } }`. Ungültiges wird mit `error` beantwortet.

| Richtung | Typ | Bedeutung |
|---|---|---|
| C → S | `hello` | `{ role: "teacher", classId, token }` oder `{ role: "device", deviceId, token }` |
| S → C | `welcome` / `error` | Erfolg bzw. `invalid_token`, `device_removed`, `class_deleted` … |
| C ↔ S | `ping` / `pong` | Heartbeat alle 10 s |
| S → Lehrer | `class.snapshot` | vollständiger Stand nach jedem (Re-)Connect |
| S → Lehrer | `class.updated` | Name, Code, Ablauf, Pause |
| S → Lehrer | `device.joined` · `device.updated` · `device.left` · `device.removed` | Schülerliste live |
| S → Gerät | `class.info` · `removed` | |
| Lehrer → S | `share.request { deviceId }` · `share.cancel { requestId }` | |
| S → Gerät | `share.requested { requestId, classId, className, teacherName, expiresAt }` | |
| S → Gerät | `share.closed { requestId, status }` | abgelaufen / zurückgezogen |
| Gerät → S | `share.deny { requestId, reason }` · `share.accept { requestId }` | `accept` erst **nach** bestätigtem Systemdialog |
| S → Lehrer | `share.updated { requestId, deviceId, status, reason }` | |
| S → beide | `stream.start { sessionId, iceServers }` | `iceServers` leer oder lokaler STUN |
| beide ↔ S | `rtc.offer` · `rtc.answer` · `rtc.ice` | Tablet ist Offerer, Safari antwortet `recvonly` |
| beide → S | `stream.state` · `stream.stop` | |
| S → beide | `stream.updated` · `stream.ended { reason }` | |
| S → Lehrer | `stream.content { visible }` | geteilte App gerade (nicht) sichtbar |

`rtc.*` wird nur zwischen den zwei Teilnehmern einer offenen Sitzung weitergeleitet.

---

## 8. Abläufe

### 8.1 Einmalige Einrichtung

```mermaid
sequenceDiagram
    autonumber
    participant H as Host-App (S21 FE)
    participant L as iPad
    participant S as Schüler-App (Pad 7)

    H->>H: Server starten, CA + Zertifikat erzeugen,<br/>mDNS _classview._tcp anmelden
    H-->>L: QR-Code / Adresse anzeigen
    L->>H: http://IP:8080/ca → Profil laden
    Note over L: Profil installieren,<br/>Zertifikat voll vertrauen
    L->>H: https://IP:8443 öffnen, zum Home-Bildschirm hinzufügen
    S->>S: HyperOS-Hinweise abarbeiten (Autostart, Akku, Benachrichtigungen)
```

### 8.2 Klasse erstellen und beitreten

```mermaid
sequenceDiagram
    autonumber
    participant L as Lehrer-Web (Safari)
    participant H as Server (Host-App)
    participant S as Schüler-App

    L->>H: POST /api/classes {name, teacherName}
    H-->>L: classId, code, teacherToken, recoveryToken
    L->>H: WS hello {role: teacher, classId, token}
    H-->>L: welcome + class.snapshot

    S->>S: Host per mDNS suchen
    S->>H: GET /api/info (Zertifikat prüfen bzw. erstmals merken)
    S->>H: POST /api/join {code, displayName, avatarPreset}
    Note over H: Rate-Limit pro IP und Code<br/>Code gültig, nicht abgelaufen, nicht pausiert?<br/>Klasse nicht voll?
    H-->>S: deviceId, deviceToken, className, teacherName
    opt eigenes Profilbild
        S->>H: PUT /api/devices/me/avatar (≤ 512 px)
    end
    S->>S: ClassroomService starten (Symbol in der Statusleiste)
    S->>H: WS hello {role: device, deviceId, token}
    H-->>S: welcome + class.info
    H-->>L: device.joined
    L->>H: GET …/avatar (Bearer teacherToken)
    Note over L: Karte erscheint ohne Neuladen
```

Danach verbindet sich die App immer mit `deviceId` + `deviceToken`, **ohne Code**. „Code erneuern“
betrifft beigetretene Schüler deshalb nicht.

### 8.3 Freigabe anfragen

```mermaid
sequenceDiagram
    autonumber
    participant L as Lehrer-Web (Safari)
    participant H as Server (Host-App)
    participant S as Schüler-App
    participant A as Android-System

    L->>H: share.request {deviceId}
    Note over H: Lehrer-Token gehört zur Klasse?<br/>Gerät gehört zur Klasse und ist online?<br/>keine andere offene Anfrage / Sitzung?
    H-->>L: share.updated {pending}
    H->>S: share.requested {requestId, classId, className, teacherName, expiresAt}
    Note over S: classId == aktuelle Klasse? sonst verwerfen
    S->>S: Dialog (Vordergrund) oder Benachrichtigung (Hintergrund)

    alt Verweigern
        S->>H: share.deny {user_denied}
        H-->>L: share.updated {denied}
    else 60 s keine Antwort
        H->>S: share.closed {expired}
        H-->>L: share.updated {expired}
    else Erlauben
        S->>A: createScreenCaptureIntent()
        alt Abbruch im Systemdialog
            A-->>S: RESULT_CANCELED
            S->>H: share.deny {system_dialog_cancelled}
            H-->>L: share.updated {denied}
        else Bestätigt (ganzer Bildschirm oder eine App)
            A-->>S: RESULT_OK + Intent
            S->>S: ClassroomService um Typ mediaProjection erweitern
            S->>H: share.accept {requestId}
            Note over H: nur wenn noch pending,<br/>sonst Fehler → App stoppt Aufnahme sofort
            H-->>L: share.updated {accepted}
            H->>S: stream.start {sessionId}
            H->>L: stream.start {sessionId}
            S->>H: rtc.offer
            H->>L: rtc.offer
            L->>H: rtc.answer
            H->>S: rtc.answer
            Note over S,L: rtc.ice in beide Richtungen über den Server
            S-->>L: Video (DTLS-SRTP), direkt im WLAN
            L->>H: stream.state {connected}
            H-->>L: stream.updated {active}
            H-->>S: stream.updated {active}
        end
    end
```

Die 60-s-Frist läuft auch während des Systemdialogs weiter.

### 8.4 Stream beenden

```mermaid
sequenceDiagram
    autonumber
    participant L as Lehrer-Web (Safari)
    participant H as Server (Host-App)
    participant S as Schüler-App

    alt Schüler tippt „Freigabe beenden“ oder stoppt über Android
        S->>S: Capturer + PeerConnection schließen,<br/>MediaProjection.stop()
        S->>H: stream.stop
        H->>L: stream.ended {student_stopped}
    else Lehrerin tippt „Beenden“
        L->>H: stream.stop
        H->>S: stream.ended {teacher_stopped}
        S->>S: alles schließen
    else Verbindungsverlust einer Seite
        Note over H: WS geschlossen oder 30 s kein Heartbeat
        H->>S: stream.ended {teacher_disconnected}
        H->>L: stream.ended {device_disconnected}
    end
    Note over L,S: Zusätzlich beendet jede Seite lokal bei WebRTC „failed“<br/>oder wenn die Verbindung länger als 5 s „disconnected“ ist
```

Wird die **Host-App** beendet oder verliert das S21 FE das WLAN, enden alle Streams auf beiden Seiten
(beide verlieren den WebSocket).

---

## 9. Schüler-App: Android-Besonderheiten

### 9.1 Bildschirmaufnahme

1. `MediaProjectionManager.createScreenCaptureIntent()` aus der Activity, **ohne** vorgegebene Konfiguration
   → Nutzer wählt **ganzen Bildschirm oder eine einzelne App**.
2. Bei `RESULT_OK`: `ClassroomService.startForeground(…, SPECIAL_USE | MEDIA_PROJECTION)`.
3. Erst danach `getMediaProjection()`, Callback registrieren, genau einmal `createVirtualDisplay`.

- **Einwilligung pro Sitzung:** Der Ergebnis-Intent wird nie gespeichert oder wiederverwendet.
- **Einzelne App:** `onCapturedContentResize(w, h)` passt die Größe an,
  `onCapturedContentVisibilityChanged(visible)` meldet der Lehrerin „geteilte App gerade nicht sichtbar“.
- **`onStop()`** (Stopp über System, Sperren …) → Sitzung `ended` (`projection_stopped`).
- **Drehung:** `VirtualDisplay.resize()` bei Konfigurationsänderung.
- **Manifest:** `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PROJECTION`,
  `FOREGROUND_SERVICE_SPECIAL_USE`, `POST_NOTIFICATIONS`, `INTERNET`, `ACCESS_NETWORK_STATE`,
  `CHANGE_WIFI_MULTICAST_STATE` (für mDNS). **Keine** Kamera, **kein** Mikrofon.

### 9.2 Anfragen im Hintergrund

- Durch den `ClassroomService` bleibt die Verbindung auch im Hintergrund bestehen.
- Kanal „Freigabe-Anfragen“ mit `IMPORTANCE_HIGH` (Heads-up).
- **„Verweigern“** direkt in der Benachrichtigung. **„Erlauben“** öffnet die App, die sofort den
  Systemdialog zeigt – der muss aus einer sichtbaren Activity kommen.
- Kein Full-Screen-Intent (ab Android 14 Wecker- und Telefonie-Apps vorbehalten).

### 9.3 HyperOS

HyperOS beendet Hintergrund-Apps aggressiv und unterdrückt oft Heads-up-Benachrichtigungen.
Einstellungen (Menünamen variieren je HyperOS-Version, ich prüfe sie in Phase 4):

1. **Autostart** erlauben (Einstellungen → Apps → App verwalten → ClassroomView).
2. **Akku-Sparmodus** der App: **„Keine Einschränkungen“**.
3. Benachrichtigungen: „Schwebende Benachrichtigungen“ und Sperrbildschirm für „Freigabe-Anfragen“ erlauben.
4. App in der Übersicht der letzten Apps **sperren** (Karte gedrückt halten → Schloss).
5. Damit das Statusleisten-Symbol sichtbar ist: *Einstellungen → Benachrichtigungen & Kontrollzentrum →
   Statusleiste → Benachrichtigungssymbole anzeigen* (HyperOS blendet sie teils standardmäßig aus).

Die App zeigt einen Hinweis-Screen mit diesen Schritten und springt, wo öffentlich möglich, direkt in die
Einstellungen.

### 9.4 Das Symbol in der Statusleiste („blau wie bei Apple“)

Ehrliche Einordnung:

- Der **rechte** Bereich der Statusleiste (Bluetooth, WLAN, Akku) ist dem System vorbehalten. Apps können
  dort unter Android **kein** eigenes Symbol ablegen – ohne Root geht das nicht.
- Apps erscheinen **links** in der Statusleiste mit dem kleinen Symbol ihrer laufenden Benachrichtigung.
  Diese Symbole färbt Android/HyperOS **einheitlich** (weiß bzw. dunkel). Eine eigene Farbe wie Blau
  übernimmt die Statusleiste nicht.

Was ich daher umsetze:

| Wo | Was |
|---|---|
| Statusleiste (links) | eigenes, gut erkennbares Klassen-Symbol, solange du verbunden bist; während der Freigabe wechselt es auf ein Bildschirm-Symbol |
| Benachrichtigungsleiste | dauerhafte Benachrichtigung „Mit Klasse 7b verbunden“, **blau eingefärbt** (`setColorized`, bei Foreground Services erlaubt; ob HyperOS die Einfärbung anzeigt, **prüfe ich am Gerät**) |
| In der App | blaue „Verbunden“-Kapsel oben, während der Freigabe deutlich als „Bildschirm wird geteilt“ |
| System | Android zeigt während der Aufnahme zusätzlich seinen eigenen Hinweis, den wir nicht beeinflussen |

### 9.5 Layout

Window-Size-Classes, Hoch- und Querformat, Zustand in ViewModels. Edge-to-Edge (Pflicht bei
`targetSdk 35`).

---

## 10. Host-App: Samsung-Besonderheiten (S21 FE)

Samsungs One UI beendet Hintergrund-Apps ebenfalls gern. Die Host-App läuft als Foreground Service mit
Wake- und WLAN-Lock. Zusätzlich (Menünamen je nach One-UI-Version leicht anders):

1. *Einstellungen → Apps → ClassroomView Host → Akku* → **„Nicht eingeschränkt“**.
2. *Akku → Hintergrundnutzungslimits* → Host-App zu **„Nie schlafende Apps“** hinzufügen;
   „Nicht verwendete Apps in Standby versetzen“ ggf. ausschalten.
3. Während des Unterrichts **ans Ladegerät** – dann greift der Android-Tiefschlaf (Doze) gar nicht.
4. Optional: Hotspot des S21 FE einschalten und iPad + Tablet damit verbinden – dann ist man vom
   Schul-/Heim-WLAN unabhängig.

Die Host-App zeigt diese Hinweise und fragt die Ausnahme von der Akku-Optimierung an
(`ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`).

---

## 11. Echtzeit und Zuverlässigkeit

| Thema | Umsetzung |
|---|---|
| Heartbeat | Client sendet alle 10 s `ping`. Server: 30 s ohne Nachricht → `offline`, Verbindung zu. Client: 25 s ohne `pong` → neu verbinden. |
| Reconnect | Backoff mit Zufallsanteil 1 s, 2 s, 4 s … max. 30 s; sofort bei Netzwechsel (Android) bzw. `online`/`visibilitychange` (Web). Schüler-App sucht bei Fehlschlag per mDNS neu (IP könnte sich geändert haben). |
| Vollständiger Abgleich | nach jedem Lehrer-Reconnect `class.snapshot`; die Web-App ersetzt ihren Zustand komplett |
| Keine Doppelkarten | Karten nach `deviceId`; pro Gerät gilt nur die neueste Verbindung |
| Keine hängenden Anfragen | Timer 60 s; Gerät offline → `expired`; Lehrerverbindung weg → `cancelled` |
| Keine verwaisten Streams | Ende bei `stream.stop`, Verbindungsverlust einer Seite, WebRTC `failed`, 20 s in `connecting`, MediaProjection-Stopp, Entfernen des Schülers, Server-Neustart. Max. **eine** Sitzung je Klasse bis Phase 9. |
| „Sofort“ | sauberes Beenden < 1 s; harter Netzverlust über WebRTC in wenigen Sekunden bzw. Heartbeat max. 30 s |

---

## 12. Design-Leitlinien (Details in Phase 2)

- Ruhig, viel Weißraum, Systemschrift (`-apple-system, system-ui` im Web).
- Abgerundete Karten (≈ 16 px), runde Avatare, dezente Schatten, kleine Statuspunkte (online grün,
  offline grau, Freigabe aktiv = Akzentblau mit Bildschirm-Symbol).
- Eine gedeckte **blaue** Akzentfarbe in allen drei Oberflächen; keine Verläufe, keine Neonfarben;
  Hell- und Dunkelmodus.
- Touch-Ziele ≥ 44 pt (iPad) bzw. 48 dp (Android).
- Code groß in Monospace, gruppiert `K7P·4XM`; Eingabe mit oder ohne Trennzeichen.
- Eigene, einfache Avatar-Grafiken aus einer gemeinsamen SVG-Quelle.

---

## 13. Risiken und Prüfpunkte

| Phase | Prüfpunkt | Plan B |
|---|---|---|
| 3 (zu Beginn) | Ktor-Netty mit TLS läuft auf Android | CIO lokal + eigener TLS-Vorschalter mit `SSLServerSocket` |
| 3 | iPad akzeptiert das selbst erzeugte Zertifikat nach Profil-Installation | Zertifikatsfelder anpassen; notfalls HTTP-Modus mit klarer Warnung |
| 4 | HyperOS-Menünamen, Benachrichtigungssymbole, `setColorized` | Hinweise anpassen |
| 4 | mDNS wird im WLAN nicht blockiert | manuelle IP-Eingabe |
| 4 | Android-Version des S21 FE (14, 15 oder 16) | `targetSdk` und Tests anpassen |
| 7 | Safari empfängt WebRTC ohne STUN im LAN | Mini-STUN in der Host-App |
| 7 | `ScreenCapturerAndroid` und Größenänderung ab Android 14 | eigener Capturer |
| 7 | Verhalten von HyperOS beim Sperren des Tablets während der Freigabe | Stream sauber beenden und anzeigen |

---

## 14. Offene Fragen

1. **Ist das S21 FE im Unterricht mit im Raum und im selben WLAN wie iPad und Tablet** (oder spannt es den
   Hotspot auf)? Das ist Voraussetzung für die Host-Lösung.
2. **Einmalige Zertifikats-Installation auf dem iPad** (ca. 1 Minute, Schritte in §5.2) – in Ordnung? Das ist
   mein Vorschlag. Die Alternative wäre unverschlüsseltes HTTP im WLAN, das ich nicht empfehle.
3. Welche **Android-Version** hat das S21 FE (*Einstellungen → Telefoninfo → Softwareinformationen*)?
   Nicht blockierend, nur für die Tests.

**Annahmen**, falls nichts anderes gesagt wird: Oberflächen auf Deutsch; max. 40 Schüler pro Klasse; ein
Stream gleichzeitig bis Phase 9.
