# LinkDeck

Universal "Super Device" alternative: one APK, two modes — **HOST** (any Android phone:
Samsung, Xiaomi, Huawei EMUI/HarmonyOS 2–4) and **CONTROLLER** (any Android tablet).

- 100% offline over local Wi-Fi / hotspot. No GMS, no Firebase, no cloud, no accounts.
- Pairing by QR (TLS fingerprint pinning, 2-minute one-time token), then mDNS
  auto-discovery and automatic reconnect.
- All traffic over TLS with self-signed, pinned certificates.
- Bilingual Arabic (RTL) + English, follows the system language by default.
- Jetpack Compose + Material 3, MVVM + Coroutines/Flow + Hilt.

## Modules
| Module | Contents |
|---|---|
| `:core-design` | Design tokens: colors, typography, spacing, line icons, theme |
| `:core-network` | TLS identity, pinned trust, Ktor WebSocket server/client, mDNS |
| `:app` | UI, pairing flows, permission center, foreground service, QS tile |

## Build
```bash
gradle wrapper --gradle-version 8.9   # once, then:
./gradlew :app:assembleDebug
```
GitHub Actions builds every push (`.github/workflows/build.yml`). For a signed release
APK, add secrets: `LINKDECK_KEYSTORE_BASE64`, `LINKDECK_STORE_PASSWORD`,
`LINKDECK_KEY_ALIAS`, `LINKDECK_KEY_PASSWORD`.

## Fonts
Drop `cairo.ttf`, `tajawal.ttf`, `amiri.ttf` (variable fonts, OFL) into
`app/src/main/assets/fonts/` — see the README there. Without them the app falls
back to the system font.

## Status
- Phase 1 ✅ (this repo): setup, design system, settings store, mode selection,
  QR pairing, mDNS, TLS connection, foreground service + shade UI + QS tile,
  hide-icon toggle, consent-first permission center, bilingual UI.
- Phase 2: file manager + transfers.
- Phase 3: notification mirror, clipboard, SMS/contacts, appearance settings.
- Phase 4: Pro mode (ADB), mirroring, audio, fallbacks.
- Phase 5: calls, app launcher, status/toggles, voice, widgets, CI release.

## Notes
- `FLAG_SECURE` content and ADB-only capabilities are surfaced in the UI instead
  of failing silently (implemented from Phase 4 onward).
