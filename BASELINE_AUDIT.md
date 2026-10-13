# BASELINE_AUDIT.md

Date: 2026-10-11

## Project structure
- src/main/java/crfms: CrfmsService.java (367), Database.java, Security.java, Main.java, LoginFrame.java, MainFrame.java (111)
- src/test/java/crfms: SecurityTest.java (16), DatabaseIntegrationTest.java (75)
- target/: compiled classes present
- docs/: ARCHITECTURE.md, ACCEPTANCE_MATRIX.md, FEATURE_MATRIX.md, TEST_REPORT.md, PROJECT_PLAN.md, DEMO_SCRIPT.md, schema.sql, UML.puml
- requirements/: CRFMS_Master_Build_Prompt.md, CRFMS_PRD_Course_Registration_Feedback_System_v1.1.docx
- tools/: validate_schema.py
- Scripts: build.bat/sh, run.bat/sh, test.bat/sh

## Build/tests (offline)
- javac --release 17 -Xlint:all: compiles clean (no warnings)
- SecurityTest: 4 assertions passed (PBKDF2 with salt, verify, random salt, plaintext not stored)
- tools/validate_schema.py: 14 checks passed (16 tables, Java/schema parity, defaults, uniqueness, range, FK)
- DatabaseIntegrationTest: requires SQLite JDBC + SLF4J jars (skipped by test.sh if missing)

## Key implementation status (from docs)
- Core: login/auth, roles, courses, registration, waitlist, feedback (simple), teaching feedback, doubts/FAQ, announcements/resources, notifications, moderation, improvement actions, suggestion rules, quality overview, backup, CSV exports - implemented in code; runtime verification pending.
- Architecture: centralized in CrfmsService (JDBC queries + business logic). UI is Swing (LoginFrame, MainFrame). Needs refactor to proper layers per master prompt.
- SeedDemo: minimal (fac101, stu1001, 2 courses, 2 FAQs, 1 feedback period). Needs expansion per spec.
- Test behavior: test.sh skips DatabaseIntegrationTest silently if deps missing; must fail or label clearly.
- Time: direct LocalDateTime.now() usage (lockout, timestamps, overdue). Needs Clock injection.

## Gap highlights vs master prompt
1. SuggestionEngine: exact 14 rules, ThresholdRule severity=limit-avg, TagRule severity=share, sort by severity desc, deterministic ties, reference test case not enforced/implemented in engine classes as specified.
2. TextTokenizer: exact stop words set (current has extras). Must match master prompt exactly.
3. Teaching feedback categories: canonical 10-category model and ordering from master prompt; engine/rulebook alignment.
4. Expanded demo dataset (3 faculty, 10 students, 8 courses incl capacity-2 + inactive + clash pair, 20+ FAQs, full 14 rules, MID-TERM period with 5-8 records producing reference scenario).
5. Clock injection, CSV injection safety, audit log viewer (last 50 for admin), department benchmark (>=3 faculty), explicit course rating formula in DECISIONS.md, backup/restore validation robustness.
6. Test scripts must not silently skip JDBC tests.
7. Complete acceptance test execution/recording (TC-01-64) with evidence.

## Recommendation
Proceed with refactoring (Clock + layers), fix test scripts, implement exact engine/tokenizer/14 rules, expand seedDemo, add missing tests for reference case.
