# LinkDeck Architecture

## Modules
- :core-design  -> tokens, theme, icons. No logic.
- :core-network -> TLS identity, pinned trust, Ktor WebSocket server/client, mDNS.
- :app          -> UI, ViewModels, pairing, permissions, foreground service, QS tile.

## Rules
- Kotlin + Jetpack Compose + Material 3, MVVM + Coroutines/Flow + Hilt.
- No GMS, Firebase, or cloud. Everything over local Wi-Fi with TLS pinned.
- UI never talks to sockets directly; it goes through a Repository.
- Strings only in strings.xml (ar + en). No hardcoded text.
- Fonts: Cairo / Readex Pro only. Never DejaVu Sans.
- Do not rename existing classes or packages.

## Protocol
- One WebSocket connection. Each message = JSON envelope:
  { "type": "...", "id": "...", "payload": {...} }
- Every new feature registers its own message types in core-network/protocol.
- Binary transfers use a separate chunked channel (see Phase 2).

## Phases
1 Done | 2 Files+transfers | 3 Notifications/clipboard/SMS | 4 Pro mode (ADB), mirroring | 5 Calls, launcher, release
