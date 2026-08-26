# 01 — Keep Library administration reachable with an empty Catalog

**What to build:** Admin/root users can still reach Library administration and create a Library
when the current Audiobookshelf server has no Libraries, including immediately after deleting the
final Library.

**Blocked by:** None — can start immediately.

**Status:** ready-for-agent

- [ ] The empty-Catalog Home experience retains an accessible path to Library administration for
      admin/root users.
- [ ] Non-admin users do not gain access to Library administration.
- [ ] Deleting the final Library leaves the administration and create flows reachable without an
      invalid pager state or navigation loop.
- [ ] Functional tests cover an initially empty Catalog and deletion of the final Library.
