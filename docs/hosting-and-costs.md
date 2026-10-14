# Hosting, Kosten und Testbetrieb

Stand: Phase 1, Revision 2. Gehostet wird alles von der **Host-App auf dem Galaxy S21 FE 5G** – ohne Root,
ohne Server, ohne Domain. Die Details zur Technik stehen in `architecture.md` §4.1, §5 und §10.

---

## 1. Was läuft wo

| Gerät | App | Aufgabe |
|---|---|---|
| **Galaxy S21 FE 5G** | **Host-APK** („ClassroomView Host“) | Server (REST + WebSocket), liefert die Lehrer-Website aus, speichert Klassen und Profilbilder, eigene Zertifizierungsstelle, mDNS-Ankündigung |
| **Xiaomi Pad 7** | **Schüler-APK** („ClassroomView“) | Profil, Beitritt, Freigabe, sendet das Video direkt ans iPad |
| **iPad** | keine App – **Safari** bzw. Home-Bildschirm-Verknüpfung | Lehrer-Oberfläche, empfängt das Video |

Alle drei müssen im **selben Netz** sein: dasselbe WLAN **oder** iPad und Tablet im Hotspot des S21 FE.

Ports auf dem S21 FE (alle über 1024, daher ohne Root möglich):

| Port | Zweck |
|---|---|
| 8443/TCP | HTTPS + WSS (Website, API, WebSocket) |
| 8080/TCP | nur CA-Zertifikat herunterladen, sonst Weiterleitung auf 8443 |
| 5353/UDP | mDNS (vom System verwaltet) |
| 3478/UDP | nur falls nötig: Mini-STUN (siehe `architecture.md` §5.3) |

---

## 2. Kosten

| Posten | Kosten |
|---|---|
| Server, Domain, Zertifikat | **0 €** – entfällt |
| Strom | vernachlässigbar (Handy hängt am Ladegerät) |
| Mobile Daten | **0** für ClassroomView – Video und Steuerung bleiben im lokalen Netz. Nur wenn iPad/Tablet über den Hotspot *zusätzlich* im Internet surfen, verbraucht das Datenvolumen. |
| Optional: GitHub | 0 € – GitHub Actions baut die APKs; für private Repositories sind monatlich Freiminuten enthalten (genaue Menge siehe GitHub-Tarif) |

**Last:** Der Server vermittelt nur Nachrichten; das Video läuft direkt Tablet → iPad. Für das S21 FE ist das
eine sehr geringe Last. Der größte Verbraucher ist das Wach-Halten von WLAN und CPU – daher Ladegerät.

---

## 3. Einrichtung (Kurzfassung, ausführlich in Phase 10)

1. **APKs installieren.** Beide APKs aus GitHub Actions (bzw. später einem GitHub-Release) herunterladen.
   Auf jedem Gerät „Installation aus unbekannten Quellen“ für den Browser bzw. Dateimanager erlauben.
2. **S21 FE:** Host-App öffnen → Server starten → Benachrichtigungen erlauben → Akku-Ausnahme bestätigen →
   Samsung-Einstellungen laut Hinweis (`architecture.md` §10). Ans Ladegerät.
3. **iPad, einmalig:** QR-Code aus der Host-App mit der Kamera scannen bzw. `http://<IP>:8080/ca` öffnen →
   Profil installieren → Zertifikat voll vertrauen (`architecture.md` §5.2) → `https://<IP>:8443` öffnen →
   *Teilen → Zum Home-Bildschirm*.
4. **Pad 7:** Schüler-App öffnen → HyperOS-Hinweise abarbeiten (`architecture.md` §9.3) → Profil → Code
   eingeben. Die App findet das S21 FE automatisch.

**Tipp:** Im Router dem S21 FE eine feste IP-Adresse zuweisen (DHCP-Reservierung). Dann ändert sich die
Adresse fürs iPad nie.

---

## 4. Entwickeln und Testen ohne Handy

Weil der Server ein reines Kotlin/JVM-Modul ist, läuft er auch auf einem PC:

- `./gradlew :backend:server:run` startet ihn auf dem PC, im Debug-Modus auch ohne TLS.
- Die Web-App läuft dann per `npm run dev` (Vite) mit Weiterleitung von `/api` und `/ws` auf den Server.
- Die automatischen Server-Tests laufen auf dem PC und in GitHub Actions.
- Die Schüler-App im Debug-Build kann eine manuelle Server-Adresse (auch `http://`) nutzen.

So lässt sich jede Phase prüfen, bevor sie aufs Handy kommt.

---

## 5. Später: Umzug auf einen Server (falls es übers Internet gehen soll)

Das S21 FE ist aus dem Internet nicht erreichbar. Soll das System später auch außerhalb eines gemeinsamen
Netzes funktionieren, läuft derselbe Server-Code auf einem kleinen VPS:

| Posten | ca. Kosten (Richtwerte, ohne Gewähr) |
|---|---|
| Kleiner VPS in der EU (2 vCPU, 2–4 GB RAM, ≥ 20 TB Traffic inkl.) | ca. 4–8 € / Monat |
| Domain (`.de`) | ca. 5–15 € / Jahr |
| TLS-Zertifikat (Let's Encrypt über Caddy) | 0 € |
| TURN-Server (coturn) auf demselben VPS | 0 € extra; ca. 1,8 GB Traffic pro Stream-Stunde, falls über TURN geleitet |

Das ist **nicht** Teil des aktuellen Plans und wird nur gebaut, wenn du es möchtest.

---

## 6. Typische Stolpersteine

| Problem | Ursache / Lösung |
|---|---|
| Tablet findet den Host nicht | mDNS im WLAN blockiert → IP manuell in der Schüler-App eintragen; oder Hotspot des S21 FE nutzen |
| iPad zeigt „Verbindung nicht privat“ | CA-Profil nicht installiert oder Vertrauen nicht eingeschaltet (`architecture.md` §5.2) |
| Alles bricht nach einiger Zeit ab | Samsung hat die Host-App schlafen gelegt → Akku „Nicht eingeschränkt“, „Nie schlafende Apps“, Ladegerät |
| Anfragen kommen am Tablet nicht an | HyperOS hat die Schüler-App beendet → Autostart, „Keine Einschränkungen“, App sperren |
| Adresse fürs iPad stimmt nicht mehr | IP des S21 FE hat sich geändert → feste IP im Router; QR-Code in der Host-App neu scannen |
| Kein Bild, obwohl „Freigabe läuft“ | Geräte erreichen sich nicht direkt (z. B. anderes WLAN) → alle ins selbe Netz bzw. in den Hotspot |
