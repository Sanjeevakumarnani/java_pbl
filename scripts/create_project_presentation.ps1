$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$out = Join-Path $root 'docs\CRFMS_Project_Presentation_6_Slides_Final.pptx'
$ppt = New-Object -ComObject PowerPoint.Application
$ppt.Visible = -1
$presentation = $ppt.Presentations.Add()
$presentation.PageSetup.SlideSize = 15 # widescreen
$ppLayoutBlank = 12; $ppSaveAsOpenXMLPresentation = 24
$white=16777215; $navy=4145203; $teal=10079487; $grey=15132390; $charcoal=3355443
function Add-Text($slide,[string]$text,[single]$left,[single]$top,[single]$width,[single]$height,[single]$size,[int]$color=$charcoal,[bool]$bold=$false) {
  $box=$slide.Shapes.AddTextbox(1,$left,$top,$width,$height)
  $box.TextFrame.TextRange.Text=$text
  $box.TextFrame.TextRange.Font.Name='Aptos'
  $box.TextFrame.TextRange.Font.Size=$size
  $box.TextFrame.TextRange.Font.Color.RGB=$color
  $box.TextFrame.TextRange.Font.Bold=if($bold){-1}else{0}
  $box.TextFrame.WordWrap=-1
  return $box
}
function Add-Slide([string]$title,[string[]]$body,[string]$footer) {
  $s=$presentation.Slides.Add($presentation.Slides.Count+1,$ppLayoutBlank)
  $s.FollowMasterBackground=0; $s.Background.Fill.ForeColor.RGB=$white
  $line=$s.Shapes.AddShape(1,0,0,960,10); $line.Fill.ForeColor.RGB=$teal; $line.Line.Visible=0
  Add-Text $s $title 54 38 850 55 30 $navy $true | Out-Null
  $joined=($body -join "`r`n`r`n")
  Add-Text $s $joined 75 130 810 330 19 $charcoal $false | Out-Null
  Add-Text $s $footer 54 500 850 20 10 $navy $false | Out-Null
  return $s
}

$s1=$presentation.Slides.Add(1,$ppLayoutBlank); $s1.FollowMasterBackground=0; $s1.Background.Fill.ForeColor.RGB=$navy
Add-Text $s1 'CRFMS' 68 110 780 70 40 $white $true | Out-Null
Add-Text $s1 'Course Registration and Feedback Management System' 70 190 780 50 25 $white $false | Out-Null
Add-Text $s1 'Java 17 desktop application with Swing and SQLite' 70 265 780 34 18 $teal $true | Out-Null
Add-Text $s1 'Project presentation  |  Problem, solution, architecture, features, testing and future scope' 70 430 800 24 13 $white $false | Out-Null

Add-Slide '1  Problem and Solution' @(
  'Problem: course registration, feedback, doubts and course communication often sit in separate manual processes. This creates delayed updates, capacity conflicts and weak feedback privacy.',
  'Solution: CRFMS brings these workflows into one role-based desktop system for administrators, faculty and students.',
  'Result: the system applies rules at the point of action, stores records in SQLite and keeps each role focused on its own responsibilities.'
) 'Core idea: one local application for the full student course lifecycle' | Out-Null

Add-Slide '2  Architecture and Application Flow' @(
  'Presentation layer: Java Swing screens provide separate workspaces for Admin, Faculty and Student.',
  'Service layer: CrfmsService enforces permissions, registration rules, privacy thresholds, validation and transactions.',
  'Data layer: SQLite stores users, courses, registrations, feedback, doubts, notifications, audits and settings.',
  'Flow: Login  >  role check  >  workspace action  >  service validation  >  database update  >  notification or report.'
) 'The user interface contains no direct SQL business logic' | Out-Null

Add-Slide '3  Main Features' @(
  'Administration: manage users, courses, settings, feedback periods, FAQs, audit events, exports and database backups.',
  'Registration: capacity, credit limit and timetable-clash checks. Full courses use a FIFO waitlist and promote the next eligible student after a drop.',
  'Feedback: simple anonymous course ratings plus ten-category teaching feedback with thresholds, strengths and editable improvement rules.',
  'Learning support: FAQ keyword matching answers known questions. Unmatched doubts become faculty tickets and can remain anonymous.',
  'Communication: announcements, resources and in-app notifications are shown only to eligible users.',
  'Unique value: privacy thresholds, anonymous doubts and transparent editable guidance rules protect students while keeping improvement actions explainable.'
) 'All key actions use validated service methods and prepared SQL statements' | Out-Null

Add-Slide '4  Testing and Demonstration Evidence' @(
  'Commands: build.bat compiles the project. test.bat runs automated security, schema and database-integration checks. run.bat --demo starts fictional demonstration data.',
  'Verified automated checks: SecurityTest passed 4 checks. Schema validation passed 14 checks. DatabaseIntegrationTest passed extended workflows.',
  'Demonstration sequence: Admin overview and course setup; Student registration and feedback; waitlist promotion; Faculty doubt answer and announcement; Admin reports, audit log and backup.',
  'Persistence: close and reopen the application to show that SQLite retains users, courses and registrations.'
) 'Use only fictional demo accounts and never display changed passwords' | Out-Null

Add-Slide '5  Future Scope and Conclusion' @(
  'Future scope: email or SMS notifications, dashboard charts, a web or mobile client, stronger report filters, database migration history and a larger FAQ knowledge base.',
  'Scalability path: retain the service rules and replace local SQLite with a managed database when multi-user network access is required.',
  'Conclusion: CRFMS delivers one complete desktop workflow for secure course registration, protected feedback, doubt handling and transparent teaching improvement.'
) 'Thank you  |  Questions and live demonstration' | Out-Null

$presentation.SaveAs($out,$ppSaveAsOpenXMLPresentation)
$presentation.Close(); $ppt.Quit()
[Runtime.InteropServices.Marshal]::ReleaseComObject($presentation) | Out-Null
[Runtime.InteropServices.Marshal]::ReleaseComObject($ppt) | Out-Null
