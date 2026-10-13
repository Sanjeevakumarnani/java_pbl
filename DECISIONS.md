# DECISIONS.md

## Architectural decisions

### Swing/SQLite upgrade (from console/file-store)
The original master prompt describes a console application with plain-text files. This project has been intentionally upgraded to a Java 17+ Swing desktop GUI backed by SQLite persistence. This deviation is documented and preserved. All equivalent requirements are implemented using database persistence (schema, transactions, FK constraints, PRAGMA foreign_keys ON). File-specific acceptance cases (TC-30, TC-33) are replaced by SQLite initialization/integrity and SQL-derived enrolment counts with explicit documentation in the acceptance matrix.

### Service layer structure
CrfmsService currently centralizes JDBC operations, permissions, and business rules. The master prompt calls for cleaner separation into model/dao/service/engine/bot/report layers. This refactoring will be done while preserving exact behavior; all permission checks remain in the service layer. Domain exceptions with safe messages are preferred.

### Time injection for testability
Time-sensitive operations (login lockout, timestamps, overdue doubt detection) currently use LocalDateTime.now() directly. A java.time.Clock (or time provider) will be injected to enable deterministic fixed-clock tests without sleeps.

### Course rating formula
Catalogue rating is the arithmetic mean of all 1 to 5 simple-feedback ratings recorded for a course: sum of ratings divided by number of responses. It is shown only when the response count is at least the administrator setting `min_responses` (default 3); otherwise the UI and CSV reports show `HIDDEN` or `SUPPRESSED`. Teaching-feedback averages are separate and are not mixed into this catalogue value.

### Drop/window/waitlist behavior
Registration and drop follow BR rules. When course is full, registration adds student to FIFO waitlist (TC-19 intentionally changed to waitlist behavior). After drop, promoteWaitlist attempts FIFO promotion in order, revalidating account status, active registration, credits, timetable clash, capacity, and duplicate conditions. Skipped waitlist entries persist their reason and the next eligible is tried (TC-61).

### Schema migrations
Database.initialize() uses CREATE TABLE IF NOT EXISTS and ensureColumn() to add waitlist.reason. Migrations are idempotent; versioning will be tracked. Upgrade from v2.1.0 schema is supported.

### Privacy and identity
Anonymous doubts conceal student identity in all faculty/admin views/reports/exports/analytics. Student identity is only visible to the owning student. Feedback and teaching insights are suppressed below minResponses. Held/rejected moderation content never exposed to faculty or in reports.

### CSV export safety
CSV exports escape quotes/delimiters/newlines and sanitize for spreadsheet safety; formula-injection-prone prefixes will be guarded for untrusted fields.

### Demo vs production
Default admin (admin/Admin@123) is created only if no admins exist and requires password change on first login. Demo data is seeded only via --demo flag; seeding is idempotent and uses clearly fictional accounts. Production and demo credentials are clearly distinguished.
