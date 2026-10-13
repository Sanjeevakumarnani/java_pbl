# FEATURE_MATRIX.md

## Summary
All core features implemented in code; JDBC/GUI runtime verification pending in some environments. Integration tests pass when JDBC deps available.

## Feature status
| Area | Status | Notes |
|---|---|---|
| Auth (login, roles, lockout, forced change, password policy) | Implemented | PBKDF2-HMAC-SHA256; lockout in memory; generic errors. |
| Admin user CRUD + reset + last-active guard | Implemented | create/update/set active/reset password; prevents deactivating last active admin. |
| Courses (create/edit/deactivate, faculty assign, capacity guard, schedule) | Implemented | validation, enrolled-count guard, search by keyword/faculty/semester. |
| Registration (window, credits, capacity, active, clash, drop, reactivation) | Implemented | transactional; exact clash rule; reactivates dropped record. |
| Waitlist (FIFO, promotion after drop/capacity change, reasons, next-eligible) | Implemented | reasons persisted; promotion revalidates constraints. |
| Simple feedback (one per student/course, resubmit, threshold, no identity) | Implemented | privacy suppression; window control. |
| Teaching feedback (10 cats, min 4 rated, tags, moderation, periods) | Implemented | HELD/OK/REJECTED moderation; period open/close; resubmission updates. |
| Suggestions/rules (engine structure, admin edit) | Implemented (core) | guidance logic present; exact 14-rule book + full engine classes per master prompt to be completed/refined. |
| Doubts/FAQ (bot, escalation, anonymous, faculty inbox) | Implemented | keyword matching from stored FAQs; anonymity enforced in views. |
| Announcements/resources (scoping to registered) | Implemented | notifications on announcements; student access scoped. |
| Notifications (per-user, unread count, mark read) | Implemented | count accurate; mark-read updates state. |
| Reports/CSV (courses/registrations/feedback; privacy) | Implemented | escaping/sanitization; sub-threshold suppressed. |
| Quality overview (ranking, alert, counts, no names) | Implemented | ranks by average; flags low ratings. |
| Backup/restore support (DB backup) | Implemented | checkpoint + copy to backups/ with timestamp. |
| Demo seeding (--demo) | Implemented (basic) | minimal seed; expansion pending to full spec. |
| Audit logging | Implemented | key actions logged. |