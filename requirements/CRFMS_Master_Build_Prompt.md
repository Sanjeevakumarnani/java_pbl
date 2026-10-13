# MASTER PROMPT — Build the complete "Course Registration & Feedback Management System" (CRFMS) in Java

> Paste everything below the line into a coding assistant (for example Claude Code) opened in an **empty folder**. The prompt is self-contained: it repeats every rule, format and feature, so the assistant does not need the PRD document. If your assistant has a size limit, paste PART A to PART F first (in two or three pieces if needed), then send the short "PHASE PROMPTS" from PART G one at a time.

---

# PART A — ROLE, GOAL AND GROUND RULES

## A1. Your role
You are a senior Java engineer and patient teacher. You will build, test and document a **complete, working, console-based Java application** called **Course Registration & Feedback Management System (CRFMS)**. It is the project for an *Object-Oriented Programming through Java* project-based-learning (PBL) course, built by a **diploma student who is new to Java**. The code must therefore be **correct, clean, heavily commented, and easy to explain in an oral exam (viva)**.

## A2. What the system is (and is not)
- It is an **administrative registration and feedback system** for a college. Students choose courses, drop courses, ask doubts and rate teaching. Faculty see enrolments, answer doubts and read feedback with improvement suggestions. Admin sets everything up.
- It is **not** an online-learning platform: it does not host lessons, videos or live classes.
- It is a **single-user console program** (text menus) that stores data in **plain text files**. No web server, no GUI, no network, no database in the main build.
- The "Doubt Bot" is a **keyword-matching FAQ assistant, not artificial intelligence**. The "improvement suggestions" are produced by **if-then rules**. Never add or claim AI/machine-learning behaviour, and never call an external service.

## A3. Hard constraints
1. **Java 17 or newer** (the reference code was verified on JDK 21). Use only the **Java standard library**. No Maven/Gradle dependencies are required to build or run the app. Provide plain `javac`/`java` scripts.
2. Tests must run **without any external library**: write a small plain-Java test runner (`TestRunner` with an `assertTrue/assertEquals/assertThrows`-style helper). You may *also* add JUnit 5 tests in a separate folder marked optional, but the plain runner is the one that must pass.
3. All text files are **UTF-8**. Data files use the **pipe `|` delimiter** with a header line. Dates in files are ISO (`2026-10-05`); dates on screen are `dd-MMM-yyyy` (for example `05-Oct-2026`).
4. **Single-threaded.** No multithreading.
5. **Beginner-friendly style:** prefer simple loops and `if` statements over advanced streams/lambdas (a few simple ones are fine, such as sorting with a `Comparator`). No reflection, no annotations other than `@Override`, no records/sealed classes/pattern matching, no external frameworks. Every public class and method has Javadoc. Every non-obvious line has a short comment explaining *why*.
6. **Honesty rules:** never invent test results, never say something works unless you compiled and ran it, never leave `TODO` stubs in delivered code, never claim a feature is complete when it is not. If something cannot be completed, say so plainly in the final report.
7. **No fabricated real-world data.** All sample/demo data must be clearly invented (names like "Demo Student 1", e-mails at `example.com`).

## A4. Required quality attributes
- Invalid input **never crashes** the program (letters at a number prompt, empty lines, huge numbers, the `|` character, very long text).
- Business rules live in the **service layer only**; the UI layer never contains rules and never touches files; DAO classes never contain rules; services never print or read the keyboard.
- Every operation that changes data is saved **immediately** (write-through) using a **safe save** (write to `name.tmp`, then move over the real file; use atomic move when available).
- Passwords are stored only as **salted PBKDF2 hashes** and are never printed or logged.
- Naming: classes `PascalCase`, methods/variables `camelCase`, constants `UPPER_SNAKE_CASE`, packages lower-case.

## A5. How you must work
- Work in **phases** (PART E). After every phase: **compile with `javac -Xlint:all`**, **run the tests for that phase**, fix every error, then print a short status. Do not start the next phase while the current one fails.
- Keep a file `PROGRESS.md` listing each phase, what was built, what was tested, and anything incomplete.
- When a decision is not specified here, choose the simplest option consistent with this prompt and record it in `DECISIONS.md` (one line each).
- At the end produce the **final report** described in PART F.

---

# PART B — ARCHITECTURE, PACKAGES AND CORE DESIGN

## B1. Four layers (each talks only to the layer below)
1. **Presentation (`ui`)** – menus, `Scanner` input through a `ConsoleHelper`, printing results/messages. No business rules, no file access.
2. **Service (`service`, plus `bot` and `engine` helpers)** – all business rules, permission checks, orchestration of DAOs, throws custom exceptions. No `System.out`, no `Scanner`.
3. **Data access (`dao` interfaces, `dao.file` implementations)** – read/write files, convert lines ↔ objects. The service layer depends on the **interfaces only**.
4. **Storage** – folders `data/`, `logs/`, `exports/`, `backups/` created automatically if missing.
Shared by all: `model`, `exception`, `util`, `config`.

**Dependency injection:** services receive their DAOs (and a `java.time.Clock`) through the constructor so tests can pass in-memory fake DAOs and a fixed clock. A single `AppContext` class (in `config` or `ui`) builds all real objects once in `Main`.

## B2. Project tree (create exactly this; add classes as needed inside these packages)
```
CRFMS/
  src/
    config/    AppConfig.java (Singleton), AppContext.java
    model/     User(abstract) Student Faculty Admin Role(enum)
               Course Registration RegistrationStatus(enum) Feedback
               FeedbackCategory(enum) TeachingFeedback FeedbackPeriod ModerationStatus(enum)
               FaqEntry Doubt DoubtStatus(enum) ImprovementAction ActionStatus(enum)
               Notification Announcement ResourceItem WaitlistEntry
    exception/ AppException + subclasses (see B5)
    dao/       UserDAO CourseDAO RegistrationDAO FeedbackDAO TeachingFeedbackDAO PeriodDAO
               FaqDAO DoubtDAO SuggestionRuleDAO ActionDAO NotificationDAO
               AnnouncementDAO ResourceDAO WaitlistDAO
    dao/file/  File*DAO implementations of each interface, plus FileStore helper
    service/   AuthService UserService CourseService RegistrationService FeedbackService
               ReportService SettingsService DoubtService TeachingFeedbackService
               FacultyInsightService NotificationService WaitlistService TimetableService
               RecommendationService AnnouncementService ResourceService QualityOverviewService
    bot/       DoubtBot BotAnswer TextTokenizer
    engine/    SuggestionRule(interface) ThresholdRule TagRule SuggestionEngine
               CategoryStats Suggestion StatsCalculator CommentAnalyzer
    report/    Report(interface) EnrolmentReport FeedbackSummaryReport FacultyRatingReport
               LowRatedCoursesReport StudentSummaryReport
    util/      PasswordUtil FileUtil InputValidator AuditLogger UserFactory
               DateUtil IdGenerator TextSanitizer
    ui/        Main LoginMenu StudentMenu FacultyMenu AdminMenu ConsoleHelper
               DoubtMenu TeachingFeedbackMenu FacultyDashboardMenu InboxMenu TimetableMenu
    demo/      SampleDataLoader
  test/        TestRunner.java and test classes mirroring src packages
  data/ logs/ exports/ backups/        (created at run time; do not commit real data)
  build.sh build.bat run.sh run.bat test.sh test.bat
  README.md PROGRESS.md DECISIONS.md CONCEPT_MAP.md TEST_REPORT.md UML.puml
```
Every `.java` file starts with its `package` line. `ui.Main` is the entry point (`java -cp out ui.Main`). Support the command-line flag `--demo` (loads demo data if `data/` is empty) and `--reset-demo` (deletes `data/` contents after asking Y/N).

## B3. The user class hierarchy (inheritance, abstraction, polymorphism)
```java
public abstract class User {
    private final String userId;      // never changes
    private String name;
    private String email;
    private String salt;              // Base64
    private String passwordHash;      // Base64
    private boolean active = true;
    private boolean mustChangePassword;
    protected User(String userId, String name, String email) { ... }
    // getters; validated setters (name, email); setCredentials(salt, hash); verifyPassword(String)
    public abstract Role getRole();
    public abstract void showMenu();  // prints that role's menu text (polymorphism demo)
    @Override public String toString() { return getRole() + " " + userId + " - " + name; }
}
```
- `Student extends User`: `rollNumber` (final), `department`, `semester` (1–12).
- `Faculty extends User`: `employeeId` (final), `department`, `designation`.
- `Admin extends User`: no extra fields.
- `Role` enum: `ADMIN, FACULTY, STUDENT`. `UserFactory.create(role, ...)` builds the right subclass (Factory pattern).
- `showMenu()` text for Student: `1. Browse / search courses  2. Register  3. Drop  4. My registrations  5. Feedback ...` (the full real menus are in PART D).

## B4. Domain classes (private fields, getters, validated setters, `toString`)
- **Course**: `courseCode` (final), `title`, `credits` (1–6), `capacity` (1–200), `semester` (1–12), `facultyId`, `dayOfWeek` (`java.time.DayOfWeek`, nullable), `startTime`/`endTime` (`LocalTime`, nullable), `active`, `enrolledCount` (**not stored in file; recomputed at start-up from registrations**). Methods: `seatsLeft()`, `hasSeat()`, `addStudent() throws CourseFullException`, `removeStudent()`, `clashesWith(Course)` (same day and times overlap: `startA < endB && startB < endA`; courses with no slot never clash), `equals/hashCode` **by course code**.
- **Registration**: `registrationId` (`REG-000001`…), `studentId`, `courseCode`, `registeredOn`, `status` (`RegistrationStatus.REGISTERED|DROPPED`), `droppedOn` (nullable). Methods: `drop(LocalDate)`, `reactivate(LocalDate)`, `isActive()`.
- **Feedback** (simple end-of-term course rating): `feedbackId` (`FB-000001`…), `studentId`, `courseCode`, four ratings (content, teaching, materials, assessment, each 1–5), `comment` (≤300), `submittedOn`. `overallRating()` = average of the four. One per student per course.
- **FeedbackCategory** enum (with display label): `EXPLANATION("Explanation")`, `EXAMPLES("Use of examples")`, `BOARD_USAGE("Use of board")`, `DOUBT_GUIDANCE("Individual doubt guidance")`, `CONCEPT_CLARITY("Concept clarity")`, `VOICE_CLARITY("Voice clarity")`, `PACE("Pace of teaching")`, `INTERACTION("Interaction")`, `PUNCTUALITY("Punctuality")`, `MATERIALS("Learning materials")`.
- **TeachingFeedback**: `tfId` (`TF-000001`…), `studentId`, `courseCode`, `periodLabel`, `ratings` (`EnumMap<FeedbackCategory,Integer>`; missing = skipped), `tags` (`Set<String>`, ≤5), `suggestion` (≤300), `moderation` (`OK|HELD|REJECTED`), `submittedOn`.
- **FeedbackPeriod**: `label` (for example `WEEK-3`, `MID-TERM`, `END-TERM`), `open`.
- **FaqEntry**: `faqId` (`FAQ-000001`…), `courseCode` (or `ALL`), `question`, `answer`, `keywords`, `helpfulVotes`, `unhelpfulVotes`, `active`, plus a derived `Set<String> tokens`.
- **Doubt**: `doubtId` (`D-000001`…), `studentId`, `courseCode`, `question` (5–500), `anonymous`, `status` (`BOT_ANSWERED|OPEN|ANSWERED|CLOSED`), `answer`, `answeredBy`, `askedOn`, `answeredOn`.
- **ImprovementAction**: `actionId` (`ACT-000001`…), `facultyId`, `courseCode`, `category` (FeedbackCategory name or `TAG`), `baseAverage` (double), `suggestion`, `status` (`PLANNED|IN_PROGRESS|DONE|DISMISSED`), `createdOn`, `updatedOn`.
- **Notification**: `notificationId` (`N-000001`…), `userId`, `message`, `createdOn`, `read`.
- **Announcement**: `announcementId` (`ANN-000001`…), `courseCode`, `facultyId`, `title` (≤100), `body` (≤500), `postedOn`.
- **ResourceItem**: `resourceId` (`RES-000001`…), `courseCode`, `title`, `linkOrNote` (≤300), `addedOn`.
- **WaitlistEntry**: `studentId`, `courseCode`, `joinedOn`.
`IdGenerator` produces the next id by scanning existing ids (prefix + zero-padded 6 digits).

## B5. Exceptions (all extend `AppException extends Exception`; message is user-friendly)
| Class | Thrown when | Message |
|---|---|---|
| `AuthenticationException` | wrong ID/password, inactive/locked account | `Invalid user ID or password.` (locked: `Too many attempts. Try again later.`; inactive: `This account is not active. Contact the Admin.`) |
| `AccessDeniedException` | role/ownership violation | `You are not allowed to do this.` |
| `ValidationException` | input breaks a rule | field-specific, e.g. `Credits must be from 1 to 6.` |
| `DuplicateResourceException` | id/email/roll/course code exists | `That ID already exists.` |
| `ResourceNotFoundException` | item not found | `<item> was not found.` |
| `RegistrationClosedException` | register/drop while window closed | `Registration is currently closed.` |
| `DuplicateRegistrationException` | already actively registered | `You are already registered for <code>.` |
| `CreditLimitExceededException` | total credits would exceed max | `This would exceed the maximum of <n> credits.` |
| `ScheduleClashException` | time overlap | `This course clashes with <code>.` |
| `CourseFullException` | no seat | `Course <code> is full. No seats are left.` |
| `FeedbackNotAllowedException` | not registered / window or period closed | `Feedback is not allowed for this course right now.` |
| `DataAccessException` | file cannot be read/written | `A data error occurred. Please contact the Admin.` |
Also give each a `serialVersionUID`.

## B6. Singleton `AppConfig` and settings file
`AppConfig.getInstance()` holds settings, loaded from / saved to `data/settings.properties` (java.util.Properties) on every change. Keys and defaults:
```
registrationOpen=true        feedbackOpen=false         maxCredits=24
lowRatingThreshold=3.0       minResponses=3             botMinScore=0.5
strengthLimit=4.3            topRatedLimit=4.3          doubtOverdueDays=3
alertThreshold=3.0           maxLoginFailures=3         lockMinutes=5
```
`feedbackOpen` controls the simple end-of-term feedback; teaching-feedback availability is controlled by **periods** (B4).

---

# PART C — DATA, VALIDATION, SECURITY, RULES

## C1. Data files (UTF-8, `|` delimiter, first line = header, skipped on load)
| File | Fields in order |
|---|---|
| `users.csv` | userId, role, name, email, salt, passwordHash, active, mustChangePwd, roll_or_employeeId, department, semester_or_designation |
| `courses.csv` | courseCode, title, credits, capacity, semester, facultyId, dayOfWeek, startTime, endTime, active |
| `registrations.csv` | registrationId, studentId, courseCode, registeredOn, status, droppedOn |
| `feedback.csv` | feedbackId, studentId, courseCode, ratingContent, ratingTeaching, ratingMaterials, ratingAssessment, comment, submittedOn |
| `periods.csv` | periodLabel, open |
| `teaching_feedback.csv` | tfId, studentId, courseCode, periodLabel, ratings (ten integers separated by `;`, `0` = skipped, in the order of the FeedbackCategory enum), tags (separated by `;`), suggestion, moderation, submittedOn |
| `faq.csv` | faqId, courseCode, question, answer, keywords, helpfulVotes, unhelpfulVotes, active |
| `doubts.csv` | doubtId, studentId, courseCode, question, anonymous, status, answer, answeredBy, askedOn, answeredOn |
| `suggestion_rules.csv` | ruleId, type (THRESHOLD/TAG), target, limit, advice, active |
| `actions.csv` | actionId, facultyId, courseCode, category, baseAverage, suggestion, status, createdOn, updatedOn |
| `notifications.csv` | notificationId, userId, message, createdOn, read |
| `announcements.csv` | announcementId, courseCode, facultyId, title, body, postedOn |
| `resources.csv` | resourceId, courseCode, title, linkOrNote, addedOn |
| `waitlist.csv` | studentId, courseCode, joinedOn |
| `settings.properties` | see B6 |
| `blocked_words.txt`, `sentiment_positive.txt`, `sentiment_negative.txt` | one lowercase word per line |
| `logs/audit.log` | appended lines: `2026-10-05T14:30:12 | userId | ACTION | result` (never passwords/hashes) |

**Storage behaviour (a `FileStore` helper, tested):**
- Create missing files/folders with their header on first run.
- Load into memory (use `HashMap`/`ArrayList`/`LinkedHashMap`) once at start; rewrite the whole file after every change using **safe save** (`FileUtil.writeAtomic`: write `name.tmp` in the same folder, then `Files.move(..., REPLACE_EXISTING, ATOMIC_MOVE)`, falling back to a normal replace move if atomic is unsupported).
- A damaged line: report `file:line` on screen, skip it, keep a `.bak` copy of the original file (only once per run), continue. Never crash.
- Text fields are sanitised with `TextSanitizer` (replace `|`, `\r`, `\n` by a space; trim) **before** saving.
- `enrolledCount` of every course is recomputed from registrations with status REGISTERED at start-up.

## C2. Input validation (implement in `InputValidator`; messages name the field and allowed range)
| Field | Rule |
|---|---|
| User ID | 4–20 chars, letters/digits/underscore, starts with a letter |
| Name | 2–60 chars: letters, spaces, dot, hyphen, apostrophe |
| E-mail | basic pattern `^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$` |
| Password | 8–64 chars, ≥1 letter and ≥1 digit, not equal to the user ID |
| Roll number / Employee ID | 3–20 chars: letters, digits, hyphen |
| Department | 2–50 chars |
| Semester | 1–12 |
| Course code | 2–4 uppercase letters + 3 digits (e.g. `CS101`) |
| Course title | 3–100 chars |
| Credits | 1–6 |  
| Capacity | 1–200 |
| Day / time | day `MON..SUN`; times `HH:mm` 24-hour; end after start; slot is optional but all three or none |
| Rating | whole number 1–5 |
| Comment / suggestion | 0–300 chars |
| Doubt text | 5–500 chars |
| Announcement title/body | ≤100 / ≤500 |
| Menu choice | integer within the listed options |
`ConsoleHelper` offers `readInt(prompt,min,max)`, `readLine(prompt)`, `readNonEmpty(prompt)`, `readYesNo(prompt)`, `readPassword(prompt)` (use `System.console().readPassword` when available, otherwise fall back to visible input), each repeating the prompt until valid.

## C3. Security
- `PasswordUtil`: PBKDF2WithHmacSHA256, 120 000 iterations, 256-bit key, 16-byte random salt (`SecureRandom`), Base64 for salt and hash, constant-time comparison (`MessageDigest.isEqual`). Methods `newSalt()`, `hash(password, salt)`, `matches(password, salt, storedHash)`.
- Default Admin on first run when no users exist: user ID `admin`, initial password `Admin@123`, `mustChangePassword=true`. Print a notice on screen and document it in the README.
- Login: generic failure message; after `maxLoginFailures` consecutive failures for one ID, lock that ID for `lockMinutes` (kept in memory, uses the injected `Clock`).
- Forced password change on first login and after an Admin reset (temporary password: generate 10 random letters+digits, show it once to the Admin).
- **Role/ownership checks in every service method** (the acting `User` is passed in). Hiding menu items is never the only protection. Throw `AccessDeniedException`.
- The last active Admin cannot be deactivated.

## C4. Business rules (enforce in services; each must have a test)
- **BR-01** Registration only while `registrationOpen`.  **BR-02** Never more active registrations than capacity.  **BR-03** One active registration per student+course.  **BR-04** Total active credits ≤ `maxCredits` (limit is inclusive).  **BR-05** Only active users log in/register; only active courses accept registrations; only active **Students** register.  **BR-06** Dropping only while `registrationOpen`.  **BR-07** Simple feedback only for REGISTERED courses.  **BR-08** One simple feedback per student+course; resubmitting edits it.  **BR-09** Ratings 1–5.  **BR-10** Feedback is anonymous in every view (student id never shown).  **BR-11** Capacity cannot go below current enrolment.  **BR-12** Users/courses with history are deactivated, never deleted.  **BR-13** IDs unique and immutable.  **BR-14** Faculty see only own courses; students see only own data.  **BR-15** A dropped registration registered again is **reactivated** (same record).  **BR-16** Last active Admin cannot be deactivated.
- **BR-17** The bot answers only from stored FAQ entries, never invents.  **BR-18** Bot match needs ≥2 shared meaningful words (or 1 when the doubt has exactly one meaningful word) **and** score ≥ `botMinScore`.  **BR-19** FAQ scoped to its course or `ALL`.  **BR-20** No match, or answer marked not helpful → escalate to the course's faculty (ticket OPEN).  **BR-21** Anonymous doubts hide the student id in **every** view/report.  **BR-22** Teaching feedback only for REGISTERED courses, once per student+course+period, only while the period is open.  **BR-23** Each category 1–5 or skipped; ≥4 categories rated; ≤5 tags.  **BR-24** Suggestion ≤300 chars, sanitised, checked against blocked words (match → `HELD`).  **BR-25** No teaching result/suggestion/comment is shown to faculty until the course-period has ≥ `minResponses` responses.  **BR-26** Suggestions visible only to the teaching faculty and Admin.  **BR-27** Suggestions are guidance only; never used to rank/penalise automatically (label them as guidance).  **BR-28** Waitlist is FIFO; auto-enrol re-checks every registration rule.  **BR-29** Time clash rule (same day, overlapping times).  **BR-30** Rule-book, FAQ-moderation and blocked-word changes are audited.

## C5. Registration algorithm (exact order inside `RegistrationService.register(student, courseCode)`)
1. acting user must be an active `Student` → else `AccessDeniedException`
2. `registrationOpen` → else `RegistrationClosedException`
3. course exists and is active → else `ResourceNotFoundException`
4. no active registration for the pair → else `DuplicateRegistrationException`
5. credits used + course credits ≤ `maxCredits` → else `CreditLimitExceededException`
6. no time clash with the student's other active courses → else `ScheduleClashException` (names the clashing code)
7. `course.addStudent()` → else `CourseFullException`
8. save: if a DROPPED record exists for the pair, **reactivate** it; otherwise create a new `Registration` with the next id
9. persist course (enrolled count is derived; just keep memory consistent), write audit line, return the registration.
Any failure → nothing is saved. `drop()`: window open, active registration exists, sets DROPPED + date, `removeStudent()`, audit, then **calls `WaitlistService.offerSeat(courseCode)`**.

---

# PART D — FEATURES

## D1. Release 1 (core) — everything below must work before anything in D2/D3
**AUTH** login/logout (menu returns to login screen), change own password, lock-out, forced change, Admin reset.  
**USERS (Admin)** add student/faculty (all fields validated, unique id/email/roll/employee id), list by role, search by id/name/department, update name/e-mail/department/semester(designation), activate/deactivate (not last admin), view own profile (all roles).  
**COURSES** Admin adds/updates/deactivates courses and assigns an active faculty member; everyone browses and **searches** (`search(String keyword)` by code or title, **overload** `search(int semester)`, and by faculty); catalogue columns: code, title, credits, semester, faculty name, seats left/capacity, `FULL` marker; faculty "My courses".  
**REGISTRATION** register (C5), drop, "My registrations" with total credits used and remaining, roster (faculty own courses / admin any: id, name, roll number).  
**SETTINGS (Admin)** open/close registration; set max credits; open/close simple feedback; set low-rating threshold and min responses.  
**SIMPLE FEEDBACK** four ratings + comment per registered course, edit allowed while `feedbackOpen`, student sees "courses needing feedback"; faculty/Admin see responses, four criterion averages, overall average, comments (no student ids); minimum-responses protection for faculty view.  
**REPORTS (Admin)** `Report` interface with `title()`, `rows()` (list of string arrays) and implementations: Enrolment (code, title, faculty, capacity, enrolled, seats left, fill %), Feedback summary (responses, 4 averages, overall), Faculty ratings (average overall per faculty), Low-rated courses (overall < `lowRatingThreshold`), Student summary (courses + credits). One generic method prints any `Report` as an aligned table and one exports it to `exports/<name>-<timestamp>.csv`.  
**DATA/ADMIN** backup export of all data files to `backups/backup-<timestamp>/`; audit log viewer (last 50 lines); sample data loader.  
**GENERAL** every prompt validated; confirmations (Y/N) for drop and deactivate; option `0` = back/logout everywhere; top-level `try/catch` in the menu loops: `AppException` → show its message; any other `Exception` → log to audit + friendly "Something went wrong. Please try again."

## D2. Release 4 — Advanced Feature Pack (28 features). Build in three waves (see PART E).

### Wave 1 — the three core ideas (Must)
**F01 Smart Doubt Bot.** `Student → Ask a Doubt`: choose a course in which the student has an active registration, type the doubt (5–500 chars), optionally tick "ask anonymously".  
`TextTokenizer.tokenize(text)`: lower-case, split on `[^a-z0-9]+`, keep words of length ≥2 that are not stop words, return a `LinkedHashSet<String>`. Stop words: `a an the is are was to of in on for and or do does how what why can i me my we you it please tell about with between this that`.  
`DoubtBot.ask(courseCode, doubt)` returns `Optional<BotAnswer>`: tokenise the doubt (empty → no match); for every **active** FAQ entry whose course is `ALL` or equals the course: `overlap` = number of doubt words contained in the entry's token set (entry tokens = tokens of `question + " " + keywords`); `score = overlap / (double) doubtWords`; the entry qualifies if `(overlap ≥ 2 || (doubtWords == 1 && overlap == 1)) && score ≥ botMinScore`; return the qualifying entry with the highest score (first wins ties).  
**F02 Auto-escalation.** No match → create a `Doubt` with status `OPEN`, tell the student "Your doubt has been sent to <faculty name>. Doubt ID: D-…" (for anonymous doubts the faculty never sees the student), and add a notification for the course's faculty.  
**F03 Faculty doubt inbox.** `Faculty → Doubt Inbox`: list OPEN doubts of own courses (oldest first, shows doubt id, course, text, date — **never** the student id/name for anonymous ones; for non-anonymous show the roll number only if you decide to, record it in DECISIONS.md), choose one, type the answer → status `ANSWERED`, `answeredBy`, `answeredOn`, notify the student.  
**F09 Ten-category teaching ratings, F10 quick-tags, F11 typed suggestion.** `Student → Give Teaching Feedback`: choose an **open period** and a registered course; rate each of the ten categories 1–5 or press Enter/`0` to skip (need ≥4 rated); choose up to 5 tags from this fixed list (numbers on screen):  
 *Needs attention:* `Could not understand`, `Voice not clear`, `Too fast`, `Too slow`, `Need more examples`, `Board writing not visible`, `Doubts not addressed`, `Too much theory`.  
 *Strengths:* `Great examples`, `Clear voice`, `Helped me individually`, `Good pace`, `Very interactive`, `Good notes and materials`.  
 Then an optional typed suggestion (≤300). One submission per student+course+period; submitting again while the period is open **edits** it.  
**F15 Faculty performance dashboard.** `Faculty → My Dashboard`: for each own course and period, show responses; if `responses < minResponses` print only "Not enough responses yet (n of minResponses)"; otherwise show the average of every rated category (one decimal), the overall average (mean of the category averages), tag counts, strengths, suggestions (F16) and the anonymous typed suggestions that are `OK` (not HELD/REJECTED), plus the trend (overall average per period in time order).  
**F16 Smart improvement suggestions.** Implement exactly this design:
```java
public interface SuggestionRule { Optional<Suggestion> evaluate(CategoryStats stats); }
public class ThresholdRule implements SuggestionRule {   // fires when category average is rated and < threshold
    // severity = threshold - average
}
public class TagRule implements SuggestionRule {         // fires when (students using tag / responses) >= minShare
    // severity = share (0..1)
}
public class SuggestionEngine {                          // holds List<SuggestionRule>, int minResponses
    public List<Suggestion> suggest(CategoryStats stats) // empty if responses < minResponses; sorted by severity descending
}
```
`CategoryStats` = `responses`, `Map<FeedbackCategory,Double> averages` (skipped ratings are excluded from a category's average), `Map<String,Integer> tagCounts`. `StatsCalculator` builds it from the `TeachingFeedback` records of one course+period (only moderation `OK`).  
**Default rule book** (insert into `suggestion_rules.csv` on first run; `limit` is the threshold or min share):
| # | type | target | limit | advice |
|---|---|---|---|---|
|1|THRESHOLD|VOICE_CLARITY|3.5|Students find it hard to hear you clearly. Speak a little louder and check from the back row that everyone can hear.|
|2|THRESHOLD|EXAMPLES|3.5|Students want more examples. Add one or two worked examples for every new concept.|
|3|THRESHOLD|CONCEPT_CLARITY|3.5|Many students are not grasping the concepts. Re-explain difficult topics in simpler steps and recap at the end of each class.|
|4|THRESHOLD|EXPLANATION|3.5|Try breaking each topic into small steps and relate it to something familiar before using technical terms.|
|5|THRESHOLD|BOARD_USAGE|3.5|Write key points and diagrams on the board or slides while you explain, and make sure the writing is large enough to read.|
|6|THRESHOLD|DOUBT_GUIDANCE|3.5|Some students feel their doubts are not addressed. Keep five minutes at the end of class for questions or announce doubt-clearing times.|
|7|THRESHOLD|PACE|3.0|Students find the pace unsuitable. Pause after each concept and ask a quick question to check how the class is following.|
|8|THRESHOLD|INTERACTION|3.5|Ask short questions during class and invite students to answer or explain to a neighbour.|
|9|THRESHOLD|PUNCTUALITY|3.5|Try to start and finish on time; students notice delays.|
|10|THRESHOLD|MATERIALS|3.5|Share notes, slides or reference links before or after each class.|
|11|TAG|Could not understand|0.40|A large share of the class could not follow. Pause more often and use simpler examples.|
|12|TAG|Voice not clear|0.30|Several students reported voice problems. Check microphone or seating, and speak louder.|
|13|TAG|Need more examples|0.30|Students asked explicitly for more examples. Add real-life or code examples to each topic.|
|14|TAG|Doubts not addressed|0.30|Make doubt time visible: announce it, or use the Doubt Bot inbox regularly.|
Strengths: every rated category with average ≥ `strengthLimit` is listed ("Great with <label>"). Always label the suggestion block **"Guidance based on student opinion — not an evaluation"** (BR-27).

### Wave 2 — depth (Should)
**F04 Bot learns from faculty:** when answering a doubt the faculty may answer "Add to FAQ? (Y/N)" → new active `FaqEntry` (course, question, answer, keywords = tokens of the question joined by space), immediately usable by the bot.  
**F05 Doubt tracking & notices:** `Student → My Doubts` lists id, course, status, answer; the student can close an ANSWERED doubt; answering creates a notification.  
**F06 "Was this helpful?":** after a bot answer ask Y/N. Y → doubt saved as `BOT_ANSWERED` (status CLOSED-equivalent, no faculty involved) and `helpfulVotes++`; N → `unhelpfulVotes++` and escalate exactly as F02.  
**F12 Mid-term pulse / periods:** Admin creates, opens and closes periods; student sees which course+period still needs feedback.  
**F13 Comment moderation:** a suggestion containing any word from `blocked_words.txt` is saved `HELD`; Admin → Moderate Comments lists HELD comments (text only), Approve → `OK` / Reject → `REJECTED` (audited).  
**F17 Admin rule book:** Admin lists/adds/edits/activates/deactivates suggestion rules (type, target, limit, advice) with validation (THRESHOLD target must be a FeedbackCategory name, limit 1.0–5.0; TAG target must be one of the fixed tags, limit 0.05–1.0); changes audited; `SuggestionEngine` is rebuilt from the file each time the dashboard opens.  
**F18 Improvement action plan:** from the dashboard the faculty may save a suggestion as an `ImprovementAction` (store category and the **current average** as `baseAverage`) and later change status; "My Action Plan" lists actions.  
**F21 Ratings in the catalogue:** show overall course rating (mean of simple-feedback overall and/or teaching-feedback overall — choose and record in DECISIONS.md) and a `★ Top rated` marker when ≥ `topRatedLimit`, only when responses ≥ `minResponses`.  
**F23 Notification inbox:** every role has `Inbox (n unread)`; list newest first, mark read on open.  
**F24 Waiting list:** a student can join the waitlist of a full active course (not if already registered or already waiting), leave it, and see their position. `WaitlistService.offerSeat(courseCode)` (called after every drop and after a capacity increase): while a seat is free, take the first waiting student; try the normal registration algorithm (C5); on success remove them from the list and notify; on failure notify them of the reason, remove them, and try the next.  
**F25 Timetable:** weekly view (MON–SUN, courses with a slot sorted by start time) for the student; the clash rule (BR-29) is part of C5.  
**F28 Admin teaching-quality overview:** all courses ranked by overall rating (courses below `alertThreshold` marked `ALERT`), count of OPEN doubts and those older than `doubtOverdueDays`, faculty with open improvement actions. Totals only — no student names.

### Wave 3 — polish (Could)
**F07 Anonymous doubts** (fully enforce BR-21 in inbox, analytics, exports).  
**F08 Doubt analytics:** top 10 words (via the tokenizer) in doubts of the faculty's courses; FAQ entries with `unhelpfulVotes > helpfulVotes`; doubts open longer than `doubtOverdueDays` (alert shown in faculty and Admin menus).  
**F14 Comment keyword summary:** top 10 words in typed suggestions of a course+period; positive/negative/neutral comment counts using `sentiment_positive.txt` and `sentiment_negative.txt` (starter words — positive: good clear helpful great excellent useful nice; negative: confusing fast slow boring unclear difficult bad). Show as "rough indicator".  
**F19 Impact check:** for each action with a category, compare the latest period's average with `baseAverage` and print "improved by 0.6 / no change / dropped by 0.2".  
**F20 Strength badges & department benchmark:** badge "Great with <label>" for categories ≥ `strengthLimit`; the department average per category over all faculty of the same department, shown only if ≥3 faculty have enough responses.  
**F22 Course recommendations:** "Courses for you": active courses of the student's department... (course department = its faculty's department) with `semester ≥` the student's semester and `≤ semester + 1`, seats left, not already registered, no time clash, sorted by overall rating (unrated last); show a one-line reason.  
**F26 Course resources** and **F27 Announcements:** faculty add/list for own courses; registered students (and Admin) read; a new announcement notifies registered students.

## D3. Menus (exact structure; the numbers may be adjusted but order should be kept)
**Login screen:** `1. Login  0. Exit`.  
**Student:** 1 Browse/search courses · 2 Register for a course · 3 Drop a course · 4 My registrations · 5 Submit/edit course feedback (simple) · 6 Give teaching feedback · 7 Ask a doubt · 8 My doubts · 9 My timetable · 10 Waiting list · 11 Courses for you · 12 Announcements & resources · 13 Inbox (n unread) · 14 My profile / change password · 0 Logout.  
**Faculty:** 1 My courses · 2 Students in a course · 3 Course feedback summary (simple) · 4 My dashboard & suggestions · 5 Doubt inbox · 6 FAQ for my courses · 7 My action plan · 8 Post announcement / add resource · 9 Doubt analytics · 10 Browse courses · 11 Inbox · 12 Profile / change password · 0 Logout.  
**Admin:** 1 Manage students · 2 Manage faculty · 3 Manage courses · 4 Settings (windows, credit limit, thresholds) · 5 Feedback periods · 6 Moderate comments · 7 Suggestion rule book · 8 Manage FAQ · 9 Reports (and CSV export) · 10 Feedback summaries · 11 Teaching-quality overview · 12 Backup & audit log · 13 Load demo data · 14 Inbox · 15 Change password · 0 Logout.  
Screens: title line, numbered list, prompt. After each action print a clear success or failure message. Show a `FULL` marker on full courses and an unread count on the inbox.

## D4. Demo data (`SampleDataLoader`, only invented values)
Create: the default Admin; 3 faculty (`fac101..fac103`, "Demo Faculty 1..3"); 10 students (`stu1001..stu1010`, "Demo Student 1..10", rolls `R-1001..R-1010`, semesters 3–4); 8 courses including one with capacity 2, one inactive, and time slots that make one clash pair; password for every demo account `Welcome123`, `mustChangePassword=false`; registrations that leave one student exactly at the credit limit and fill the capacity-2 course; 20+ FAQ entries (generic Java/OOP/DBMS and registration questions, with course codes `ALL` or the demo course codes); one open period `MID-TERM` with 5–8 teaching-feedback records for one course with a deliberately low Examples average and several "Could not understand" tags (so the dashboard shows real suggestions); the 14 default rules; the three word lists. README lists the demo accounts.

---

# PART E — PHASES, ACCEPTANCE GATES AND TESTS

Work through the phases in order. After each phase: compile (`javac -Xlint:all`), run the test runner, update `PROGRESS.md`, and fix failures before continuing.

| Phase | Build | Gate (must pass before moving on) |
|---|---|---|
| **0 Scaffold** | folder tree, scripts (`build`, `run`, `test` for sh and bat), `TestRunner`, `AppConfig`, `FileUtil`, `TextSanitizer`, `IdGenerator`, `DateUtil` | project compiles; `test` script runs and reports 0 failures |
| **1 Model** | all model classes/enums, exceptions, `InputValidator`, `PasswordUtil`, `UserFactory` | unit tests: validators (valid + invalid for every row of C2), password hash/verify/different salts, `Course` seats/equals/hashCode/clash, `Registration` drop/reactivate, `Feedback.overallRating`, polymorphic `showMenu()` |
| **2 Storage** | all DAO interfaces, `FileStore`, File DAOs for users, courses, registrations, feedback, periods | tests with temp folders: save→reload equality, missing file recreated, damaged line skipped with `.bak`, no `.tmp` left, enrolled counts recomputed |
| **3 Core services** | `AuthService`, `UserService`, `CourseService`, `SettingsService`, `AuditLogger` | tests: login ok/wrong/unknown (same message), lock-out with fixed `Clock`, inactive refused, password policy, duplicate id/email, last-admin rule, capacity-below-enrolment rule, role checks |
| **4 Registration** | `RegistrationService` (C5), `WaitlistService` skeleton (no-op until Phase 8), `TimetableService` clash check | tests for TC-13…TC-21 plus clash |
| **5 Simple feedback + reports** | `FeedbackService`, `ReportService` and all reports, CSV export | tests for TC-22…TC-28, min-responses protection |
| **6 UI core** | `ConsoleHelper`, `Main`, Login/Student/Faculty/Admin menus for all Release 1 features, `--demo`, `SampleDataLoader` (core part) | scripted run: feed a text file of keystrokes via `java -cp out ui.Main < script.txt` and show the transcript for: admin login & forced password change, add faculty/student/course, student register/drop, bad input does not crash (TC-31) |
| **7 Wave 1** | F01, F02, F03, F09, F10, F11, F15, F16 (+ DoubtService, TeachingFeedbackService, FacultyInsightService, bot, engine, DAOs, menus, demo data) | tests TC-35…TC-39, TC-44…TC-54; the demo dashboard shows the Examples and "Could not understand" suggestions; fewer than 3 responses shows nothing |
| **8 Wave 2** | F04, F05, F06, F12, F13, F17, F18, F21, F23, F24, F25, F28 | tests TC-40, TC-41, TC-48…TC-49, TC-55, TC-56, TC-58…TC-62, TC-64 |
| **9 Wave 3** | F07, F08, F14, F19, F20, F22, F26, F27 | tests TC-42, TC-43, TC-57, TC-63 and one test each for F14, F20, F22 |
| **10 Finish** | README, CONCEPT_MAP, UML.puml, TEST_REPORT, full regression, code-review pass | all tests pass; final report (PART F) |

## E1. Test cases to implement (system-level behaviours; implement as automated tests where possible, otherwise as scripted-input runs)
**Auth/users/courses** TC-01 login each role · TC-02 wrong password → generic message · TC-03 unknown id → same message · TC-04 inactive refused · TC-05 3 wrong passwords lock for 5 min (even a correct password refused during lock) · TC-06 weak then valid new password · TC-07 add student ok · TC-08 duplicate id and duplicate e-mail rejected · TC-09 invalid fields rejected one by one · TC-10 add course ok · TC-11 duplicate course code rejected · TC-12 capacity below enrolment rejected · TC-34 last Admin cannot be deactivated.  
**Registration** TC-13 success + confirmation + seats −1 · TC-14 window closed blocks register and drop · TC-15 unknown/inactive course · TC-16 duplicate registration · TC-17 reach credit limit exactly (accepted) · TC-18 one more refused · TC-19 full course refused · TC-20 drop releases seat · TC-21 re-register reactivates same record (no second record).  
**Simple feedback/reports/data** TC-22 submit ok · TC-23 unregistered/dropped refused · TC-24 window closed refused · TC-25 rating 0, 6, "abc" rejected · TC-26 resubmit edits (one record) · TC-27 faculty sees own course anonymously; other faculty's course → `AccessDeniedException` · TC-28 report figures equal hand-computed values and CSV export opens · TC-29 data survives restart · TC-30 missing file recreated + damaged line skipped with `.bak` · TC-31 junk input never crashes · TC-32 `|` and line breaks sanitised · TC-33 enrolled count recomputed from edited registrations file.  
**Advanced** TC-35…TC-64 exactly as: 35 reworded doubt matches FAQ · 36 unrelated doubt → OPEN ticket · 37 FAQ of another course ignored · 38 doubt for unregistered course refused · 39 faculty answers (only own-course doubts visible) · 40 Add-to-FAQ then same doubt answered instantly · 41 "not helpful" escalates and counts · 42 anonymous doubt shows no student id anywhere · 43 doubt open >3 days flagged (use fixed clock) · 44 teaching feedback with ten ratings saved · 45 <4 categories or rating 6 rejected · 46 six tags rejected · 47 duplicate edits; dropped course refused · 48 closed period refused · 49 blocked word → HELD, hidden from faculty · 50 dashboard with 2 responses shows only the count · 51 averages equal a hand calculation · 52 Examples 2.4 and Voice 3.1 → both suggestions, examples first · 53 all averages 4.5 and no tags → no suggestions, strengths listed · 54 other faculty's dashboard / student access denied · 55 changing a rule limit changes the output and is audited · 56 action saved, status persists · 57 impact check shows +0.8 for 2.4→3.2 · 58 catalogue rating hidden under 3 responses, shown at 8 · 59 notification created, unread count 1 then 0 · 60 waitlist auto-enrols the first student when a seat frees · 61 first student at credit limit is skipped with a reason, next is tried · 62 clashing registration refused naming the course · 63 announcement visible to registered student only, with notification · 64 overview ranks courses, flags low-rated, shows no student names.  
**Unit tests (additional):** `DoubtBot` (wording variation, general FAQ, course scoping, unrelated, empty, one-word doubt), `SuggestionEngine` (verified reference: with 10 responses, Examples 2.4, Voice 3.1, Board 4.2, Explanation 4.0 and 5/10 students tagging "Could not understand", the suggestions are exactly {Examples (severity 1.10), "Could not understand" tag (0.50), Voice (0.40)} in that order and Board does not fire; with 2 responses → empty list), `PasswordUtil`, `FileUtil.writeAtomic`, `Course.clashesWith`, and every business rule BR-01…BR-30 at least once with a passing and a failing case.

---

# PART F — DOCUMENTS, FINAL REPORT AND SELF-CHECK

## F1. Documents to produce
- **README.md**: what it is/is not, requirements (JDK 17+), how to build/run/test on Windows, Linux and macOS, folder layout, default Admin login, demo accounts, how to use each role (short walkthrough), how to reset demo data, known limitations (keyword bot, rule-based suggestions, single user, files).
- **CONCEPT_MAP.md**: a table of every OOP concept used → real class/method names (classes & objects, constructors, encapsulation, inheritance, overriding, overloading, polymorphism, abstract class, interface, static/final, this/super, packages, exceptions, collections, generics, enums, equals/hashCode/toString, strings/regex, date-time API, file I/O, Scanner, Singleton/DAO/Factory/dependency injection, Comparator sorting, Optional). Explain each in one beginner-friendly sentence.
- **UML.puml**: PlantUML text for the use-case diagram, class diagram (model, DAOs, services, engine, bot) and a sequence diagram of "register for a course" and of "ask a doubt".
- **TEST_REPORT.md**: table of every test (id, description, PASS/FAIL, how it was run) generated from real runs, plus the raw runner output. Do not edit results by hand.
- **PROGRESS.md / DECISIONS.md** as in A5.

## F2. Final report (print at the end, plain text)
1. Total files/lines, packages, number of classes.
2. Build and test commands and the **actual** final output (tests passed/failed).
3. For each of F01–F28: *implemented & tested / implemented, not tested / not implemented* with one line of evidence.
4. For each of TC-01…TC-64: PASS/FAIL (or "manual").
5. Every deviation from this prompt and why; every known bug or limitation.
6. A 10-step demo script the student can follow (login as Admin → … → faculty dashboard shows suggestions).

## F3. Self-check before you finish (answer each yes/no in the report)
- Program compiles with no errors and no raw warnings that matter; no `TODO` left.
- No business rule in `ui` or `dao`; no `System.out`/`Scanner` in `service`/`dao`.
- Every public class/method has Javadoc; names follow conventions; no duplicated blocks.
- Passwords never appear in output, logs or data files in plain text.
- Garbage input at every prompt type was tried and did not crash the program.
- Data written in one run is present in the next run.
- Demo data loads and the faculty dashboard shows real, rule-generated suggestions.
- The bot answered a reworded FAQ doubt and escalated an unrelated one.
- Nothing claims AI; suggestions are labelled as guidance.

---

# PART G — PHASE PROMPTS (use only if your assistant stops or you prefer step-by-step)

Send these one at a time after pasting PART A–F once. Each says what to do next.

1. **"Do Phase 0 and Phase 1 now. Show me the files you created, the compile output and the test results. Explain in simple words what each model class does."**
2. **"Do Phase 2 and Phase 3. Show test results. Explain how the DAO interface lets the service stay unaware of files."**
3. **"Do Phase 4 and Phase 5. Show the registration tests TC-13…TC-21 and feedback tests TC-22…TC-28 with real output."**
4. **"Do Phase 6. Run the scripted keystroke transcript for the core demo and paste it. Fix anything that crashes."**
5. **"Do Phase 7 (Wave 1). Load the demo data, then show the Doubt Bot answering a reworded question, escalating an unrelated one, and the faculty dashboard with generated suggestions."**
6. **"Do Phase 8 (Wave 2). Show the waiting list auto-enrolling, the timetable clash refusal, comment moderation and the rule-book edit changing the dashboard."**
7. **"Do Phase 9 (Wave 3) only if everything earlier passes. Then do Phase 10: documents, regression, final report."**
8. **"Review the whole code base against the self-check in F3 and list anything that fails. Fix it, re-run all tests, and update TEST_REPORT.md from the real run."**

---
*End of master prompt.*
