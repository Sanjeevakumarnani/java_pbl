# End-to-End Project Plan

## Completed in this delivery
- [x] Define Java 17+ desktop app structure and SQLite schema.
- [x] Implement login, role-based workspace tabs, forced password change and lockout.
- [x] Implement admin account creation/edit/status/password reset.
- [x] Implement course management and course search.
- [x] Implement registration/drop rules, credit cap, schedule clash and capacity checks.
- [x] Implement FIFO waitlist and automatic promotion attempt after drop.
- [x] Implement anonymous simple feedback and minimum response suppression.
- [x] Implement feedback periods and ten-category teaching feedback.
- [x] Implement FAQ matching and doubt escalation/answering.
- [x] Implement announcements, resources and notifications.
- [x] Implement held-suggestion moderation and improvement action plans.
- [x] Implement timestamped SQLite backup and CSV exports.
- [x] Implement settings, CSV exports with feedback privacy suppression, and ranked quality overview.
- [x] Add admin-editable suggestion rules, strengths callouts, course-rating threshold display, period-to-period comparison, and stale open-doubt flagging.
- [x] Add offline SQLite schema/constraint validator and align FAQ default matching score with the documented one-word FAQ behavior.
- [x] Persist waitlist eligibility reasons and show them in the waitlist view.
- [x] Add per-user unread notification count, course-rating privacy display, simple-feedback window control, duplicate course-create rejection, and teaching-feedback resubmission updates.
- [x] Add a TC-01…TC-64 acceptance traceability matrix with honest partial/missing status.
- [x] Add run/build/test scripts, schema SQL, README, feature matrix and test report.
- [x] Compile source with Java 17 target and `-Xlint:all`.
- [x] Run offline password-security tests.
- [x] Validate schema DDL through SQLite syntax smoke test.

## Must complete on a computer with network access
1. [ ] Run `run.bat` or `./run.sh` on a network-enabled machine to download SQLite JDBC and SLF4J dependencies.
2. [ ] Confirm first-launch database creation and forced admin password change.
3. [ ] Run `test.bat` or `./test.sh` and confirm `DatabaseIntegrationTest` passes.
4. [ ] Use `--demo` and test demo logins, changing each initial password.
5. [ ] Verify register/drop/full-course/waitlist promotion and timetable clash scenarios.
6. [ ] Verify feedback and teaching-feedback privacy thresholds with enough accounts.
7. [ ] Verify announcements/resources are hidden from unregistered students.
8. [ ] Export CSVs and open them in Excel/LibreOffice.
9. [ ] Back up and restore `data/crfms.db`.
10. [ ] Run the 64 acceptance tests in the source PRD and record pass/fail evidence.

## Suggested final QA scenarios
- [ ] Three failed logins lock an ID for five minutes; correct password is refused during the lock.
- [ ] Student cannot register while registration is closed.
- [ ] Student cannot exceed maximum credits or register into a time clash.
- [ ] Course capacity cannot be reduced below current enrolment.
- [ ] Duplicate registration is rejected; a dropped registration can be reactivated.
- [ ] Full course adds student to FIFO waitlist; drop promotes the first eligible student.
- [ ] Faculty cannot access another faculty member's course roster, doubts or feedback.
- [ ] Feedback remains hidden below the configured threshold and becomes visible above it.
- [ ] Anonymous doubt never reveals the student ID to faculty/admin.
- [ ] Teaching feedback requires at least four categories and an open period.
- [ ] Blocked-word suggestion is held and not shown to faculty.
- [ ] Students see announcements/resources only for registered courses.
