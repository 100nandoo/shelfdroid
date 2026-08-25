# 01 — Make shared socket ownership safe

**What to build:** Existing and future real-time consumers can subscribe independently, navigate
away safely, reconnect, and coexist without replacing listeners or disconnecting the shared socket.

**Blocked by:** None — can start immediately.

**Status:** complete

- [x] Multiple consumers can independently subscribe to the same or different socket events.
- [x] Removing one subscription does not remove another consumer's listener.
- [x] Releasing one consumer does not disconnect the socket while another owner still needs it.
- [x] Reconnection restores active subscriptions without duplicate event delivery.
- [x] Existing podcast real-time behaviour remains functional through navigation and reconnection.
- [x] Automated tests cover independent subscription, removal, ownership, and reconnection.
