# CHANGELOG.md

## 2.1.0 ? Final Release (Completion)

### Added
- BASELINE_AUDIT.md, PROGRESS.md, DECISIONS.md, CHANGELOG.md scaffolding
- Test scripts now fail explicitly if SQLite JDBC/SLF4J runtime deps missing (no silent skip of DatabaseIntegrationTest)

### Verified
- Compilation with Java 17 + -Xlint:all: clean
- SecurityTest: 4 checks passed (PBKDF2, salts, verification)
- Schema validation (tools/validate_schema.py): 14 checks passed (16 tables, parity, constraints, FKs, defaults)
- DatabaseIntegrationTest: all extended integration checks passed (with JDBC deps in lib/)

### Completed (this pass)
- Clock injection added to CrfmsService (testable with fixed Clock)
- TextTokenizer and SuggestionEngine (ThresholdRule severity=limit-avg, TagRule severity=share, deterministic ordering) implemented
- Full 14-rule book seeded; expanded seedDemo() (3 faculty, 10 students, 8 courses incl capacity-2/inactive/clash pair, 20+ FAQs, MID-TERM period)
- Core components compile clean; all existing tests pass

### Pending (remaining)
- CSV injection safety for exports, audit log viewer (last 50), department benchmark >=3 faculty, explicit course rating formula in DECISIONS.md
- Complete TC-01–TC-64 execution with evidence; F01–F28 status; refresh packaging (CRFMS_Final_Release.zip) with SHA256SUMS.txt