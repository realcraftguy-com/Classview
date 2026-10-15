# Hosting, Kosten und Testbetrieb

Stand: Phase 1, Revision 4. Gehostet wird alles von der **Host-App auf dem Galaxy S21 FE 5G zuhause** –
ohne Root, ohne Mietserver und **ohne Einstellungen am Heim-Router**. iPad und Tablet in der Schule
erreichen es über **Tailscale Funnel** unter einer festen Adresse `https://classview.<tailnet>.ts.net`.
Details zur Technik: `architecture.md` §4.1, §5 und §10.

---

## 1. Was läuft wo

| Ort | Gerät | App | Aufgabe |
|---|---|---|---|
| Zuhause | **Galaxy S21 FE 5G** (One UI 8) | **Host-APK** („ClassroomView Host“) | Server, Lehrer-Website, Klassen und Profilbilder, eingebetteter Tailscale-Knoten mit Funnel |
| Internet | Tailscale | – | leitet verschlüsselte Verbindungen aus der Schule zum S21 FE weiter (kann sie nicht lesen) |
| Schule | **Xiaomi Pad 7** | **Schüler-APK** („ClassroomView“) | Profil, Beitritt, Freigabe, sendet das Video direkt ans iPad |
| Schule | **iPad der Lehrerin** | keine App – **Safari** bzw. Home-Bildschirm-Verknüpfung | Lehrer-Oberfläche, empfängt das Video |

Datenwege:

- **Steuerdaten** (Beitritt, Liste, Anfragen, WebRTC-Signalisierung, Profilbilder): Schule → Tailscale-Relais
  → S21 FE. Wenige KB/s, Ende-zu-Ende verschlüsselt.
- **Video**: nur innerhalb des Schul-WLANs, Tablet → iPad.

---

## 2. Kosten

| Posten | Kosten |
|---|---|
| Server | **0 €** – das S21 FE |
| Tailscale (Personal-Tarif inkl. Funnel) | **0 €** |
| Adresse und Zertifikat (`…ts.net`) | **0 €** |
| Strom fürs S21 FE im Dauerbetrieb | wenige Euro pro Jahr |
| Datenvolumen Heimanschluss | vernachlässigbar (kein Video) |
| GitHub Actions zum Bauen der APKs | 0 € im Rahmen der Freiminuten |

---

## 3. Einrichtung (Kurzfassung, ausführlich in Phase 3)

1. **Tailscale-Konto** anlegen (erledigst du gerade). Im Adminbereich – Anleitung folgt in Phase 3 –
   HTTPS-Zertifikate und Funnel erlauben und einen Auth-Key erzeugen.
2. **S21 FE:** Host-APK installieren (aus GitHub), Auth-Key eintragen, Benachrichtigungen erlauben,
   Akku-Ausnahme bestätigen, Samsung-Einstellungen laut Hinweis (`architecture.md` §10). Dauerhaft ans
   Ladegerät.
3. Die Host-App zeigt die Adresse und „Von außen erreichbar“, sobald alles passt.
4. **Lehrerin:** bekommt den **Einladungslink** aus der Host-App (z. B. per Nachricht), öffnet ihn in Safari
   → *Teilen → Zum Home-Bildschirm*. Mehr muss sie nicht einrichten.
5. **Pad 7:** Schüler-APK installieren, HyperOS-Hinweise abarbeiten (`architecture.md` §9.3), Profil anlegen,
   Code eingeben.

APKs: GitHub Actions baut bei jedem Push beide APKs. Du lädst sie auf dem jeweiligen Gerät herunter und
erlaubst einmalig „Installation aus unbekannten Quellen“ für Browser bzw. Dateimanager.

---

## 4. Entwickeln und Testen ohne Handy

Der Server ist ein reines Kotlin/JVM-Modul und läuft auch auf einem PC:

- `./gradlew :backend:server:run` startet ihn lokal (ohne Tailscale, nur `http://localhost`).
- Web-App per `npm run dev` (Vite) mit Weiterleitung von `/api` und `/ws` auf den Server.
- Die automatischen Server-Tests laufen auf dem PC und in GitHub Actions.
- Die Schüler-App im Debug-Build kann eine andere Server-Adresse (auch `http://`) nutzen.

---

## 5. Typische Stolpersteine

| Problem | Ursache / Lösung |
|---|---|
| In der Schule „Server nicht erreichbar“ | S21 FE aus, nicht im WLAN oder schläft → Ladegerät, Samsung-Akku-Einstellungen; Internet zuhause gestört; Funnel im Tailscale-Adminbereich nicht erlaubt |
| Host-App meldet „Tailscale nicht angemeldet“ | Auth-Key abgelaufen oder widerrufen, bevor der Knoten angemeldet war → neuen Key erzeugen |
| Anfragen kommen am Tablet nicht an | HyperOS hat die Schüler-App beendet → Autostart, „Keine Einschränkungen“, App sperren |
| Mehrere Schüler können nicht gleichzeitig beitreten | zu strenges Rate-Limit (alle teilen sich die Schul-IP) → wird in Phase 3 so ausgelegt, dass das nicht passiert |
| Kein Bild, obwohl „Freigabe läuft“ | iPad und Tablet nicht im selben Schul-WLAN, oder Safari findet ohne STUN keinen Weg (Prüfpunkt Phase 7) |
