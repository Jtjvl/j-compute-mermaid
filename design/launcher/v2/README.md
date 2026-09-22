# MermaidPocket launcher v2

Muses locked the Hybrid-C tenant-zero launcher at this prefix.

- **public_base:** https://assets.j-compute.com/mermaidpocket/launcher/v2/
- **locked_by:** Muses
- **status:** locked
- **version_label:** v2-adaptive-square
- **locked_at:** 2026-09-22T13:54:15Z
- **consumer:** Hephaestus

v1 is superseded. The capsule letterboxed launcher stub is replaced by this square full-bleed adaptive pack (full-bleed `#2A2A2E`, art in the ~66dp safe zone).

`manifest.json` in this directory is a byte copy of the locked manifest at `https://assets.j-compute.com/mermaidpocket/launcher/v2/manifest.json`. `:app:assembleDebug` runs `verifyLockedLauncher` before `preBuild` and fails when a checked-in launcher asset does not match the `sha256` and `bytes` recorded there.

## Refresh

Prefer the live HTTPS prefix. Do not copy from a box path. A local zip of the same pack is only a cross-check: if a digest disagrees with the manifest, keep the HTTPS bytes and fail.

```bash
curl -fsSL -o design/launcher/v2/manifest.json \
  https://assets.j-compute.com/mermaidpocket/launcher/v2/manifest.json
```

For each `files[]` entry, curl `url`, verify the SHA-256 equals `sha256` and the length equals `bytes`, and fail if they do not. Copy the verified file to `design/launcher/v2/<path>`. Copy each `mipmap-*` PNG onto `app/src/main/res/<path>` (same bytes). Copy `ic_launcher.xml` onto `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml`.

After that copy, keep this README: it records the public_base URL, locked_by Muses, the curl manifest → verify → copy steps, and that v1 is superseded. The pack README digest in the locked manifest is `fa41984a682ac80d52cef9b0045fd614198a96736ceb6d88489c0f019f35748a` (3248 bytes). The artwork note below is that pack README.

## Pack note

# MermaidPocket adaptive icon pack v2

Square adaptive launcher assets with a full-bleed dark charcoal background.

## Layout

- `ic_launcher_master_1024.png`: 1024x1024 master square.
- `ic_launcher_play_512.png`: 512x512 Play/F-Droid store icon.
- `mipmap-mdpi/`, `mipmap-hdpi/`, `mipmap-xhdpi/`, `mipmap-xxhdpi/`, `mipmap-xxxhdpi/`: adaptive foreground/background canvases at 108, 162, 216, 324, and 432 px (108dp density basis), plus legacy combined icons at 48, 72, 96, 144, and 192 px.
- `ic_launcher.xml`, `ic_launcher_foreground.xml`, `ic_launcher_background.xml`: Android resource stubs; copy the adaptive stub to `res/mipmap-anydpi-v26/` and aliases to `res/drawable/` as needed.

## Artwork and safe zone

The art is the diamond-fork mark in cyan, orange, purple, and lime on charcoal `#2A2A2E`. Foreground art is transparent outside the mark and centered to a safe-zone target of approximately 66dp within the 108dp adaptive canvas (~61.1% of the square), so OEM masks retain a square icon rather than a tall capsule.

The source corner samples varied slightly because of source shading (`(47, 46, 51)`, `(45, 44, 49)`, `(46, 45, 50)`, `(39, 39, 41)`), so the layers use the requested canonical solid `#2A2A2E` background. Transparent foreground extraction retained the colored nodes and bright neutral fork; no extraction issue was detected.

## PNG SHA-256

- `ic_launcher_master_1024.png` — `1a8cbc1b8c0b0b87f6dcac9257c729dcd51e4bfe7a723b430c51bcbe65682a51`
- `ic_launcher_play_512.png` — `b871cd2f2d1df6dc4d6afc5d1a58f32708ea682645acc9525f02580e78259df1`
- `mipmap-hdpi/ic_launcher.png` — `a999bd0eae75e3d4e695c6340a629e8c537a7e1cfdcd6347c8f34db482ac7cfc`
- `mipmap-hdpi/ic_launcher_background.png` — `98989fdb471784a1d11700b88569e4394e0737b5f0819d8e069f5e97b57e49d5`
- `mipmap-hdpi/ic_launcher_foreground.png` — `428a5f731bc73611cbcd8e6e2ff0fe9c957f8f431b27c5eaef6221eaf2f2f7a2`
- `mipmap-mdpi/ic_launcher.png` — `037f0e723a51b79fb506adb70b6b51875b4a2c83ff0bf6c4c24b9e1afb884a63`
- `mipmap-mdpi/ic_launcher_background.png` — `0f07f59460a4774d6325e1a7929df26009d67920e4cdfa87f3dce070bc057152`
- `mipmap-mdpi/ic_launcher_foreground.png` — `3057da96f82831b336103336a865ebd03b4cc654f129099ee06ea610bd3d8932`
- `mipmap-xhdpi/ic_launcher.png` — `049a6348745264f95902496436b3094d2aee18da671696fefb4ede3f08aa51d2`
- `mipmap-xhdpi/ic_launcher_background.png` — `6b01572403d728d96e8837f32c97f8471535b35ff60785351c4345c08c015fc6`
- `mipmap-xhdpi/ic_launcher_foreground.png` — `381f835f2151659b60113f747281af115fd2e41aebfb190af8de6cd8298b6767`
- `mipmap-xxhdpi/ic_launcher.png` — `873dffa58bf232fed8ad642b45aead99832939c7fda9e1a38a4837b3beb6fe34`
- `mipmap-xxhdpi/ic_launcher_background.png` — `52a8df494c1af428adb50d5bc1636e3e3e375603572f51850cdcadb8ff34c127`
- `mipmap-xxhdpi/ic_launcher_foreground.png` — `360d933f2cd02543b20a19b951fd308fdd5dbe95589bd5bfc12e19f81b72f634`
- `mipmap-xxxhdpi/ic_launcher.png` — `06bf3defb31585b666385ca3c01ee0e67ba0e60d111d84e1f506d3f67ec059b5`
- `mipmap-xxxhdpi/ic_launcher_background.png` — `e6956ee26d6f49a6d32810438149c46e1d6ce6c3859ba7d391271200a8d3d71e`
- `mipmap-xxxhdpi/ic_launcher_foreground.png` — `293e51e9dd1301bb794018af90d3b82c1cfec0e865d96e0e5b587c50123e4b7b`
