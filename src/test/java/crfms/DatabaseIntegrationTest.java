package crfms;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/** Integration checks run when the SQLite JDBC driver is available. */
public final class DatabaseIntegrationTest {
    private DatabaseIntegrationTest() { }
    public static void main(String[] args) throws Exception {
        Path root=Files.createTempDirectory("crfms-integration-");
        Database database=new Database(root);CrfmsService service=new CrfmsService(database);
        CrfmsService.User admin=service.login("admin","Admin@123");check(admin!=null,"default admin exists");service.changePassword(admin,"AdminNew123");admin=service.login("admin","AdminNew123");check(admin!=null,"admin password change persists");
        service.createUser(admin,"fac100","Faculty Test","faculty@test.example","FACULTY","Computer Science","EMP100",0,"Assistant Professor","Faculty123");
        service.createUser(admin,"fac200","Other Faculty","faculty2@test.example","FACULTY","Computer Science","EMP200",0,"Assistant Professor","FacultyTwo123");
        service.createUser(admin,"stud100","Student Test","student@test.example","STUDENT","Computer Science","ROLL100",3,"","Student123");
        service.createUser(admin,"stud101","Student Test Two","student2@test.example","STUDENT","Computer Science","ROLL101",3,"","Student123");
        service.createUser(admin,"stud102","Student Test Three","student3@test.example","STUDENT","Computer Science","ROLL102",3,"","Student123");
        CrfmsService.User firstOtherFaculty=service.login("fac200","FacultyTwo123");CrfmsService.User firstStudent=service.login("stud100","Student123");CrfmsService.User firstStudent2=service.login("stud101","Student123");CrfmsService.User firstStudent3=service.login("stud102","Student123");CrfmsService.User firstFaculty=service.login("fac100","Faculty123");
        boolean forcedChangeBlocked=false;try{service.register(firstStudent,"CS101");}catch(SecurityException ex){forcedChangeBlocked=true;}check(forcedChangeBlocked,"forced password change blocks normal actions");
        service.changePassword(firstOtherFaculty,"FacultyTwoNew123");service.changePassword(firstStudent,"StudentNew123");service.changePassword(firstStudent2,"StudentNew123");service.changePassword(firstStudent3,"StudentNew123");service.changePassword(firstFaculty,"FacultyNew123");
        CrfmsService.User otherFaculty=service.login("fac200","FacultyTwoNew123");CrfmsService.User student=service.login("stud100","StudentNew123");CrfmsService.User student2=service.login("stud101","StudentNew123");CrfmsService.User student3=service.login("stud102","StudentNew123");CrfmsService.User faculty=service.login("fac100","FacultyNew123");
        check(student!=null&&student2!=null&&student3!=null&&faculty!=null,"student and faculty authentication after forced password changes");
        service.saveCourse(admin,"CS101","Programming Fundamentals",3,1,3,"fac100","MON","09:00","10:00");
        service.saveCourse(admin,"CS102","Database Systems",3,5,3,"fac100","MON","09:30","10:30");
        service.saveCourse(admin,"CS103","Software Engineering",3,5,3,"fac100","TUE","09:00","10:00");
        service.saveCourse(admin,"CS104","Networks",3,5,3,"fac200","WED","09:00","10:00");
        boolean duplicateCourse=false;try{service.createCourse(admin,"CS103","Duplicate code",3,5,3,"fac100","THU","09:00","10:00");}catch(IllegalArgumentException ex){duplicateCourse=true;}check(duplicateCourse,"duplicate course code is rejected on create");
        service.deactivateCourse(admin,"CS104");
        check(service.courses(student,"",null,true).stream().noneMatch(c->c.code.equals("CS104")),"non-admin catalogue cannot expose inactive courses");
        check(service.courses(faculty,"",null,true).stream().noneMatch(c->c.code.equals("CS104")),"faculty course list is scoped to assigned courses");
        boolean forbiddenFeedback=false;try{service.feedbackSummary(faculty,"CS104");}catch(SecurityException ex){forbiddenFeedback=true;}check(forbiddenFeedback,"faculty cannot read another faculty member's feedback");
        check(service.register(student,"CS101").contains("successful"),"course registration");
        check(service.register(student2,"CS101").contains("waitlist"),"full course adds student to waitlist");
        check(service.drop(student,"CS101").contains("registered from waitlist"),"drop promotes eligible FIFO student");
        check(service.unreadNotificationCount(student2)==1,"waitlist promotion creates one unread notification");
        boolean clash=false;try{service.register(student2,"CS102");}catch(IllegalArgumentException ex){clash=ex.getMessage().contains("Timetable clash");}check(clash,"overlapping schedule is rejected");
        service.register(student,"CS103");service.register(student2,"CS103");service.register(student3,"CS103");
        service.setSetting(admin,"feedback_open","0");boolean closedFeedback=false;try{service.saveFeedback(student,"CS103",5,"Closed window");}catch(IllegalArgumentException ex){closedFeedback=ex.getMessage().contains("closed");}check(closedFeedback,"closed simple-feedback window rejects submission");service.setSetting(admin,"feedback_open","1");
        service.saveFeedback(student,"CS103",5,"Clear explanations");service.saveFeedback(student2,"CS103",4,"Good examples");
        check(service.feedbackSummary(admin,"CS103").get(0)[0].equals("SUPPRESSED"),"privacy threshold suppresses two responses");
        check(service.courseRating(student,"CS103")[0].equals("HIDDEN"),"catalogue rating hidden below threshold");
        service.saveFeedback(student3,"CS103",4,"Helpful class");check(service.feedbackSummary(admin,"CS103").get(0)[0].equals("VISIBLE"),"feedback becomes visible at threshold");
        check(service.courseRating(student,"CS103")[0].equals("VISIBLE"),"catalogue rating visible at threshold");
        Path csv=root.resolve("feedback.csv");service.exportCsv(admin,"feedback",csv);check(Files.readString(csv).contains("<3,SUPPRESSED"),"feedback CSV suppresses sub-threshold averages");
        service.addFaq(admin,"CS103","What is a variable?","A named storage location.");
        check(service.askDoubt(student,"CS103","What is a variable?",true).contains("named storage"),"FAQ bot answers from stored FAQ");
        List<String[]> doubtRows=service.doubts(faculty);check(doubtRows.stream().anyMatch(r->r[1].equals("Anonymous")),"anonymous doubt hides student identity");
        service.saveAnnouncement(faculty,"CS101","Assessment notice","The lab assessment is next week.");check(service.announcements(student2).size()==1,"registered student can see course announcement");check(service.announcements(student).isEmpty(),"dropped student cannot see that course announcement");
        check(service.unreadNotificationCount(student2)==2,"announcement adds a user-scoped unread notification");for(String[] n:service.notifications(student2))service.markNotificationRead(student2,Long.parseLong(n[0]));check(service.unreadNotificationCount(student2)==0,"marking notifications read returns unread count to zero");
        service.createFeedbackPeriod(admin,"Test period",LocalDate.now().minusDays(1).toString(),LocalDate.now().plusDays(5).toString(),3);
        long period=Long.parseLong(service.feedbackPeriods(admin).get(0)[0]);int[] ratings={2,4,4,4,2,4,4,4,4,5};service.submitTeachingFeedback(student,"CS103",period,ratings,"clarity,examples","Use more worked examples.");
        check(service.teachingInsights(faculty,"CS103",period).get(0)[0].equals("SUPPRESSED"),"teaching results remain hidden below threshold");
        int[] revisedRatings={2,4,4,4,1,4,4,4,4,5};service.submitTeachingFeedback(student,"CS103",period,revisedRatings,"examples,revised","Revised submission.");
        try(java.sql.Connection c=database.open();java.sql.PreparedStatement q=c.prepareStatement("SELECT COUNT(*) FROM teaching_feedback WHERE student_id='stud100' AND course_code='CS103' AND period_id=?")){q.setLong(1,period);try(java.sql.ResultSet r=q.executeQuery()){r.next();check(r.getInt(1)==1,"teaching feedback resubmission edits the existing record");}}
        int[] ratings2={3,4,4,4,3,4,4,4,4,5};service.submitTeachingFeedback(student2,"CS103",period,ratings2,"examples,practice","idiot suggestion should be held");
        int[] ratings3={2,4,4,4,2,4,4,4,4,5};service.submitTeachingFeedback(student3,"CS103",period,ratings3,"examples,practice","Add another example.");
        List<String[]> insights=service.teachingInsights(faculty,"CS103",period);
        check(insights.stream().anyMatch(row->row[0].equals("examples")&&row[2].startsWith("GUIDANCE:")),"rule-based guidance appears for a low category");
        check(insights.stream().anyMatch(row->row[0].startsWith("Strength:")&&row[2].contains("Maintain")),"strong categories produce positive guidance");
        check(service.heldSuggestions(admin).size()==1,"blocked suggestion is held for moderation");
        List<String[]> rules=service.suggestionRules(admin);String exampleRule=rules.stream().filter(row->row[2].equals("examples")).findFirst().orElseThrow()[0];
        String oldAdvice=rules.stream().filter(row->row[0].equals(exampleRule)).findFirst().orElseThrow()[5];service.updateSuggestionRule(admin,Long.parseLong(exampleRule),2.0,oldAdvice,true);
        check(service.teachingInsights(faculty,"CS103",period).stream().noneMatch(row->row[0].equals("examples")&&row[2].startsWith("GUIDANCE:")),"changing a rule threshold changes generated guidance");
        boolean wrongOwnerAction=false;try{service.createImprovementAction(otherFaculty,"CS103","Unauthorized action test");}catch(SecurityException ex){wrongOwnerAction=true;}check(wrongOwnerAction,"faculty cannot create an action for another faculty course");
        service.createFeedbackPeriod(admin,"Follow-up period",LocalDate.now().minusDays(1).toString(),LocalDate.now().plusDays(5).toString(),3);
        long laterPeriod=Long.parseLong(service.feedbackPeriods(admin).get(0)[0]);int[] improved={4,4,4,4,4,4,4,4,4,5};
        service.submitTeachingFeedback(student,"CS103",laterPeriod,improved,"examples,practice","");service.submitTeachingFeedback(student2,"CS103",laterPeriod,improved,"examples,practice","");service.submitTeachingFeedback(student3,"CS103",laterPeriod,improved,"examples,practice","");
        check(service.compareTeachingPeriods(faculty,"CS103",period,laterPeriod).stream().anyMatch(row->row[0].equals("examples")&&row[2].startsWith("+")),"period comparison reports positive change without exposing identities");
        check(service.registrations(student2).size()>=2,"registration records persist");
        for(int i=0;i<3;i++)service.login("stud100","WrongPass123");check(service.login("stud100","StudentNew123")==null,"three failed logins trigger five-minute lockout");
        System.out.println("DatabaseIntegrationTest: extended integration checks passed.");
        try(var paths=Files.walk(root)){paths.sorted(java.util.Comparator.reverseOrder()).forEach(p->{try{Files.deleteIfExists(p);}catch(Exception ignored){}});}
    }
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
