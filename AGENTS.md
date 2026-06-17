## Approach

- Always use Context7 when I need library/API documentation, code generation, setup or configuration
  steps without me having to explicitly ask.
- Think before acting. Read existing files before writing code.
- Be concise in output but thorough in reasoning.
- Prefer editing to rewriting whole files.
- Do not re-read files you have already read.
- Test your code before declaring done.
- No sycophantic openers or closing fluff.
- Keep solutions simple and direct.
- User instructions always override this file.
- For icons, fetch from Google Fonts icons in the Rounded
  style: https://fonts.google.com/icons?icon.style=Rounded

## Agent skills

### Issue tracker

Issues for this repo are tracked in GitHub Issues via the local `gh` CLI context. See
`docs/agents/issue-tracker.md`.

### Triage labels

This repo uses the default triage label vocabulary: `needs-triage`, `needs-info`, `ready-for-agent`,
`ready-for-human`, and `wontfix`. See `docs/agents/triage-labels.md`.

### Domain docs

This repo is configured as a single-context repo with root-level domain docs and ADR lookup rules.
See `docs/agents/domain.md`.

### Orchestration labels

This repo uses the default orchestration-label scheme with stage-based `f<N>-<C>` and `p<N><L>-<C>`
labels. See `docs/agents/orchestration-labels.md`.
