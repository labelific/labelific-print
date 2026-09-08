# assets/signing/

This directory holds three unrelated concerns; keep them straight:

## 1. `labelific-ca.crt` — the CA public certificate

Trust anchor baked into the JAR at build time (via ant's
`authcert.use` property, which copies it to `override.crt` inside
the distribution). Any print request whose signature chains to this
cert is silently authorized — no browser dialog.

Committed publicly. Generated once via the openssl playbook in
[../../docs/labelific/TRUST-MODEL.md](../../docs/labelific/TRUST-MODEL.md).

The CA **private** key (`labelific-ca.key`) is NEVER in the repo. If
you see one, treat it as a leak and rotate immediately.

## 2. `sign-message.*` — customer sample code

QZ shipped example implementations of the HMAC-signing dance in ~20
languages so that any web stack could integrate. We inherit them from
upstream. Labelific customers today are all PHP/JS (LabelDesigner
web) and never touch these, so keeping them costs nothing and helps
upstream rebases stay clean.

If you ever want to prune, keep at minimum:

- `sign-message.php` — matches the LabelDesigner web stack.
- `sign-message.js` — matches the JS SDK's example flow.

Delete the rest in ONE topical commit (`assets/signing: prune
unused sample code`) so upstream rebases only fight you over the
delete once.

## 3. Historical: `qz.ks` and `private.properties` under `ant/private/`

These are QZ's development keystore for code-signing the JAR itself
(different from the auth cert above). Kept for reproducibility of
upstream builds. For Labelific's signed releases, we use SimplySign
(Windows) and the Apple Developer cert (macOS) — see the installer
signing docs (Phase 2 follow-up).
