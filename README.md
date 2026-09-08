# Labelific Print

Local print bridge for the [Labelific](https://labelific.com) label
workflow. Sits on the customer's PC (or on our Pi kiosk appliance),
accepts print jobs from any Labelific web app over a local
HTTPS/WebSocket loopback, and hands them to the physically attached
printer without an OS dialog.

## Fork status

This is a fork of [QZ Tray](https://github.com/qzind/tray) (LGPL-2.1).
Upstream `qzind/tray` is tracked as the `upstream` remote and merged
regularly. All Labelific customizations live on the `labelific` branch:

- Labelific branding (name, icons, About dialog, installer strings,
  Windows service name, macOS bundle id)
- Trust model swap — Labelific's CA public cert is baked in at build
  time; our web apps' server-signed print requests validate silently
  against it, no user dialog
- Auto-update channel repointed at Labelific's release feed

`main` mirrors upstream so `git merge upstream/main` stays trivial.
See [.github/labelific-notes.md](.github/labelific-notes.md) for the
branch strategy and how upstream releases get picked up.

## License

LGPL-2.1, inherited from QZ Tray. See [LICENSE.txt](LICENSE.txt) for
the full text. QZ Industries' copyright notices in individual files
are preserved; Labelific's modifications are additionally
Copyright © Labelific.

## Original QZ Tray docs

For build instructions, JS SDK usage, and platform-specific notes,
QZ's own documentation still applies (only the identity strings
change under Labelific):

- [Getting Started](https://github.com/qzind/tray/wiki/getting-started)
- [Compiling](https://github.com/qzind/tray/wiki/compiling)
- [Install dependencies](https://github.com/qzind/tray/wiki/install-dependencies)

The upstream README lives at
[github.com/qzind/tray](https://github.com/qzind/tray/blob/master/README.md).
