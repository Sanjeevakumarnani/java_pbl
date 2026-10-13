# CRFMS Desktop Demo Script (8–10 minutes)

## Preparation
1. Start with `run.bat --demo` or `./run.sh --demo`.
2. Sign in as `admin` / `Admin@123`; set a new password.
3. Sign out and sign in as `fac101` / `Welcome123`; set a new password.
4. Sign out and sign in as `stu1001` / `Welcome123`; set a new password.

## Demonstration sequence
1. **Admin overview:** show user/course/registration/feedback/doubt counts and export a CSV.
2. **Admin users:** add a faculty/student, edit a profile, reset a password, and show the forced-change behaviour.
3. **Admin courses:** create a course with credits, capacity, faculty and a timetable slot.
4. **Registration rules:** register the student; attempt a second overlapping course to show the clash rejection. Set capacity to one, fill it, and show the waitlist message.
5. **Drop and promotion:** sign in as the registered student and drop the course; show the FIFO waitlist promotion and notification for the next eligible student.
6. **Simple feedback privacy:** submit ratings from at least three demo/test student accounts; show suppression below the threshold and average/comments after it is met.
7. **FAQ bot:** ask a question covered by a stored FAQ. Then ask an unrelated question to show escalation to faculty. Mark a bot answer unhelpful to escalate it.
8. **Faculty workflow:** open own-course roster, answer an escalated doubt, post a course announcement, and add a resource link.
9. **Teaching feedback:** Admin creates a period. Students submit ten-category feedback with at least four categories rated. Show the privacy suppression threshold.
10. **Quality improvement:** show the rule-based guidance, approve/reject a held suggestion as Admin, create an improvement action and update its status.
11. **Persistence:** close and reopen the app, then confirm users, courses and registrations remain stored in `data/crfms.db`.

Explain honestly: the FAQ assistant uses keyword matching over approved stored entries; teaching improvement advice comes from explicit rules. Neither is a generative AI model.
