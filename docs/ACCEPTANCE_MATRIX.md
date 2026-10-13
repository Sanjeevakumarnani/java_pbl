# CRFMS Acceptance Matrix

Source basis: `requirements/CRFMS_Master_Build_Prompt.md`, Part E. The latest delivery deliberately upgrades the original console/file-store brief to a Swing desktop app with SQLite. Tests that specifically require editable text files are therefore replaced by database persistence and integrity tests.

**Status definitions**
- **Implemented — runtime pending:** corresponding behavior exists in code, but the JDBC integration suite has not been executed in this build environment.
- **Partial:** a related workflow exists, but the exact acceptance condition is not fully implemented.
- **Not implemented:** no complete workflow or automated test evidence yet.
- **Replaced by SQLite:** original file-storage-only acceptance is not applicable to the chosen upgraded architecture.

| ID | Acceptance scenario | Status |
|---|---|---|
| TC-01 | Login for Admin, Faculty and Student roles | Implemented — runtime pending |
| TC-02 | Wrong password displays generic error | Implemented — runtime pending |
| TC-03 | Unknown ID displays same generic error as wrong password | Implemented — runtime pending |
| TC-04 | Inactive account cannot sign in | Implemented — runtime pending |
| TC-05 | Three failed attempts cause a five-minute lockout, including correct-password attempts during lock | Implemented — runtime pending |
| TC-06 | Weak password rejected; valid password accepted | Implemented — runtime pending |
| TC-07 | Admin creates student account | Implemented — runtime pending |
| TC-08 | Duplicate user ID/email rejected by application/database | Implemented — runtime pending |
| TC-09 | Invalid account fields rejected | Implemented — runtime pending |
| TC-10 | Admin creates course | Implemented — runtime pending |
| TC-11 | Duplicate course code rejected on create; edit uses a separate workflow | Implemented — runtime pending |
| TC-12 | Capacity cannot be lowered below enrolment | Implemented — runtime pending |
| TC-13 | Registration succeeds and seats are recalculated | Implemented — runtime pending |
| TC-14 | Closed registration window blocks register and drop | Implemented — runtime pending |
| TC-15 | Unknown/inactive course registration rejected | Implemented — runtime pending |
| TC-16 | Duplicate active registration rejected | Implemented — runtime pending |
| TC-17 | Registration at credit limit succeeds | Implemented — runtime pending |
| TC-18 | Registration exceeding credit limit rejected | Implemented — runtime pending |
| TC-19 | Full course handling | **Changed intentionally:** adds student to FIFO waitlist rather than refusing without recourse |
| TC-20 | Drop releases a seat and attempts waitlist promotion | Implemented — runtime pending |
| TC-21 | Re-registering a dropped course reactivates its registration record | Implemented — runtime pending |
| TC-22 | Course feedback submission succeeds | Implemented — runtime pending |
| TC-23 | Feedback for unregistered/dropped course rejected | Implemented — runtime pending |
| TC-24 | Closed simple-feedback window rejects submission | Implemented — runtime pending |
| TC-25 | Rating outside 1–5 rejected by service | Implemented — runtime pending |
| TC-26 | Resubmitting simple feedback updates one record | Implemented — runtime pending |
| TC-27 | Faculty sees only own course feedback, anonymously | Implemented — runtime pending |
| TC-28 | Report totals/averages and CSV output verified | Partial — CSV exists; JDBC execution and spreadsheet-open check pending |
| TC-29 | Data persists across application restart | Implemented by SQLite — runtime pending |
| TC-30 | Missing text file recreated and damaged lines skipped with `.bak` | Replaced by SQLite schema initialization and database backup |
| TC-31 | Invalid/junk UI input does not terminate the application | Partial — action errors are dialog-handled; full manual UI sweep pending |
| TC-32 | Pipe delimiters and line breaks sanitized in stored text | Implemented — runtime pending |
| TC-33 | Enrolment count recomputed from edited registrations text file | Replaced by SQL count derived from active registrations |
| TC-34 | Last active Admin cannot be deactivated | Implemented — runtime pending |
| TC-35 | Reworded doubt can match a stored FAQ | Implemented — runtime pending |
| TC-36 | Unrelated doubt creates an OPEN faculty ticket | Implemented — runtime pending |
| TC-37 | FAQ scoped to another course is ignored | Implemented — runtime pending |
| TC-38 | Doubt for unregistered course rejected | Implemented — runtime pending |
| TC-39 | Faculty answers only own-course doubts | Implemented — runtime pending |
| TC-40 | Adding FAQ allows later matching questions to be answered | Implemented — runtime pending |
| TC-41 | Student can mark a bot answer unhelpful and escalate it | Partial — escalation exists; dedicated aggregate count is not exposed |
| TC-42 | Anonymous doubt hides student ID from role-facing doubt views | Implemented — runtime pending |
| TC-43 | Open doubt older than three days is flagged | Implemented using local timestamps; fixed-clock test not implemented |
| TC-44 | Teaching feedback accepts ten category ratings | Implemented — runtime pending |
| TC-45 | Fewer than four ratings or a rating outside 1–5 rejected | Implemented — runtime pending |
| TC-46 | More than five tags rejected | Implemented — runtime pending |
| TC-47 | Duplicate teaching-feedback submission edits same record; dropped course rejected | Implemented — runtime pending |
| TC-48 | Closed feedback period rejects teaching feedback | Implemented — runtime pending |
| TC-49 | Blocked suggestion is held and not shown to faculty | Implemented — runtime pending |
| TC-50 | Below-threshold teaching dashboard reveals no averages/suggestions | Implemented — runtime pending |
| TC-51 | Teaching averages match hand calculations | Partial — calculation implemented; full numeric test coverage pending |
| TC-52 | Exact source prompt category ordering/severity for Examples, Voice and Board | **Partial:** configurable guidance and Examples-first ordering exist; source prompt's exact category/tag scoring engine is not fully implemented |
| TC-53 | High averages produce strengths; no low-category suggestions | Implemented — runtime pending |
| TC-54 | Other faculty cannot access another course's teaching insights | Implemented — runtime pending |
| TC-55 | Editing rule threshold changes guidance and creates audit event | Implemented — runtime pending |
| TC-56 | Improvement action status persists | Implemented — runtime pending |
| TC-57 | Compare two periods and show rating delta | Implemented — runtime pending; exact +0.8 scenario not yet automated |
| TC-58 | Catalogue rating hidden below configured response threshold and shown at/above it | Implemented — runtime pending |
| TC-59 | Notification created, unread count increments and returns to zero after read | Implemented — per-user unread count is shown in the notification workspace; runtime pending |
| TC-60 | FIFO waitlist promotes first eligible student after seat frees | Implemented — runtime pending |
| TC-61 | Ineligible first waitlist student is skipped with reason; next student is tried | Implemented — reason is persisted/displayed and the next FIFO candidate is attempted; runtime pending |
| TC-62 | Timetable clash rejected with conflicting course code | Implemented — runtime pending |
| TC-63 | Announcement visible only to registered students and notification generated | Implemented — runtime pending |
| TC-64 | Admin overview ranks courses, flags low ratings, exposes no student names | Implemented — runtime pending |

## Verification gate

`./build.sh`, `./test.sh`, four password-security checks and 14 SQLite schema/constraint checks pass in the current environment. The JDBC integration suite has been expanded but **has not run here** because the SQLite JDBC driver could not be downloaded. Do not treat any “runtime pending” case as passed until `DatabaseIntegrationTest` and the manual GUI acceptance run pass on a machine with the driver available.
