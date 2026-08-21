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
