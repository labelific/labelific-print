# Trust model — Labelific Print

**Goal:** the browser's "Allow this site to print?" dialog never appears
for Labelific's own web apps (labelific.com, stitkomat.cz, and any
dev / staging subdomains). Third-party sites still get the dialog —
that's the point.

## How it works, in one paragraph

QZ Tray already ships a mechanism for what QZ calls "community builds":
if the JAR contains a file named `override.crt` (a public certificate
in PEM form), the tray treats any print request whose signature chains
to that cert as pre-authorized. No dialog. `Constants.OVERRIDE_CERT`
names the file. `build.xml`'s `override-authcert` target copies
`${authcert.use}` (a path we pass at build time) into
`${dist.dir}/override.crt`. So the whole swap for our fork is:

1. Generate a Labelific CA (root key + public cert). One-off. Private
   key stays offline.
2. Point `authcert.use` at the CA's public cert when we build. The
   JAR ships with our cert baked in.
3. Generate a signing keypair for each Labelific web host and sign it
   with the CA. Web hosts sign print requests with these keys.
4. Web apps' HMAC-signed print calls now validate silently against the
   embedded CA.

Steps 1 and 3 are what this document is about. Step 2 is one build
flag. Step 4 is already how the LabelDesigner web-side code is
structured — it uses `shared/src/GoPay.php`'s HTTP-signing helpers as
the pattern; the same shape gets reused for print-bridge signatures.

## What YOU do vs what stays committed

- **The CA private key** (`labelific-ca.key`) never enters the repo
  and never enters this Cowork session. Keep it offline in a
  password-protected file (or on a YubiKey / hardware token in the
  long run). If it leaks, anyone can bypass the browser dialog for
  every Labelific Print install in the world — regenerate the CA and
  ship a new build to invalidate it.
- **The CA public cert** (`labelific-ca.crt`) can be committed
  publicly; it's shipped inside the JAR at build time. Anyone who
  extracts the JAR sees it. Convention: put it at
  `assets/signing/labelific-ca.crt` and reference it from the
  build.
- **Per-host signing keys** (`labelific-web.key`, `stitkomat-web.key`,
  …) live on the web servers that use them, in
  `LabelDesigner/shared/config/print-signing/`. Never in this repo.
- **Per-host signing certs** (`labelific-web.crt`, etc.) are shipped
  by the web servers to the browser as part of the print request; they
  chain to the CA and can be regenerated any time (rotate every ~2
  years is a sensible cadence).

## One-time: generate the Labelific CA

Do this on a machine you trust, offline is nice, once. Everything
below is `openssl` on macOS/Linux — no other tools.

```bash
# Somewhere OUTSIDE any git repo (a folder in your password manager,
# an encrypted USB stick, etc). Never commit the *.key files.
mkdir -p ~/Documents/Secrets/labelific-ca
cd ~/Documents/Secrets/labelific-ca

# 1. Root CA private key — 4096-bit RSA is the sensible default for
#    a long-lived root that signs sparingly. The -aes256 pass-encrypts
#    it at rest; store the passphrase in your password manager.
openssl genrsa -aes256 -out labelific-ca.key 4096

# 2. Root CA public cert — self-signed, 20 years.
openssl req -x509 -new -nodes -key labelific-ca.key -sha256 -days 7300 \
    -out labelific-ca.crt \
    -subj "/C=CZ/O=Labelific/CN=Labelific Print CA"

# 3. Sanity check.
openssl x509 -in labelific-ca.crt -noout -text | head -15
```

Copy `labelific-ca.crt` into the repo:

```bash
cp labelific-ca.crt ~/Documents/Webs/labelific-print/assets/signing/
```

That file gets committed. `labelific-ca.key` stays where it is,
locked to your local disk.

## Per-host: generate a signing keypair

For each Labelific web property (labelific.com, stitkomat.cz, any
dev / staging host that will drive prints), issue a signing cert
chained to the CA.

```bash
cd ~/Documents/Secrets/labelific-ca

# labelific.com signing key (kept on the web server that uses it).
HOST=labelific.com

openssl genrsa -out ${HOST}-web.key 2048

openssl req -new -key ${HOST}-web.key -out ${HOST}-web.csr \
    -subj "/C=CZ/O=Labelific/CN=${HOST}"

openssl x509 -req -in ${HOST}-web.csr -CA labelific-ca.crt \
    -CAkey labelific-ca.key -CAcreateserial -sha256 \
    -days 730 -out ${HOST}-web.crt

# Sanity: chain verifies?
openssl verify -CAfile labelific-ca.crt ${HOST}-web.crt
#  → ${HOST}-web.crt: OK
```

Repeat with `HOST=stitkomat.cz`, dev/staging hosts, etc. Ship the
`.key` and `.crt` pair to each host's config directory:

```bash
# on the labelific.com VPS:
mkdir -p /var/www/labels/shared/config/print-signing
chown www-data:www-data /var/www/labels/shared/config/print-signing
chmod 700 /var/www/labels/shared/config/print-signing
# scp the .key + .crt into that directory, then chmod 400 the key.
```

The Labelific web-side PHP will read these when signing print requests
(see the `PrintBridge` class that lands with Phase 2's web-side work).

## Build the JAR with the CA baked in

Once `assets/signing/labelific-ca.crt` is committed:

```bash
cd ~/Documents/Webs/labelific-print
ant clean
ant -Dauthcert.use=assets/signing/labelific-ca.crt
```

Verify the JAR now ships `override.crt`:

```bash
unzip -l out/dist/labelific-print.jar | grep override.crt
```

For CI / repeat builds, pin the property in `ant/project.properties`
alongside the other project.* settings so `ant` alone does the right
thing:

```
# Bake the Labelific CA into every build so signed requests from our
# own web apps skip the "Allow this site to print?" dialog silently.
authcert.use=assets/signing/labelific-ca.crt
```

Just note the same file is copied to `${dist.dir}/override.crt`; the
runtime looks it up by that name.

## DNS: `localhost.labelific.com` → 127.0.0.1

QZ resolves `localhost.qz.io` to 127.0.0.1 in DNS as a workaround for
the browser's "no plaintext HTTPS certs for IPs" rule — it lets the
locally-installed cert be valid for a real DNS name that just happens
to point at loopback. Do the same for us.

**Cloudflare DNS record on labelific.com:**

| Type | Name | Content | Proxy |
| ---- | ---- | ------- | ----- |
| A    | localhost | 127.0.0.1 | DNS-only (grey cloud, not proxied) |

Verify from any machine:

```bash
dig +short localhost.labelific.com
#  → 127.0.0.1
```

Then swap the JS default in `js/qz-tray.js`:
`localhost.qz.io` → `localhost.labelific.com`. That change lands with
this commit series.

## Where the browser dialog gets skipped (proof-of-work test)

Once the JAR is rebuilt with `override.crt`:

1. Install Labelific Print locally (macOS pkg or run the JAR directly).
2. Serve a test page from `https://localhost.labelific.com:port/` with
   a Labelific-signed print request.
3. First call goes through — no dialog. Compare to an unsigned call
   (or a request from a random unrelated site) which still shows the
   allow/block prompt.

The Labelific web-side code that signs requests lives in the
LabelDesigner monorepo (`shared/src/PrintBridge.php`) — that's a
separate deliverable, but it's the missing counterpart to this
document. Until it's in place, the JAR trusts the CA silently but
nothing's yet sending signed calls.

## Key rotation

- **CA:** every 5–10 years, or immediately if the key leaks.
  Rotating requires shipping a new JAR to every install (bundled with
  the new CA cert). Auto-update covers that flow — see
  [AUTO-UPDATE.md](AUTO-UPDATE.md).
- **Per-host signing certs:** every 1–2 years. Issue a new cert
  chained to the same CA, deploy to the host, no client changes
  needed (the CA is what's baked into the JAR; individual certs are
  discovered at request time).

## Don't do this

- Don't check `labelific-ca.key` into git under any circumstances.
  `.gitignore` covers `*.key` already; leave it alone.
- Don't reuse the CA private key on multiple machines — one
  authoritative location, backed up encrypted somewhere else.
- Don't sign a cert with `Subject: *` or any wildcard that would
  cover attacker-controlled subdomains. Use exact hostnames.
- Don't extend the CA cert lifetime past 20 years; browsers may drop
  support for extremely long-lived roots.
- Don't let the CA-signed per-host certs live forever. If a web server
  gets compromised, the attacker gets a valid signing key. Rotation
  limits blast radius.
