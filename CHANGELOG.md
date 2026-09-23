# Changelog

User-facing releases only. See [VERSIONING.md](VERSIONING.md).

## 0.3.3

- CI failures also POST to Hephaestus webhook

## 0.3.2

- Lean CI: pull requests assemble only and upload no artifact; push to main keeps the debug APK for 2 days
- Failure mail goes to ops-alert@j-compute.com via alerts@ SMTP

## 0.3.1

- Open/import unlinks so Save creates a new file instead of overwriting a previously linked diagram
- Picked folder with `.mmd` files is the library (no silent nest into empty MermaidPocket)
- Files rows show title + filename; Files ⋮ matches Edit/View

## 0.3.0

- Edit-view Save FAB (overwrite linked file; blank first tap creates and links)
- Blank diagram is menu-only (no lower-corner New)
- Save a copy into the library folder now appears in the Files list

## 0.2.0

- Square adaptive launcher (Muses v2)
- One Save + one Share (options on press)
- Durable `.mmd` under Documents/MermaidPocket
