# CRFMS Desktop — Course Registration & Feedback Management System

**Version 2.1.0 | Java 17+ | Swing desktop GUI | SQLite database**

This release upgrades the earlier console/file-storage prototype to a desktop GUI backed by a real relational database. It preserves the core CRFMS domain rules while adding advanced feedback, FAQ doubt matching, waitlists, announcements, resources, and notification workflows.

## Quick start

### Windows
1. Install a JDK 17 or newer (not only a JRE). Verify `java -version` and `javac -version` in Command Prompt.
2. Extract the ZIP into a normal writable folder.
3. Double-click `run.bat` or open Command Prompt in the extracted folder and run `run.bat`.
4. The first launch downloads SQLite JDBC and its small logging dependencies from Maven Central. Internet access is required for this one-time setup.
5. Sign in with `admin` / `Admin@123`. The application requires a new password on first login.

### Linux / macOS
```bash
chmod +x run.sh build.sh test.sh
./run.sh
```

The first launch downloads the SQLite JDBC driver and SLF4J runtime jars. Run `./run.sh --demo` to seed fictional demonstration accounts, courses, FAQ entries, and a feedback period.

### Demo accounts
`./run.sh --demo` (or `run.bat --demo`) seeds these fictional accounts if absent:

| Role | User ID | Initial password |
|---|---|---|
| Admin | `admin` | `Admin@123` |
| Faculty | `fac101` | `Welcome123` |
| Student | `stu1001` | `Welcome123` |

All seeded accounts require a password change on first sign-in. Demo identities and data are fictional; do not use them as institutional records.

## What is implemented

### Authentication and user management
- Admin, faculty, and student roles with role-specific workspaces.
- PBKDF2-HMAC-SHA256 password hashes with random 16-byte salts and 120,000 iterations.
- Forced password change on first sign-in; change-password flow verifies the current password.
- In-memory lockout after three failed login attempts for five minutes.
- Admin can create/edit/deactivate users and reset passwords; reset password is shown once.
- Last active administrator cannot be deactivated.

### Courses and registration
- Course create/edit/deactivate; faculty assignment to active faculty accounts.
- Search by course code/title/faculty name; catalogue includes capacity and available seats.
- Register/drop; one active registration per student/course; dropped records are reactivated.
- Registration window, credit limit, course capacity, active account/course, and timetable clash checks.
- FIFO waitlist when a course is full; after a drop the system attempts promotion while rechecking account, credits, schedule clash, and duplicate-registration conditions.
- Registration views are scoped to the signed-in student or faculty member.

### Feedback and teaching quality
- One simple course feedback entry per student/course; resubmission edits it. Admin can open/close the simple-feedback window independently from course registration.
- Rating validation (1–5), optional comment, anonymous aggregation.
- Faculty feedback is hidden until the configured minimum response count is met.
- Feedback periods can be created and opened/closed by the administrator.
- Ten-category teaching feedback with optional skipped ratings, tags, and a suggestion.
- Minimum four rated categories; up to five tags; suggestions are moderated against a small configurable-in-code blocked-word list.
- Teaching results and suggestions are withheld until each period's minimum response threshold is reached.
- Explainable, rule-based teaching improvement guidance; admin-editable thresholds and advice, positive-strength callouts, and before/after period comparisons. Guidance is not an automated ranking or disciplinary decision.
- Admin teaching-quality overview, course averages, response counts, and open-doubt counts.
- Held-suggestion moderation (approve/reject without exposing student identity).
- Faculty/admin improvement action plans with open, in-progress, completed, and cancelled statuses.

### Doubts and learning support
- Students can ask course-related doubts and choose anonymity.
- Keyword-matching FAQ assistant answers only from stored FAQ entries scoped to a course or `ALL`.
- Unmatched questions are escalated to the assigned faculty as open tickets.
- Faculty can answer tickets for their own courses; students receive notifications.
- Admin can manage FAQ entries.

### Communication and reporting
- Faculty/admin announcements; only currently registered students see course announcements.
- Faculty/admin resource links; students see resources for courses they are registered in.
- In-app notifications and mark-as-read workflow.
- CSV export for courses, registrations, and feedback summaries; feedback averages/counts are suppressed below the configured minimum response threshold.
- Database backup action creates timestamped snapshots under `backups/`.
- Local SQLite database stored at `data/crfms.db`; the schema is also included in `docs/schema.sql`.
- Audit events for key actions, and safe parameterized SQL for user input.

## Database and privacy

- SQLite is local and embedded; no server or network database is needed.
- Foreign keys are enabled for each connection.
- Passwords are never stored as plain text.
- Simple feedback reports omit student identity and suppress results below the configured minimum response count.
- Anonymous doubts hide the student's ID in doubt lists. The system stores the ID internally so the student can view their own ticket.
- User-entered text is trimmed and line breaks/pipe delimiters are sanitised where appropriate.

The database file is persistent. Back up `data/crfms.db` before manually editing or deleting data. Do not share the database file publicly because it contains student account records and feedback.

## Tests and build

```bash
./build.sh
./test.sh
```

Windows equivalents: `build.bat` and `test.bat`.

- The project compiles with `javac --release 17 -Xlint:all` without needing the JDBC jar at compile time.
- `SecurityTest` is a no-dependency test and runs offline.
- `DatabaseIntegrationTest` contains extended integration checks for role isolation, privacy thresholds, waitlist promotion, rule-driven guidance, strengths, and suggestion moderation and runs automatically when the SQLite JDBC and SLF4J jars exist in `lib/`. It checks forced password changes, account creation, registration rules, waitlist promotion, timetable clash rejection, feedback privacy, FAQ matching, anonymous doubts, announcements, teaching-feedback suppression, persistence, and login lockout.
- `run.sh`/`run.bat` fetch the runtime JDBC jars on first use. If network access is unavailable, install those jars separately into `lib/` before launching.

## Project structure

```text
CRFMS_Desktop/
  src/main/java/crfms/      Swing UI, SQLite access, services, password security
  src/test/java/crfms/      Offline security test and database integration test
  docs/schema.sql           Relational schema and default settings
  docs/FEATURE_MATRIX.md    Feature-to-acceptance overview
  docs/ACCEPTANCE_MATRIX.md Source PRD TC-01…TC-64 traceability and gaps
  docs/PROJECT_PLAN.md      Implementation plan and remaining verification
  docs/TEST_REPORT.md       Actual local verification results and limitations
  run.sh / run.bat          Download dependencies if needed, compile, launch GUI
  build.sh / build.bat      Offline Java compilation
  test.sh / test.bat        Test runners
  pom.xml                   Optional Maven project definition
  data/                     Created at runtime; contains crfms.db
  exports/                  Created at runtime; CSV exports
```

## Honest status and limitations

The Java source compiles and the schema passed a local SQLite syntax smoke test using Python's SQLite engine. The four offline password-security checks and 14 offline SQLite schema/constraint checks pass. The SQLite JDBC driver could not be downloaded in the generation environment because outbound DNS/network access is disabled, so the GUI/database integration test was not executed here. It is included for execution after the driver is available.

This is a substantial working implementation, but it is not a claim that all 64 PRD acceptance cases have been independently verified. The source-aligned acceptance matrix is in `docs/ACCEPTANCE_MATRIX.md`. It identifies intentional architecture changes and remaining partial cases, including waitlist skip reasons and the exact source-prompt category/tag scoring engine. Before institutional use, run the JDBC integration suite and manually verify all role workflows, privacy thresholds, waitlist behavior, comparison reports and CSV exports.
#   j a v a _ p b l  
 