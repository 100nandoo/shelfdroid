# 06 — Consolidate the Library event projection boundary

**What to build:** Library administration requests and external Library events use one catalog-facing
Library projection, while socket ownership remains independently testable outside the event
repository's file boundary.

**Blocked by:** None — can start immediately.

**Status:** ready-for-agent

- [ ] All server Library payloads use one shared conversion to the Library administration model.
- [ ] Request and socket-event paths preserve identical media type, identity, name, and display
      order semantics.
- [ ] The Library event repository is the only class declared in its repository file, with socket
      ownership extracted without changing its application-scoped lifecycle.
- [ ] Existing event parsing, deduplication, shared socket ownership, and reconciliation tests pass.
