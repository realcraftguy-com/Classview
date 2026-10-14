# ClassroomView

Kleines, selbst gehostetes Klassenraumsystem: Eine Lehrerin erstellt im iPad-Safari
eine Klasse, Schülerinnen und Schüler treten mit einem 6-stelligen Code über eine Android-App bei und
erscheinen live mit Name und Profilbild. Die Lehrerin kann eine Bildschirmfreigabe **anfragen**; nur wenn
der Schüler zustimmt **und** den Android-Systemdialog bestätigt, sieht sie den Bildschirm live (WebRTC).

Gehostet wird alles von einer **Host-App auf einem Android-Handy zuhause** (Galaxy S21 FE 5G, ohne Root).
Die Schule erreicht es übers Internet unter einer kostenlosen DuckDNS-Adresse mit Let's-Encrypt-Zertifikat;
das Video selbst bleibt im Schul-WLAN.

| APK / Oberfläche | Gerät |
|---|---|
| ClassroomView Host (Server + Lehrer-Website) | Galaxy S21 FE 5G (zuhause) |
| ClassroomView (Schüler-App) | Xiaomi Pad 7 (Schule) |
| Lehrer-Web-App in Safari | iPad der Lehrerin (Schule) |

> Status: **Phase 1 – Architektur (Revision 3).** Es gibt noch keinen Code.

## Repository-Struktur (geplant)

| Ordner                | Inhalt                                                                  | ab Phase |
|-----------------------|-------------------------------------------------------------------------|----------|
| `/docs`               | Architektur, Sicherheit, Hosting, später Test- und Fehlerhilfe           | 1        |
| `/web`                | Lehrer-Web-App (React + TypeScript + Vite), zuerst statische Mockups     | 2        |
| `/android/app-student`| Schüler-App (Kotlin, Jetpack Compose, Material 3)                        | 2        |
| `/backend`            | `protocol` (gemeinsame Nachrichten) und `server` (Kotlin + Ktor, läuft auch auf dem PC) | 3 |
| `/android/app-host`   | Host-App: bettet Server und Web-App ein                                  | 3        |
| `/.github/workflows`  | Tests und Bau beider APKs                                                | 3        |

## Dokumentation

- [`docs/architecture.md`](docs/architecture.md) – Komponenten, Technikwahl, Datenmodell, Protokoll, Ablaufdiagramme, Android-Besonderheiten
- [`docs/security.md`](docs/security.md) – Sicherheitsmodell, Grenzen ohne Benutzerkonten, Checkliste
- [`docs/hosting-and-costs.md`](docs/hosting-and-costs.md) – Was wo läuft, Kosten, Erreichbarkeit, Einrichtung, Entwickeln ohne Handy

## Phasen

| #  | Phase                                     | Status        |
|----|-------------------------------------------|---------------|
| 1  | Architektur                               | in Review     |
| 2  | Mockups (HTML + Compose-Previews)         | offen         |
| 3  | Server + Host-App: Klassen, Codes, Tokens, Beitritt | offen |
| 4  | Android-Basis: Profil, Avatar, Beitritt   | offen         |
| 5  | Echtzeit-Schülerliste                     | offen         |
| 6  | Freigabe-Anfragen (ohne Stream)           | offen         |
| 7  | WebRTC-Einzelstream                       | offen         |
| 8  | Robustheit & Tests                        | offen         |
| 9  | Optional: mehrere Streams                 | offen         |
| 10 | Feinschliff & Doku                        | offen         |
