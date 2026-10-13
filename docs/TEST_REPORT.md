# CRFMS Desktop Test Report

Date: 2026-10-11

## Executed in this environment

| Check | Result | Evidence |
|---|---|---|
| Java compilation (`--release 17 -Xlint:all`) | PASS | All main and test Java sources compiled with no warnings after corrections |
| PBKDF2 password security tests | PASS | `SecurityTest`: 4 assertions passed |
| SQLite schema syntax and constraints | PASS | `tools/validate_schema.py`: 14 checks; validates 16 tables, parity with Java initializer, defaults, unique email, credit range, registration uniqueness and foreign keys |
| Core SQL smoke test | PASS | Python SQLite executed representative user/course/registration/feedback inserts and aggregate queries |
| SQLite JDBC integration tests | NOT RUN | Runtime driver download failed because outbound DNS/network access is unavailable in this environment |
| Swing GUI launch | NOT RUN | Requires SQLite JDBC driver at runtime |
| Full PRD TC-01…TC-64 suite | NOT VERIFIED | Detailed QA matrix is supplied in `ACCEPTANCE_MATRIX.md`; run against a real local database before claiming final acceptance |

## Integration test included

`src/test/java/crfms/DatabaseIntegrationTest.java` contains an expanded set of integration checks for default admin setup, forced password change, account creation, course creation, registration, full-course waitlist, waitlist promotion, timetable clash rejection, feedback privacy thresholds, stored FAQ matching, anonymous doubt visibility, registered-course announcements, teaching-feedback privacy, persistence, and failed-login lockout.

Run it using `./test.sh` or `test.bat` after the JDBC and SLF4J jars are present in `lib/`. It is skipped automatically when those runtime dependencies are absent.

## Acceptance traceability

See `ACCEPTANCE_MATRIX.md` for TC-01…TC-64 status. It distinguishes implemented code from runtime-verified behavior and explicitly lists partial/missing acceptance conditions.

## Known verification boundary

Compilation proves Java source correctness at compile time, not successful JDBC runtime behavior. The SQL schema and representative statements have been syntax-checked by SQLite, but all JDBC prepared statements, role workflows and GUI paths still require integration execution. Do not describe the app as production-verified until those tests and manual acceptance scenarios pass.
