# Labelific Print — fork notes

## Branch strategy

- `master` — mirror of `upstream/master` (QZ Tray upstream). Never
  commit here directly. Fast-forward from upstream when a new QZ
  release lands.
- `labelific` — the active development branch. Every Labelific
  customization is a commit here. Default branch on the fork.

Upstream tracked as remote `upstream = https://github.com/qzind/tray`.

## Pulling an upstream release

QZ tags releases like `v2.2.7`, `v2.2.8`. When one lands:

```bash
git fetch upstream --tags
git checkout master
git reset --hard upstream/master
git push --force-with-lease origin master

git checkout labelific
git rebase v2.2.7          # or whatever the new tag is
# resolve any conflicts — usually just in Constants.java + project.properties
git push --force-with-lease origin labelific
```

Rebasing (not merging) keeps `labelific` a linear series of "Labelific
customization" commits on top of the current QZ release. Each merge
window is roughly proportional to how much QZ touched
`Constants.java`, `ant/project.properties`, and the installer
templates under `ant/{apple,linux,windows}/` — the same files we
edit for branding.

## What lives on the `labelific` branch

Commit series, kept small and topical so rebases stay tractable:

1. `brand: rebrand strings to Labelific Print` — project.properties,
   Constants.java, README, JS alias
2. `brand: replace tray + installer icons` — assets/branding/*.svg,
   then run assets/branding/create_branding.sh to regenerate ICO/ICNS
3. `trust: bake Labelific CA into signing chain` — replaces the QZ
   authcert; see docs/TRUST-MODEL.md for the openssl playbook
4. `installer: sign Windows MSI with SimplySign` — GitHub Actions
   workflow, cert stored as encrypted repo secret
5. `installer: sign + notarize macOS pkg` — same, with the Apple
   Developer cert
6. `update: point auto-update at Cloudflare R2` — Constants.
   VERSION_CHECK_URL + a versions.json schema published to R2

Each of these lives as its own commit (or small commit series) so
rebasing over an upstream release only conflicts on the layers that
QZ touched.

## Rule of thumb: what NOT to change

- **The Java package `qz.*`** — renaming it would touch hundreds of
  files and make every upstream merge a fight. All identity strings
  live in ant properties + Constants.java + resource bundles.
- **`LICENSE.txt`** — LGPL-2.1 must be preserved verbatim.
- **QZ's copyright headers in individual source files** — add
  Labelific's copyright as a second line under theirs, never
  replace.
- **`assets/signing/sign-message.*` sample files** — these are
  QZ's customer-facing examples in ~20 languages showing how to
  HMAC-sign print requests. Keep only the languages Labelific
  customers actually need (php, js at minimum) and delete the rest
  in one commit so upstream rebases don't keep re-adding files we
  don't ship.

## License hygiene

The fork is LGPL-2.1. Modifications inherit it and their source
must be available to anyone who receives a binary — a public GitHub
fork satisfies that.

The Labelific proprietary web app and rendering API live in a
separate repo and communicate with the print agent over local
HTTPS/WebSocket only. That process boundary is what keeps the web
side from being a derivative work of the LGPL agent. Do NOT
symlink or check the fork into any proprietary repo, and do NOT
copy Java source out of it for reference — if a shared constant is
needed on both sides, define it independently.
