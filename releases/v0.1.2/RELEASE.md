# snap-meal v0.1.2

Archive date: 2026-06-25

This directory stores the source snapshot for the Web user ordering interaction update.

Highlights:

- Added Dock-style animated category navigation in the Web user ordering page.
- Added AnimatedList-based dish list scrolling effects.
- Added add-to-cart feedback: success check mark, image hop, cart fly-dot, and error shake.
- Added expandable cart panel above the bottom cart bar.
- Added cart item increment/decrement controls backed by real cart APIs.
- Rebuilt frontend static assets for Spring Boot.
- Updated README documentation for v0.1.2.

Files:

- `snap-meal-v0.1.2-source-20260625.zip`: source archive
- `snap-meal-v0.1.2-source-20260625.sha256.txt`: SHA-256 checksum

Excluded local/generated items:

- `.git`
- `.m2`
- `.claude`
- `.tmp-release`
- `frontend/node_modules`
- `target`
- `dist`
- `releases`
- `output`
- `*.log`
- `codex-prompts.txt`
- runtime database files under `data/` such as `*.mv.db`

Note: this archive is intended as a source snapshot, not a live runtime database snapshot.
