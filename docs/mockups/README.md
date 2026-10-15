# Mockups (Phase 2)

Statische Entwürfe zum Anschauen und für Feedback. Noch keine Funktion, keine Verbindung zum Server.

## Lehrer-Web-App (iPad, Safari)

Quelle: [`web/mockups/index.html`](../../web/mockups/index.html) – einfach im Browser öffnen. Oben in der
Mockup-Leiste lassen sich alle Bildschirme und Hell/Dunkel umschalten. Im Schüler-Detail zeigen die
gestrichelten Knöpfe die Zustände der Anfrage (bereit, wartet, abgelehnt, abgelaufen, läuft).

| Bildschirm | Bild |
|---|---|
| Start mit Klassenliste | ![](web/01-start.png) |
| Neue Klasse | ![](web/02-create.png) |
| Klasse (Schülerraster, Code) | ![](web/03-class.png) |
| Klasse, dunkel | ![](web/03-class-dunkel.png) |
| Klasse, Hochformat | ![](web/hochformat-820-class.png) |
| Code als Vollbild | ![](web/04-code.png) |
| Schüler-Detail (Anfrage wartet) | ![](web/05-detail.png) |
| Live-Bildschirm | ![](web/06-stream.png) |
| Klasseneinstellungen | ![](web/07-settings.png) |

## Schüler-App (Xiaomi Pad 7)

Seit Phase 4 sind das die echten Bildschirme mit Beispielzustand (`android/app-student/.../ui/StudentScreens.kt`).
Bilder erzeugt mit `./gradlew :android:app-student:recordRoborazziDebug`. Der Freigabe-Dialog und die
Benachrichtigungs-Illustration aus Phase 2 kommen in Phase 6 wieder dazu.

| Bildschirm | Bild |
|---|---|
| Start | ![](android/student/01-start.png) |
| Start, noch kein Profil, mit Hinweis | ![](android/student/01b-start-neu.png) |
| Mein Profil | ![](android/student/02-profil.png) |
| Beitreten | ![](android/student/03-beitreten.png) |
| Beitreten – lädt | ![](android/student/04-beitreten-laedt.png) |
| Beitreten – falscher Code | ![](android/student/05-beitreten-fehler.png) |
| Beitreten – noch kein Server | ![](android/student/05b-beitreten-ohne-server.png) |
| Server-Adresse | ![](android/student/05c-server-adresse.png) |
| Beigetreten | ![](android/student/06-beigetreten.png) |
| Beigetreten, dunkel | ![](android/student/07-beigetreten-dunkel.png) |
| Hochformat, keine Verbindung | ![](android/student/08-beigetreten-hochformat-offline.png) |
| HyperOS-Hinweis | ![](android/student/13-hyperos-hinweis.png) |

## Host-App (Galaxy S21 FE)

Quelle: `android/app-host/.../mockup/HostScreens.kt`. Bilder mit
`./gradlew :android:app-host:recordRoborazziDebug`.

| Bildschirm | Bild |
|---|---|
| Einrichtung (Tailscale) | ![](android/host/01-einrichtung.png) |
| Server läuft | ![](android/host/02-server-laeuft.png) |
| Server gestoppt, dunkel | ![](android/host/03-server-gestoppt-dunkel.png) |
| Einladungslink | ![](android/host/04-einladung.png) |

## Mockup-APKs zum Durchklicken

`./gradlew :android:app-student:assembleDebug :android:app-host:assembleDebug` erzeugt zwei APKs, die
eine Liste aller Mockup-Bildschirme zeigen (Zurück-Geste führt zur Liste):

- `android/app-student/build/outputs/apk/debug/app-student-debug.apk` → Xiaomi Pad 7
- `android/app-host/build/outputs/apk/debug/app-host-debug.apk` → Galaxy S21 FE

## Bewusste Entscheidungen

- Eine gedeckte Akzentfarbe (Blau) in allen drei Oberflächen, sonst Grautöne mit leichtem Blaustich.
- Systemschrift: im Web `-apple-system`, auf Android Roboto.
- Statusfarben nur für Zustände: Grün = online, Grau = offline, Blau mit Ring = Freigabe aktiv.
- Kein QR-Code zum Beitreten: Die Schüler-App nutzt absichtlich keine Kamera. Der Code wird abgetippt.
- „Name doppelt“ und „Neu“ helfen der Lehrkraft, falsche Beitritte zu erkennen (siehe `security.md` §4.5).
- Avatare sind eigene, einfache Formen (Welle, Berg, Sonne, Mond, Stern, Blatt, Wolke, Planet), in Web und
  Android aus demselben 100×100-Raster.

## TODO

- `TODO:` App-Symbole (Launcher-Icons) für beide Apps – kommen in Phase 4.
- `TODO:` Echter QR-Code in der Host-App (ZXing) – Phase 3; im Mockup ist es ein Platzhaltermuster.
