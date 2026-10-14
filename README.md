# ClassroomView

Kleines, selbst gehostetes Klassenraumsystem: Eine Lehrerin erstellt im iPad-Safari eine Klasse,
Schülerinnen und Schüler treten mit einem 6-stelligen Code über eine Android-App bei und erscheinen
live mit Name und Profilbild. Die Lehrerin kann eine Bildschirmfreigabe **anfragen**; nur wenn der
Schüler zustimmt **und** den Android-Systemdialog bestätigt, sieht sie den Bildschirm live (WebRTC).

> Status: **Phase 1 – Architektur.** Es gibt noch keinen Code.

## Repository-Struktur (geplant)

| Ordner     | Inhalt                                                              | ab Phase |
|------------|---------------------------------------------------------------------|----------|
| `/docs`    | Architektur, Sicherheit, Hosting, Kosten, später Test- und Fehlerhilfe | 1        |
| `/web`     | Lehrer-Web-App (React + TypeScript + Vite), zuerst statische Mockups | 2        |
| `/android` | Schüler-App (Kotlin, Jetpack Compose, Material 3)                    | 2        |
| `/backend` | Node.js + TypeScript, REST + WebSocket-Signalisierung, SQLite        | 3        |
| `/deploy`  | Docker Compose (Backend, Caddy, coturn), Beispielkonfigurationen     | 3 / 7    |

## Dokumentation

- [`docs/architecture.md`](docs/architecture.md) – Komponenten, Technikwahl, Datenmodell, Protokoll, Ablaufdiagramme, Android-Besonderheiten
- [`docs/security.md`](docs/security.md) – Sicherheitsmodell, Grenzen ohne Benutzerkonten, Checkliste
- [`docs/hosting-and-costs.md`](docs/hosting-and-costs.md) – Was selbst gehostet wird, laufende Kosten, lokaler WLAN-Testbetrieb

## Phasen

| #  | Phase                                     | Status        |
|----|-------------------------------------------|---------------|
| 1  | Architektur                               | in Review     |
| 2  | Mockups (HTML + Compose-Previews)         | offen         |
| 3  | Backend: Klassen, Codes, Tokens, Beitritt | offen         |
| 4  | Android-Basis: Profil, Avatar, Beitritt   | offen         |
| 5  | Echtzeit-Schülerliste                     | offen         |
| 6  | Freigabe-Anfragen (ohne Stream)           | offen         |
| 7  | WebRTC-Einzelstream inkl. TURN            | offen         |
| 8  | Robustheit & Tests                        | offen         |
| 9  | Optional: mehrere Streams                 | offen         |
| 10 | Feinschliff & Doku                        | offen         |
