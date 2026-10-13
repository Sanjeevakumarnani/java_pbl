# Architecture Overview

```text
Swing UI (LoginFrame, MainFrame)
          |
          v
CrfmsService — authentication, permissions, validation, registration rules,
               feedback privacy, FAQ matching, waitlist, notifications, reports
          |
          v
Database — JDBC connections, schema initialization, foreign keys, backups
          |
          v
SQLite file: data/crfms.db
```

## Core tables

- `users`: roles, profile fields, active status, salt/hash, forced-password-change flag.
- `courses`: credit value, capacity, semester, assigned faculty, optional schedule slot.
- `registrations`: one student/course row with `REGISTERED` or `DROPPED` status.
- `feedback`: one simple rating/comment per student/course; reports omit identity.
- `feedback_periods`, `teaching_feedback`: ten-category feedback and privacy threshold.
- `faq_entries`, `doubts`: approved answers, keyword matching and faculty escalation.
- `waitlist`, `notifications`: FIFO queue and in-app notices.
- `announcements`, `resources`: course communication and material links.
- `suggestion_rules`, `improvement_actions`: explainable guidance and action tracking.
- `settings`, `audit_log`: configurable business thresholds and traceable admin actions.

## Important implementation note

This desktop upgrade centralizes JDBC operations in `CrfmsService` for a compact, understandable student project. It enforces service-level permission checks, but it does not yet split every entity into a separate DAO interface as the original console-only master prompt described. The GUI/SQLite architecture is the deliberate choice for the newer desktop-app request.
