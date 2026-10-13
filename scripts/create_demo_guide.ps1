$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$out = Join-Path $root 'docs\CRFMS_Testing_and_Demonstration_Guide.docx'
$word = New-Object -ComObject Word.Application
$word.Visible = $false
$word.DisplayAlerts = 0
$doc = $word.Documents.Add()
$sel = $word.Selection
$wdCollapseEnd = 0; $wdPageBreak = 7; $wdNormal = -1; $wdHeading1 = -2; $wdHeading2 = -3; $wdTitle = -63; $wdListBullet = 2; $wdListNumber = 3
function Add-Text([string]$text, [int]$style = -1) {
  if ($style -eq $wdTitle) { $sel.Style = 'Title' }
  elseif ($style -eq $wdHeading1) { $sel.Style = 'Heading 1' }
  elseif ($style -eq $wdHeading2) { $sel.Style = 'Heading 2' }
  else { $sel.Style = 'Normal' }
  $sel.Font.Name = 'Aptos'; $sel.Font.Size = 10.5; $sel.Font.Bold = 0
  if ($style -eq $wdTitle) { $sel.Font.Size = 22; $sel.Font.Bold = 1 }
  if ($style -eq $wdHeading1) { $sel.Font.Size = 15; $sel.Font.Bold = 1 }
  if ($style -eq $wdHeading2) { $sel.Font.Size = 12; $sel.Font.Bold = 1 }
  $sel.TypeText($text); $sel.TypeParagraph()
}
function Add-Bullets([string[]]$items) { foreach($item in $items) { $sel.Style = 'List Bullet'; $sel.Font.Name = 'Aptos'; $sel.Font.Size = 10.5; $sel.TypeText($item); $sel.TypeParagraph() } }
function Add-Numbers([string[]]$items) { foreach($item in $items) { $sel.Style = 'List Number'; $sel.Font.Name = 'Aptos'; $sel.Font.Size = 10.5; $sel.TypeText($item); $sel.TypeParagraph() } }
function Break-Page { $sel.InsertBreak($wdPageBreak) }
function Add-Command([string]$command) { $sel.Style = 'Normal'; $sel.Font.Name = 'Consolas'; $sel.Font.Size = 9.5; $sel.Font.Bold = 0; $sel.TypeText($command); $sel.TypeParagraph() }
function Add-Table([string[]]$headers, [object[]]$rows) {
  $range=$sel.Range; $table=$doc.Tables.Add($range,$rows.Count+1,$headers.Count); $table.Style='Table Grid'; $table.Rows.Item(1).Range.Font.Bold=1
  for($c=1;$c -le $headers.Count;$c++){ $table.Cell(1,$c).Range.Text=$headers[$c-1] }
  for($r=0;$r -lt $rows.Count;$r++){ for($c=0;$c -lt $headers.Count;$c++){ $table.Cell($r+2,$c).Range.Text=[string]$rows[$r][$c] } }
  $sel.SetRange($table.Range.End,$table.Range.End); $sel.TypeParagraph()
}

Add-Text 'CRFMS Testing and Demonstration Guide' $wdTitle
Add-Text 'Course Registration and Feedback Management System' 0
Add-Text 'Purpose: use this guide to test the Java desktop application and present a confident, honest end-to-end demonstration to the faculty evaluator.' 0
Add-Text 'What you will show' $wdHeading1
Add-Bullets @('Secure sign-in and role-based workspaces','Course creation, student registration, waitlist and notifications','Feedback privacy, teaching-quality guidance, FAQ doubt support and communication','Reports, backups, audit log and persistence')
Add-Text 'Important demonstration rule' $wdHeading1
Add-Text 'Use only fictional demo data. Never display a real password after it has been changed, and do not open or share data\crfms.db outside the project machine.' 0

Break-Page
Add-Text '1  Before You Start' $wdHeading1
Add-Text 'Required software' $wdHeading2
Add-Bullets @('Windows 10 or 11','JDK 17 or newer. A JRE alone is not sufficient.','Internet only if the three library JAR files are not already present in lib.')
Add-Text 'Open Command Prompt in the project folder' $wdHeading2
Add-Text 'In File Explorer, open the CRFMS_Desktop folder. Click the address bar, type cmd, and press Enter. Run the following checks.' 0
Add-Command 'java -version'
Add-Command 'javac -version'
Add-Text 'Expected result: each command shows version 17 or newer. If javac is not recognized, install a JDK and set JAVA_HOME/PATH before continuing.' 0
Add-Text 'Build and automated test commands' $wdHeading2
Add-Command 'build.bat'
Add-Command 'test.bat'
Add-Text 'Expected result for test.bat: SecurityTest: 4 checks passed; SchemaValidation: 14 checks passed; DatabaseIntegrationTest: extended integration checks passed. These tests compile the project, check password security, validate the SQLite schema, and exercise key database workflows.' 0
Add-Text 'Start the demonstration database' $wdHeading2
Add-Command 'run.bat --demo'
Add-Text 'The first demo launch creates or reuses fictional courses, accounts, FAQ entries and a feedback period. If a previous practice session makes the data confusing, close the app and preserve data\crfms.db as a backup before starting over.' 0

Break-Page
Add-Text '2  Demo Accounts and Password Setup' $wdHeading1
Add-Text 'Use these fictional accounts after running run.bat --demo.' 0
Add-Table @('Role','User ID','Initial password','First action'), @(
  @('Administrator','admin','Admin@123','Change password when prompted'),
  @('Faculty','fac101','Welcome123','Change password when prompted'),
  @('Student','stu1001','Welcome123','Change password when prompted')
)
Add-Text 'Suggested practice passwords' $wdHeading2
Add-Text 'Use a different strong password for each account, for example AdminDemo2026, FacultyDemo2026 and StudentDemo2026. The password must be 8-64 characters, contain letters and digits, and differ from the user ID.' 0
Add-Text 'First login procedure' $wdHeading2
Add-Numbers @('Enter admin and Admin@123, then complete the forced password change.','Click Logout. Sign in as fac101 with Welcome123 and change that password.','Click Logout. Sign in as stu1001 with Welcome123 and change that password.','Return to admin using the new password. Do not type the replacement passwords aloud during the demonstration.')
Add-Text 'What to explain' $wdHeading2
Add-Text 'Passwords are stored as PBKDF2-HMAC-SHA256 hashes with a random salt. The system forces a password change for initial and reset passwords, and locks an account for five minutes after three failed sign-in attempts.' 0

Break-Page
Add-Text '3  Recommended 10 Minute Demonstration' $wdHeading1
Add-Table @('Time','Screen and action','What to say'), @(
  @('0:00-1:00','Admin Overview','This is the administrator dashboard. It summarizes users, active courses, registrations, feedback and open doubts.'),
  @('1:00-2:00','Users and Courses','The admin creates and manages accounts, assigns faculty, and configures capacity, credits, semester and timetable.'),
  @('2:00-3:30','Student Course catalogue','A student can search courses, see available seats, register, and view privacy-protected ratings.'),
  @('3:30-4:30','Waitlist and notifications','When a course is full, the next request enters FIFO waitlist. A drop triggers eligibility checking and promotion.'),
  @('4:30-5:30','Feedback','Simple feedback is anonymous in reports and averages remain hidden until the response threshold is met.'),
  @('5:30-6:30','FAQ and doubts','Known questions receive a stored FAQ answer; unrelated questions are escalated to the assigned faculty.'),
  @('6:30-7:30','Faculty workspace','Faculty see only their courses, answer doubts, post announcements and add resources.'),
  @('7:30-8:30','Teaching quality','Ten-category feedback produces transparent rule-based guidance; it is not AI ranking or discipline.'),
  @('8:30-9:30','Audit, benchmark and exports','Admin can review the latest audit events, privacy-gated department averages, CSV reports and a SQLite backup.'),
  @('9:30-10:00','Persistence','Close and reopen to show that database records remain saved locally.')
)

Break-Page
Add-Text '4  Detailed Click by Click Demonstration' $wdHeading1
Add-Text 'A  Administrator workflow' $wdHeading2
Add-Numbers @('On Overview, click Refresh. Point out the counts. Click Export courses CSV and explain it creates exports\courses.csv.','Open Users. Click Add user and create a temporary student only if you need another account for waitlist/feedback testing. Explain that Reset password shows a one-time temporary password and forces a change.','Open Courses. Click Add course. Use a distinct course code, 3 credits, capacity 1 or 2, semester 3, faculty fac101, and a valid day/time.','Open Settings. Confirm Registration open is 1, Course feedback open is 1, and Minimum feedback responses is 3.','Open Audit log. Click Refresh last 50 and show that actions are time stamped without exposing passwords.','Open Department benchmarks. Explain that an average appears only if the department has at least three faculty and enough feedback responses; otherwise it remains suppressed.')
Add-Text 'B  Student workflow' $wdHeading2
Add-Numbers @('Log in as stu1001. Open Course catalogue and select a course. Click Register selected.','Open My registrations to show the active record. Select it and click Drop selected only if you are demonstrating waitlist promotion.','Open Course feedback, click My registered courses, then Submit or edit feedback. Enter a rating from 1 to 5 and a short constructive comment.','Open Ask a doubt. Enter a registered course code. Ask a known FAQ question first; then ask an unrelated question and choose anonymous submission.','Open Notifications and show the unread count and Mark selected read action.')
Add-Text 'C  Faculty workflow' $wdHeading2
Add-Numbers @('Log in as fac101. Open My courses and point out that only assigned courses are visible.','Open Doubts inbox and answer the escalated doubt.','Open Announcements and post a short course notice. Open Resources and add a valid https link.','Open Teaching insights and load a course and feedback period. Explain that results appear only after the minimum response count.')

Break-Page
Add-Text '5  How to Demonstrate Important Rules' $wdHeading1
Add-Table @('Rule','How to show it','Expected message or outcome'), @(
  @('Timetable clash','Register one student in two overlapping courses.','The second registration is rejected and identifies the timetable clash.'),
  @('Full course waitlist','Set capacity to 1, register one student, then register another.','Second student enters FIFO waitlist.'),
  @('Promotion','Drop the first registration.','The earliest eligible waiting student is registered and notified.'),
  @('Feedback privacy','Show 1 or 2 responses with threshold 3, then submit a third response.','Average is hidden below threshold and visible at/above threshold.'),
  @('Anonymous doubt','Student chooses anonymous while asking a doubt.','Faculty sees Anonymous rather than the student ID.'),
  @('FAQ escalation','Ask a question that does not match stored FAQ.','It becomes an OPEN faculty ticket.'),
  @('Role isolation','Try to access another faculty member course from faculty workspace.','The service denies access.'),
  @('CSV safety','Export a report.','Text beginning with =, +, -, @ or tab is prefixed as text before export.')
)
Add-Text 'Teaching feedback explanation' $wdHeading2
Add-Text 'Students can rate up to ten teaching categories and must rate at least four. Up to five tags are permitted. The system suppresses results until the period threshold is reached. Guidance is generated by visible, editable threshold rules, and held suggestions require administrator moderation.' 0

Break-Page
Add-Text '6  Test Checklist for Submission Day' $wdHeading1
Add-Text 'Run these items before meeting the evaluator.' 0
Add-Table @('Check','Command or action','Pass condition'), @(
  @('Compile','build.bat','Build passed message; no compiler warnings or errors.'),
  @('Automated checks','test.bat','All three pass lines displayed.'),
  @('Launch','run.bat --demo','Login window opens.'),
  @('Admin sign-in','admin plus current demo password','Admin tabs load.'),
  @('Faculty sign-in','fac101 plus current demo password','Only faculty scopes are available.'),
  @('Student sign-in','stu1001 plus current demo password','Catalogue and student tabs load.'),
  @('Persistence','Close, relaunch, sign in','Created records remain.'),
  @('Export','Overview or Registrations export button','CSV is present in exports folder.'),
  @('Backup','Overview Back up database button','Timestamped .db is present in backups folder.')
)
Add-Text 'If a test fails' $wdHeading2
Add-Bullets @('Confirm JDK 17+: java -version and javac -version.','Run from the project folder, not from a copied subfolder.','Ensure lib contains sqlite-jdbc-3.50.3.0.jar, slf4j-api-2.0.17.jar and slf4j-nop-2.0.17.jar.','Do not delete data\crfms.db during the evaluator demonstration. Instead, make a backup first.','Read the exact dialog message; validation errors are designed to explain what needs correction.')

Break-Page
Add-Text '7  Closing Statement for the Evaluator' $wdHeading1
Add-Text 'CRFMS is a Java Swing desktop application with a local SQLite database. It separates administrator, faculty and student responsibilities; enforces registration, capacity, timetable and privacy rules; supports feedback and teaching-improvement workflows; and retains data across restarts. The FAQ assistant uses stored keyword matching and the quality guidance uses editable rule thresholds. Neither feature should be described as generative AI.' 0
Add-Text 'Project commands at a glance' $wdHeading1
Add-Command 'build.bat'
Add-Command 'test.bat'
Add-Command 'run.bat --demo'
Add-Command 'run.bat'
Add-Text 'Project files to mention if asked' $wdHeading2
Add-Bullets @('README.md - installation, features and credentials','docs\schema.sql - SQLite tables and constraints','docs\ACCEPTANCE_MATRIX.md - requirement traceability','src\main\java\crfms\CrfmsService.java - business rules and permissions','src\test\java\crfms\DatabaseIntegrationTest.java - integration coverage')

$doc.SaveAs2([string]$out, 16)
$doc.Close()
$word.Quit()
[Runtime.InteropServices.Marshal]::ReleaseComObject($sel) | Out-Null
[Runtime.InteropServices.Marshal]::ReleaseComObject($doc) | Out-Null
[Runtime.InteropServices.Marshal]::ReleaseComObject($word) | Out-Null
