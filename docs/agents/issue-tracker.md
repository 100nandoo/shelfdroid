# Issue tracker: local markdown

Issues, specs, and ticket-like work for this repo live as markdown files under `docs/scratch/`.

## Conventions

- Create a new feature work item as a directory under `docs/scratch/<feature>/`.
- Save the feature spec as `docs/scratch/<feature>/spec.md`.
- Save each ticket as `docs/scratch/<NN>-<slug>.md`, numbered from `01` in dependency order.
- Use one file per ticket; do not combine the ticket breakdown into a single file.
- Read existing work from the relevant feature's `spec.md` and numbered ticket files under `docs/scratch/`.
- Update status by editing the file directly.
- Cross-reference related work with relative markdown links where useful.

## When a skill says "publish to the issue tracker"

Create `docs/scratch/<feature>/spec.md`. If publishing tickets for a feature, create one `docs/scratch/<NN>-<slug>.md` file per ticket using the generic skill's numbering and content template.

## When a skill says "fetch the relevant ticket"

Open the corresponding numbered file from `docs/scratch/`. If the request is about the feature spec, open `docs/scratch/<feature>/spec.md`.

## Work lifecycle and archives

- Future proposals and research live under `docs/plan/`. Published feature specs and implementation tickets use the `docs/scratch/` paths above. Link related plans from the feature spec rather than maintaining two copies.
- Record feature lifecycle as `Lifecycle: proposed`, `active`, `blocked`, `completed`, or `abandoned` near the top of its spec, with an `Updated: YYYY-MM-DD` line. Keep implementation and verification details in the existing `Status:` text.
- Ticket `Status:` values retain the triage vocabulary in `triage-labels.md`; feature lifecycle does not replace ticket triage.
- Keep work with outstanding acceptance or verification checks open, even when implementation is present.
- Before archiving, update current architecture or usage guidance for any enduring behavior, and record completion evidence or the reason for abandonment. Move the related spec, tickets, and reports together under `docs/archive/<feature>/`, preserving filenames and repairing links.
- Read `docs/archive/README.md` when looking for completed work or historical specs. Archives are excluded from open-work scans; archived specifications may describe behavior superseded by later changes.
