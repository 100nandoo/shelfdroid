# Shared socket lifecycle for server tasks

ShelfDroid will track long-running **Server tasks** in an application-scoped, operation-agnostic repository that bootstraps from the Audiobookshelf HTTP task list and then reconciles socket task events. The repository is not coupled to Library scan or Book matching, although those are the only task types surfaced by the Library administration UI in this phase. The singleton socket connection will support independent subscriptions and shared connection ownership instead of allowing individual screens to replace listeners or disconnect other consumers, because Library administration tasks must survive navigation and coexist with podcast and future real-time features. ShelfDroid will not poll for task state: it reloads the HTTP task snapshot after socket reconnection or explicit user refresh to recover missed events.

**Consequences**

Screens consume task state rather than owning raw socket callbacks. Connection teardown occurs only when no owner remains, task-event subscriptions must be independently removable without affecting other subscribers, and task-sensitive controls remain disabled whenever current task state is unknown. This phase does not add a generic task activity UI or generic task-cancellation API; those can be introduced separately without changing repository ownership or its event model.
