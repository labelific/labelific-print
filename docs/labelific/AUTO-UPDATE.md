# Auto-update — Labelific Print

Two options, both work today. Pick one; the docs and code cover both.

## Option A — GitHub Releases (default, zero-config)

Already wired. `Constants.VERSION_CHECK_URL` points at
`https://api.github.com/repos/labelific/labelific-print/releases`
and the tray checks for a newer release once a day (QZ's built-in
schedule).

**To publish a new version:**

```bash
# 1. Bump version in Constants.java and ant/version.xml.
# 2. Build and sign per-platform installers.
ant clean
ant nsis        # Windows MSI (SimplySign)
ant pkgbuild    # macOS pkg (codesign + notarize)
ant makeself    # Linux .sh

# 3. Tag + push.
git tag v2.2.7
git push origin v2.2.7

# 4. Create the GitHub Release from the tag. Attach the three built
#    installers as release assets. Mark as latest.
gh release create v2.2.7 \
    out/dist/labelific-print-2.2.7.msi \
    out/dist/labelific-print-2.2.7.pkg \
    out/dist/labelific-print-2.2.7.sh \
    --title "Labelific Print 2.2.7" \
    --notes-file docs/labelific/CHANGELOG.md \
    --target labelific
```

**Constraint:** GitHub Releases lists every release. If you ever want
to ship a private / customer-specific build (a signed build with an
extra whitelist, say), it has to be a separate distribution channel.
That's what R2 is for.

**Pros:** free, familiar, no infrastructure to run. Download stats
built in. Signed installers are also visible for security researchers.
**Cons:** every release is public; download traffic hits GitHub's
CDN which is fine but geographically neutral (a customer in Ostrava
downloads from GitHub's US-hosted-mostly LFS). GitHub API rate limits
apply (60 req/hour per IP unauthenticated, 5000/hour authenticated) —
irrelevant at Labelific's current scale, worth noting when we hit
thousands of installs polling daily.

## Option B — Cloudflare R2 (private downloads, custom domain)

R2 is Cloudflare's S3-compatible object store with zero egress fees
and a global CDN. Same shape of feed as GitHub Releases, but you
control what's listed.

### One-time R2 setup

1. In Cloudflare dashboard → R2 → Create bucket → name it
   `labelific-print-updates`, location Automatic.

2. Bucket → Settings → Public access → **Custom domain** → connect
   `updates.labelific.com`. Cloudflare creates the DNS record for
   you.

3. Verify the custom domain resolves:

```bash
curl -sI https://updates.labelific.com/
#  → HTTP/2 400 (or a Cloudflare-styled 404 body — either means
#    routing works; the bucket is just empty)
```

### The versions.json schema

Publish a `versions.json` at the bucket root that MIMICS GitHub's
Releases API shape — one line change on the client side (URL swap)
and everything Just Works:

```json
[
  {
    "name": "2.2.7",
    "tag_name": "v2.2.7",
    "target_commitish": "labelific",
    "html_url": "https://updates.labelific.com/releases/2.2.7/",
    "published_at": "2026-09-15T14:23:00Z",
    "assets": [
      { "name": "labelific-print-2.2.7.msi", "browser_download_url": "https://updates.labelific.com/downloads/2.2.7/labelific-print-2.2.7.msi" },
      { "name": "labelific-print-2.2.7.pkg", "browser_download_url": "https://updates.labelific.com/downloads/2.2.7/labelific-print-2.2.7.pkg" },
      { "name": "labelific-print-2.2.7.sh",  "browser_download_url": "https://updates.labelific.com/downloads/2.2.7/labelific-print-2.2.7.sh" }
    ]
  }
]
```

Order matters: newest FIRST. The client picks the first entry whose
`target_commitish` matches (see `AboutInfo.findLatestVersion()`) — so
keep the array reverse-chronological.

### Publish flow

```bash
# 1. Build the installers as in Option A.
# 2. Upload signed installers to R2 with the aws CLI (S3-compatible).
export AWS_ACCESS_KEY_ID=...            # R2 credentials from CF dashboard
export AWS_SECRET_ACCESS_KEY=...
export AWS_ENDPOINT_URL=https://<accountid>.r2.cloudflarestorage.com

aws s3 cp out/dist/labelific-print-2.2.7.msi s3://labelific-print-updates/downloads/2.2.7/
aws s3 cp out/dist/labelific-print-2.2.7.pkg s3://labelific-print-updates/downloads/2.2.7/
aws s3 cp out/dist/labelific-print-2.2.7.sh  s3://labelific-print-updates/downloads/2.2.7/

# 3. Regenerate versions.json (append new entry at the top of the array).
# 4. Upload it LAST — that's what tells every install a new version is out.
aws s3 cp versions.json s3://labelific-print-updates/versions.json \
    --content-type application/json \
    --cache-control "public,max-age=60"
```

The `max-age=60` header caps how long stale copies live at the CDN
edge — new releases propagate within a minute. Individual installer
files can safely be `max-age=31536000` (immutable, filename encodes
version).

### Repoint the client at R2

In `src/qz/common/Constants.java`:

```java
public static final String VERSION_CHECK_URL = "https://updates.labelific.com/versions.json";
public static final String VERSION_DOWNLOAD_URL = "https://updates.labelific.com/downloads/";
```

That's the whole swap. `AboutInfo.findLatestVersion()` parses the
GitHub-shaped array we're serving, so no Java parser change needed.

### Pros / cons

**Pros:** custom domain (installs poll `updates.labelific.com`, not
GitHub); private / staged rollouts (upload but don't append to
versions.json yet); zero egress fees; Cloudflare's global CDN
(Prague / Frankfurt PoPs mean CZ downloads are fast).
**Cons:** you own the pipeline — a broken versions.json breaks every
install's update check. Test in a `updates-staging.labelific.com`
bucket before promoting. R2's write API is S3-compatible but the
error messages are Cloudflare-flavored; when a upload fails, check
the R2 dashboard's activity log alongside the CLI error.

## Related bug: releases from the `labelific` branch

QZ's `findLatestVersion()` filters on
`target_commitish == "master"`. Our releases are cut from the
`labelific` branch (per BRANCH-STRATEGY.md), so `target_commitish`
will be `labelific`, not `master`, and the current parser silently
skips them.

Fixed in this commit series — `AboutInfo.java` now accepts either
`labelific` (our fork) or `master` (upstream's default). See the diff
in commit `fix: accept 'labelific' branch as a valid release target`.

Applies to both Option A (GitHub Releases with the `--target labelific`
flag on `gh release create`) and Option B (versions.json entries carry
`target_commitish: "labelific"`).

## Which to pick?

- Ship Option A now. It works with the current commit and needs
  nothing new to deploy.
- Move to Option B once you have paying customers who care about (a)
  a Labelific-branded update URL or (b) selective/staged rollouts.
  The switch is a one-line `Constants.java` change plus a one-time R2
  setup — no client-side rebuild story per install.
