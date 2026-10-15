# Einrichtung und Test

Stand: Phase 4. Diese Anleitung wächst mit jeder Phase.

## Überblick

| Gerät | Was | Phase 3 |
|---|---|---|
| Galaxy S21 FE (zuhause) | **ClassroomView Host** – Server + Lehrer-Website + Tailscale | voll funktionsfähig |
| iPad der Lehrerin | Safari – Lehrer-Web-App | Klassen anlegen, Code zeigen, Schülerliste (lädt alle 5 s neu; live ab Phase 5) |
| Xiaomi Pad 7 | **ClassroomView** – Schüler-App | Profil, Avatar/Foto, Beitritt per Code, gespeicherte Klasse, HyperOS-Hinweise |

## 1. APKs besorgen

- Bei jedem Push baut GitHub Actions beide APKs: Repository → **Actions** → letzter Lauf von „Build“ → unten
  unter *Artifacts* **classview-apks** herunterladen (ZIP mit beiden APKs). Dafür musst du bei GitHub
  angemeldet sein.
- Alternativ schicke ich sie dir direkt.
- Beim ersten Mal auf dem Gerät „Installation aus unbekannten Quellen“ für den Browser bzw. die Dateien-App erlauben.

> **Einmalig:** Die Mockup-APKs aus Phase 2 sind mit einem anderen Schlüssel signiert. Deinstalliere sie,
> bevor du die neuen installierst (sie enthalten keine Daten). Ab jetzt lassen sich neue Versionen einfach
> darüber installieren – Klassen und Anmeldung bleiben erhalten.

## 2. Tailscale vorbereiten (einmalig, im Browser)

Alles unter **login.tailscale.com** (am Handy geht das auch, am Computer ist es bequemer).

1. **Anmelden** (z. B. mit Google oder GitHub). Beim ersten Mal fragt Tailscale, ein Gerät hinzuzufügen –
   das kannst du überspringen.
2. **DNS** (oben im Menü):
   - **MagicDNS** einschalten, falls noch aus.
   - Weiter unten **HTTPS Certificates → Enable HTTPS** einschalten.
   - Notiere den **Tailnet-Namen** (z. B. `tail4b2c1.ts.net`).
3. **Access controls**: Die Richtlinie muss Funnel erlauben. In neuen Tailnets steht das meist schon drin.
   Prüfe, ob es einen Abschnitt wie diesen gibt, und füge ihn sonst hinzu:
   ```json
   "nodeAttrs": [
     { "target": ["autogroup:member"], "attr": ["funnel"] }
   ]
   ```
   Speichern.
4. **Settings → Keys → Generate auth key**: Standardwerte lassen (nicht wiederverwendbar, nicht
   „ephemeral“), **Generate key**, den Key (`tskey-auth-…`) kopieren. Er wird nur einmal gebraucht.

## 3. Host-App auf dem S21 FE

1. **ClassroomView Host** öffnen, Benachrichtigungen erlauben.
2. Auth-Key einfügen → **Verbinden**. Nach etwa 10–60 Sekunden steht oben **„Server läuft“** mit der Adresse
   `classview.<tailnet>.ts.net`. Beim allerersten Start kann das Zertifikat etwas länger dauern.
3. **Wichtig – Schlüsselablauf ausschalten:** Im Tailscale-Adminbereich unter **Machines** taucht jetzt
   `classview` auf. Dort über das Menü (…) **Disable key expiry** wählen. Sonst meldet Tailscale das Gerät
   nach 180 Tagen ab und der Server ist nicht mehr erreichbar.
4. **Akku-Optimierung**: In der App bei „Akku-Optimierung“ auf **Beheben** tippen und erlauben. Dann
   **Hinweise für Samsung-Akku** abarbeiten. Handy ans Ladegerät.
5. Die Zeile **„Von außen erreichbar“** wird nach spätestens einer Minute grün.

Klappt etwas nicht: oben rechts **Protokoll** öffnen – dort steht, woran es hängt. Typische Meldungen:

| Meldung | Lösung |
|---|---|
| „HTTPS-Zertifikate sind … nicht eingeschaltet“ | Schritt 2.2 |
| „Funnel ist für dieses Gerät nicht erlaubt“ | Schritt 2.3, danach Server aus- und einschalten |
| „Der Auth-Key ist ungültig oder abgelaufen“ | neuen Key erzeugen, in der App **Tailscale neu einrichten** |
| hängt lange bei „Wird gestartet …“ | Internet am Handy prüfen; Protokoll ansehen |

## 4. Lehrer-Web-App auf dem iPad

1. In der Host-App **Einladungslink für Lehrkraft** → **Link teilen** (z. B. per Nachricht) oder den QR-Code
   mit der iPad-Kamera scannen.
2. Link in **Safari** öffnen. Es erscheint „Einladung gespeichert“.
3. *Teilen → Zum Home-Bildschirm*, dann die App vom Home-Bildschirm öffnen.
   **Achtung:** Home-Bildschirm-App und Safari-Tab haben getrennten Speicher. Am einfachsten: den
   Einladungslink direkt in der Home-Bildschirm-App noch einmal öffnen bzw. die Klasse dort anlegen.
4. **Neue Klasse** → Name, optional Name der Lehrkraft, Code-Ablauf → **Erstellen**.
5. Code erscheint groß. **Schüler hinzufügen** zeigt ihn als Vollbild.

## 5. Test für Phase 3

| Schritt | Erwartung |
|---|---|
| Host-App: Server einschalten | „Server läuft“, „Von außen erreichbar: ja“ |
| iPad (im Schul-WLAN oder Mobilfunk): Einladungslink öffnen | „Einladung gespeichert“ |
| Klasse anlegen | 6-stelliger Code ohne 0/O/1/I/L, Ablaufzeit sichtbar |
| **Erneuern** → **Alten Code ungültig machen** | neuer Code |
| **Einstellungen** → Beitritt pausieren / Code-Ablauf ändern | wird sofort übernommen |
| Wiederherstellungslink kopieren und in einem anderen Browser öffnen | Klasse öffnet sich dort; unter „Angemeldete Lehrer-Geräte“ stehen zwei Geräte |
| Host-App zeigt unter „Auf diesem Server“ | 1 Klasse |
| Host-App ausschalten, iPad neu laden | „Server nicht erreichbar“ |

## 6. Schüler-App auf dem Xiaomi Pad 7 (Phase 4)

1. **ClassroomView** installieren und öffnen.
2. **Profil anlegen**: Name (Vorname oder Spitzname), dann einen Avatar wählen oder **Foto aus der Galerie**
   (Android-Fotoauswahl, keine Speicher-Berechtigung nötig). → **Fertig**.
3. **Klasse beitreten**. Beim ersten Mal fragt die App nach der **Server-Adresse**: Sie steht auf dem iPad
   unter dem großen Code (z. B. `classview.tail4b2c1.ts.net`). Die App prüft sie und merkt sie sich.
4. Code eingeben. Großschreibung passiert automatisch, Trennzeichen sind egal; tippst du 0, O, 1, I oder L,
   weist die App darauf hin, dass es diese Zeichen in Codes nicht gibt.
5. Nach dem Beitritt zeigt die App die **HyperOS-Hinweise**. Die Knöpfe öffnen die passenden Einstellungen;
   „Autostart“ und „App sperren“ kann die App nicht selbst prüfen – dort tippst du auf den Kreis, wenn es
   erledigt ist. Später erreichbar über **Einstellungen für HyperOS**.

### Test für Phase 4

| Schritt | Erwartung |
|---|---|
| Ohne Namen „Klasse beitreten“ | nicht möglich, App führt zuerst ins Profil |
| Falsche Server-Adresse eintragen | verständliche Fehlermeldung, nichts wird gespeichert |
| Falschen Code eingeben | „Diesen Code gibt es nicht …“ |
| Abgelaufenen Code (am iPad Ablauf „1 Stunde“ und warten) bzw. pausierten Beitritt | eigene Meldung |
| Richtigen Code eingeben | „Verbunden“, Klassenname und Lehrkraft; auf dem iPad erscheint die Karte nach ≤ 5 s mit Name und Bild |
| Profil ändern (Name, Avatar, Foto) | iPad zeigt die Änderung nach ≤ 5 s |
| App schließen (aus „Zuletzt verwendet“ wischen) und neu öffnen | App ist ohne Code wieder in der Klasse |
| WLAN aus, Profil ändern, WLAN an | „Keine Verbindung“, danach wird die Änderung automatisch übertragen |
| Am iPad „Code erneuern“ | Tablet bleibt in der Klasse |
| Am iPad Schüler entfernen | Tablet zeigt nach ≤ 30 s „Du wurdest aus der Klasse entfernt.“ |
| „Klasse verlassen“ | Karte verschwindet auf dem iPad |
| Tablet drehen | Hoch- und Querformat funktionieren |

Noch nicht in Phase 4: dauerhafte Verbindung im Hintergrund mit Statusleisten-Symbol und sofortiger
Online-Status (Phase 5), Freigabe-Anfragen (Phase 6).

### Ohne Tablet testen

Beitreten geht auch per `curl`:

```bash
curl -X POST https://classview.<tailnet>.ts.net/api/join \
  -H 'Content-Type: application/json' -d '{"code":"K7P4XM","displayName":"Test","avatarPreset":"sonne"}'
```

Der Schüler erscheint dann innerhalb von 5 Sekunden auf dem iPad.

## 7. Entwickeln am PC

```bash
./gradlew :backend:server:test          # Server-Tests
./gradlew :android:app-student:testDebugUnitTest   # Schüler-App gegen echten Server, Screenshots
./gradlew :backend:server:run           # Server auf http://127.0.0.1:8080, Einladungslink steht in der Ausgabe
cd web && npm ci && npm run dev         # Web-App mit Hot-Reload (leitet /api an Port 8080 weiter)
./android/tailscale-bridge/build.sh     # tsbridge.aar (Go + Android NDK nötig)
./gradlew :android:app-host:assembleDebug :android:app-student:assembleDebug
```
