package crfms;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

/** Application service layer. All permissions and registration rules are enforced here. */
public final class CrfmsService {
    public static final class User {
        public final String id, name, email, role, department, extra, designation;
        public final int semester;
        public final boolean active, mustChange;
        User(String id, String name, String email, String role, String department, String extra, String designation, int semester, boolean active, boolean mustChange) {
            this.id=id; this.name=name; this.email=email; this.role=role; this.department=department; this.extra=extra; this.designation=designation; this.semester=semester; this.active=active; this.mustChange=mustChange;
        }
        @Override public String toString() { return name + " (" + id + ", " + role + ")"; }
    }
    public static final class Course {
        public final String code,title,facultyId,facultyName,day,start,end;
        public final int credits,capacity,semester,enrolled;
        public final boolean active;
        Course(String code,String title,int credits,int capacity,int semester,String facultyId,String facultyName,String day,String start,String end,int enrolled,boolean active) {
            this.code=code;this.title=title;this.credits=credits;this.capacity=capacity;this.semester=semester;this.facultyId=facultyId;this.facultyName=facultyName;this.day=day;this.start=start;this.end=end;this.enrolled=enrolled;this.active=active;
        }
        @Override public String toString() { return code+" — "+title+" ("+credits+" credits, seats "+Math.max(0,capacity-enrolled)+"/"+capacity+")"; }
    }
    private final Database db;
    private final Clock clock;
    private static final class LoginState { int failures; LocalDateTime lockedUntil; }
    private final Map<String, LoginState> loginStates = new HashMap<>();
    public CrfmsService(Database db) throws Exception { this(db, Clock.systemDefaultZone()); }
    public CrfmsService(Database db, Clock clock) throws Exception { this.db=db; this.clock = clock == null ? Clock.systemDefaultZone() : clock; seedInitialData(); }
    private void seedInitialData() throws Exception {
        try(Connection c=db.open()) {
            try(PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM users WHERE role='ADMIN'");ResultSet r=p.executeQuery()) {
                if(r.next() && r.getInt(1)==0) { String salt=Security.newSalt(); try(PreparedStatement i=c.prepareStatement("INSERT INTO users(id,name,email,role,department,roll_or_employee,salt,password_hash,active,must_change) VALUES(?,?,?,?,?,?,?,?,1,1)")) { i.setString(1,"admin");i.setString(2,"System Administrator");i.setString(3,"admin@crfms.local");i.setString(4,"ADMIN");i.setString(5,"Administration");i.setString(6,"ADMIN-001");i.setString(7,salt);i.setString(8,Security.hash("Admin@123",salt));i.executeUpdate(); } }
            }
            try(PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM suggestion_rules");ResultSet r=p.executeQuery()) {
                if(r.next() && r.getInt(1)==0) {
                    String[][] rules={
                        {"Low voice clarity","voice_clarity","LT","3.5","Students find it hard to hear you clearly. Speak a little louder and check from the back row that everyone can hear."},
                        {"Need more examples","examples","LT","3.5","Students want more examples. Add one or two worked examples for every new concept."},
                        {"Low concept clarity","concept_clarity","LT","3.5","Many students are not grasping the concepts. Re-explain difficult topics in simpler steps and recap at the end of each class."},
                        {"Low explanation","explanation","LT","3.5","Try breaking each topic into small steps and relate it to something familiar before using technical terms."},
                        {"Board usage","board_usage","LT","3.5","Write key points and diagrams on the board or slides while you explain, and make sure the writing is large enough to read."},
                        {"Doubt guidance","doubt_guidance","LT","3.5","Some students feel their doubts are not addressed. Keep five minutes at the end of class for questions or announce doubt-clearing times."},
                        {"Pace unsuitable","pace","LT","3.0","Students find the pace unsuitable. Pause after each concept and ask a quick question to check how the class is following."},
                        {"Low interaction","interaction","LT","3.5","Ask short questions during class and invite students to answer or explain to a neighbour."},
                        {"Punctuality","punctuality","LT","3.5","Try to start and finish on time; students notice delays."},
                        {"Materials","materials","LT","3.5","Share notes, slides or reference links before or after each class."},
                        {"Tag: Could not understand","could_not_understand","TAG","0.40","A large share of the class could not follow. Pause more often and use simpler examples."},
                        {"Tag: Voice not clear","voice_not_clear","TAG","0.30","Several students reported voice problems. Check microphone or seating, and speak louder."},
                        {"Tag: Need more examples","need_more_examples","TAG","0.30","Students asked explicitly for more examples. Add real-life or code examples to each topic."},
                        {"Tag: Doubts not addressed","doubts_not_addressed","TAG","0.30","Make doubt time visible: announce it, or use the Doubt Bot inbox regularly."}
                    };
                    for(String[] a:rules) try(PreparedStatement i=c.prepareStatement("INSERT INTO suggestion_rules(name,category,operator,threshold,advice) VALUES(?,?,?,?,?)")){i.setString(1,a[0]);i.setString(2,a[1]);i.setString(3,a[2]);i.setDouble(4,Double.parseDouble(a[3]));i.setString(5,a[4]);i.executeUpdate();}
                }
            }
        }
    }
    public synchronized User login(String id,String password) throws Exception {
        String key=id==null?"":id.trim().toLowerCase(Locale.ROOT);
        LoginState state=loginStates.computeIfAbsent(key,k->new LoginState());
        if(state.lockedUntil!=null&&LocalDateTime.now(clock).isBefore(state.lockedUntil)) return null;
        try(Connection c=db.open();PreparedStatement p=c.prepareStatement("SELECT * FROM users WHERE id=? COLLATE NOCASE AND active=1")) {
            p.setString(1,key);
            try(ResultSet r=p.executeQuery()) {
                if(r.next()&&Security.verify(password,r.getString("salt"),r.getString("password_hash"))) { state.failures=0;state.lockedUntil=null;return user(r); }
            }
        }
        state.failures++;
        if(state.failures>=3){state.lockedUntil=LocalDateTime.now(clock).plusMinutes(5);state.failures=0;}
        return null;
    }
    /** Verifies the current password before allowing an ordinary password change. */
    public void changeOwnPassword(User actor,String current,String next) throws Exception {
        if(actor==null||!actor.active||!Set.of("ADMIN","FACULTY","STUDENT").contains(actor.role))throw new SecurityException("Access denied.");
        try(Connection c=db.open();PreparedStatement p=c.prepareStatement("SELECT salt,password_hash FROM users WHERE id=? AND active=1")) {
            p.setString(1,actor.id);
            try(ResultSet r=p.executeQuery()) { if(!r.next()||!Security.verify(current,r.getString(1),r.getString(2)))throw new SecurityException("Current password is incorrect."); }
        }
        changePassword(actor,next);
    }
    public void changePassword(User actor,String next) throws Exception {
        if(actor==null||!actor.active||!Set.of("ADMIN","FACULTY","STUDENT").contains(actor.role))throw new SecurityException("Access denied."); validatePassword(next,actor.id); String salt=Security.newSalt();
        try(Connection c=db.open();PreparedStatement p=c.prepareStatement("UPDATE users SET salt=?,password_hash=?,must_change=0 WHERE id=?")){p.setString(1,salt);p.setString(2,Security.hash(next,salt));p.setString(3,actor.id);p.executeUpdate();audit(c,actor.id,"PASSWORD_CHANGED","Own password changed");}
    }
    public List<User> users(User actor,String role) throws Exception { require(actor,"ADMIN"); List<User> out=new ArrayList<>();String sql="SELECT * FROM users"+(role==null||role.equals("ALL")?"":" WHERE role=?")+" ORDER BY role,name";try(Connection c=db.open();PreparedStatement p=c.prepareStatement(sql)){if(role!=null&&!role.equals("ALL"))p.setString(1,role);try(ResultSet r=p.executeQuery()){while(r.next())out.add(user(r));}}return out; }
    public void createUser(User actor,String id,String name,String email,String role,String department,String extra,int semester,String designation,String password) throws Exception {
        require(actor,"ADMIN");id=required(id,"User ID",4,20);if(!id.matches("^[A-Za-z][A-Za-z0-9_]{3,19}$"))throw new IllegalArgumentException("User ID must start with a letter and contain 4-20 letters, digits or underscores.");
        name=required(name,"Name",2,60);if(!name.matches("[A-Za-z .\'-]{2,60}"))throw new IllegalArgumentException("Name may contain letters, spaces, dot, apostrophe, and hyphen only.");email=required(email,"Email",5,120);if(!email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))throw new IllegalArgumentException("Enter a valid email address.");
        if(!Set.of("STUDENT","FACULTY","ADMIN").contains(role))throw new IllegalArgumentException("Invalid role.");department=required(department,"Department",2,50);if(!department.matches("[A-Za-z0-9 &.-]{2,50}"))throw new IllegalArgumentException("Department contains unsupported characters.");validatePassword(password,id);
        if(role.equals("STUDENT")){extra=required(extra,"Roll number",3,20);if(!extra.matches("[A-Za-z0-9-]{3,20}"))throw new IllegalArgumentException("Roll number must use 3-20 letters, digits, or hyphens.");if(semester<1||semester>12)throw new IllegalArgumentException("Semester must be 1-12.");} else {extra=required(extra,"Employee ID",3,20);if(!extra.matches("[A-Za-z0-9-]{3,20}"))throw new IllegalArgumentException("Employee ID must use 3-20 letters, digits, or hyphens.");}
        String salt=Security.newSalt();try(Connection c=db.open();PreparedStatement p=c.prepareStatement("INSERT INTO users(id,name,email,role,department,roll_or_employee,semester,designation,salt,password_hash,active,must_change) VALUES(?,?,?,?,?,?,?,?,?,?,1,1)")){p.setString(1,id);p.setString(2,name);p.setString(3,email);p.setString(4,role);p.setString(5,department);p.setString(6,extra);if(role.equals("STUDENT"))p.setInt(7,semester);else p.setNull(7,java.sql.Types.INTEGER);p.setString(8,designation==null?"Faculty":designation);p.setString(9,salt);p.setString(10,Security.hash(password,salt));p.executeUpdate();audit(c,actor.id,"USER_CREATED",role+":"+id);}
    }
    public void setUserActive(User actor,String id,boolean active) throws Exception {
        require(actor,"ADMIN");try(Connection c=db.open()) {if(!active)try(PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM users WHERE role='ADMIN' AND active=1");ResultSet r=p.executeQuery()){if(r.next()&&r.getInt(1)<=1){try(PreparedStatement q=c.prepareStatement("SELECT role,active FROM users WHERE id=?")){q.setString(1,id);try(ResultSet x=q.executeQuery()){if(x.next()&&x.getString(1).equals("ADMIN")&&x.getInt(2)==1)throw new IllegalArgumentException("The last active administrator cannot be deactivated.");}}}}
            try(PreparedStatement p=c.prepareStatement("UPDATE users SET active=? WHERE id=?")){p.setInt(1,active?1:0);p.setString(2,id);if(p.executeUpdate()!=1)throw new IllegalArgumentException("User not found.");}audit(c,actor.id,"USER_STATUS",id+" active="+active);}
    }
    /** Resets an account password and returns a one-time temporary password to the administrator. */
    public String resetPassword(User actor,String targetId) throws Exception {
        require(actor,"ADMIN");
        String alphabet="ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";StringBuilder temp=new StringBuilder();java.security.SecureRandom random=new java.security.SecureRandom();
        for(int i=0;i<12;i++)temp.append(alphabet.charAt(random.nextInt(alphabet.length())));
        String salt=Security.newSalt();
        try(Connection c=db.open();PreparedStatement p=c.prepareStatement("UPDATE users SET salt=?,password_hash=?,must_change=1 WHERE id=?")) {
            p.setString(1,salt);p.setString(2,Security.hash(temp.toString(),salt));p.setString(3,targetId);
            if(p.executeUpdate()!=1)throw new IllegalArgumentException("User not found.");
            audit(c,actor.id,"PASSWORD_RESET","Password reset for "+targetId);
        }
        return temp.toString();
    }
    /** Updates editable profile fields without changing the immutable user identifier. */
    public void updateUser(User actor,String targetId,String name,String email,String department,String extra,int semester,String designation) throws Exception {
        require(actor,"ADMIN");name=required(name,"Name",2,60);if(!name.matches("[A-Za-z .\'-]{2,60}"))throw new IllegalArgumentException("Name may contain letters, spaces, dot, apostrophe, and hyphen only.");email=required(email,"Email",5,120);if(!email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))throw new IllegalArgumentException("Enter a valid email address.");department=required(department,"Department",2,50);if(!department.matches("[A-Za-z0-9 &.-]{2,50}"))throw new IllegalArgumentException("Department contains unsupported characters.");extra=required(extra,"Roll/employee ID",3,20);if(!extra.matches("[A-Za-z0-9-]{3,20}"))throw new IllegalArgumentException("Roll/employee ID must use 3-20 letters, digits, or hyphens.");
        try(Connection c=db.open();PreparedStatement p=c.prepareStatement("UPDATE users SET name=?,email=?,department=?,roll_or_employee=?,semester=CASE WHEN role='STUDENT' THEN ? ELSE semester END,designation=CASE WHEN role='FACULTY' THEN ? ELSE designation END WHERE id=?")) {
            p.setString(1,name);p.setString(2,email);p.setString(3,department);p.setString(4,extra);p.setInt(5,semester);p.setString(6,designation);p.setString(7,targetId);
            if(p.executeUpdate()!=1)throw new IllegalArgumentException("User not found.");audit(c,actor.id,"USER_UPDATED",targetId);
        }
    }
    public void setFeedbackPeriodOpen(User actor,long id,boolean open) throws Exception {require(actor,"ADMIN");try(Connection c=db.open();PreparedStatement p=c.prepareStatement("UPDATE feedback_periods SET is_open=? WHERE id=?")){p.setInt(1,open?1:0);p.setLong(2,id);if(p.executeUpdate()!=1)throw new IllegalArgumentException("Feedback period not found.");audit(c,actor.id,"FEEDBACK_PERIOD_STATUS",id+" open="+open);}}
    /** Adds clearly fictional demo accounts, courses, FAQs, and an open feedback period. */
    public void seedDemo() throws Exception {
        try(Connection c=db.open()) {
            try(PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM users WHERE id='fac101'");ResultSet r=p.executeQuery()) {if(r.next()&&r.getInt(1)==0){String salt=Security.newSalt();try(PreparedStatement i=c.prepareStatement("INSERT INTO users(id,name,email,role,department,roll_or_employee,designation,salt,password_hash,active,must_change) VALUES('fac101','Demo Faculty','faculty@example.com','FACULTY','Computer Science','EMP101','Assistant Professor',?,?,1,1)")){i.setString(1,salt);i.setString(2,Security.hash("Welcome123",salt));i.executeUpdate();}}}
            try(PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM users WHERE id='fac102'");ResultSet r=p.executeQuery()) {if(r.next()&&r.getInt(1)==0){String salt=Security.newSalt();try(PreparedStatement i=c.prepareStatement("INSERT INTO users(id,name,email,role,department,roll_or_employee,designation,salt,password_hash,active,must_change) VALUES('fac102','Demo Faculty 2','faculty2@example.com','FACULTY','Computer Science','EMP102','Assistant Professor',?,?,1,0)")){i.setString(1,salt);i.setString(2,Security.hash("Welcome123",salt));i.executeUpdate();}}}
            try(PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM users WHERE id='fac103'");ResultSet r=p.executeQuery()) {if(r.next()&&r.getInt(1)==0){String salt=Security.newSalt();try(PreparedStatement i=c.prepareStatement("INSERT INTO users(id,name,email,role,department,roll_or_employee,designation,salt,password_hash,active,must_change) VALUES('fac103','Demo Faculty 3','faculty3@example.com','FACULTY','Computer Science','EMP103','Assistant Professor',?,?,1,0)")){i.setString(1,salt);i.setString(2,Security.hash("Welcome123",salt));i.executeUpdate();}}}
            try(PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM users WHERE id='stu1001'");ResultSet r=p.executeQuery()) {if(r.next()&&r.getInt(1)==0){String salt=Security.newSalt();for(int k=1;k<=10;k++){String id="stu10"+String.format("%02d",k);String name="Demo Student "+k;String roll="R-10"+String.format("%02d",k);String email="student"+k+"@example.com";int sem=(k<=5)?3:4;try(PreparedStatement i=c.prepareStatement("INSERT OR IGNORE INTO users(id,name,email,role,department,roll_or_employee,semester,salt,password_hash,active,must_change) VALUES(?,?,?,?,?,?,?,?,?,1,0)")){i.setString(1,id);i.setString(2,name);i.setString(3,email);i.setString(4,"STUDENT");i.setString(5,"Computer Science");i.setString(6,roll);i.setInt(7,sem);i.setString(8,salt);i.setString(9,Security.hash("Welcome123",salt));i.executeUpdate();}}}}
            try(PreparedStatement p=c.prepareStatement("INSERT OR IGNORE INTO courses(code,title,credits,capacity,semester,faculty_id,day,start_time,end_time,active) VALUES('CS101','Programming Fundamentals',4,2,3,'fac101','MON','09:00','10:00',1),('CS102','Database Systems',3,40,3,'fac101','MON','10:00','11:00',1),('CS103','Data Structures',3,2,3,'fac102','TUE','09:00','10:00',1),('CS104','OOP',4,40,3,'fac102','TUE','10:00','11:00',1),('CS105','Algorithms',4,40,4,'fac103','WED','09:00','10:00',1),('CS106','Operating Systems',3,40,4,'fac103','WED','10:00','11:00',1),('CS107','Networks',3,40,3,'fac101','THU','09:00','10:00',0),('CS108','Software Engineering',4,40,4,'fac102','THU','10:00','11:00',1)")){p.executeUpdate();}
            try(PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM faq_entries");ResultSet r=p.executeQuery()){if(r.next()&&r.getInt(1)==0){try(PreparedStatement i=c.prepareStatement("INSERT INTO faq_entries(course_code,question,answer) VALUES('ALL','How do I register for a course?','Open the course catalogue, select a course, and use Register. Registration must be open and credit, capacity, and timetable rules must pass.'),('CS101','What is a variable?','A variable is a named storage location whose value can be used and changed while a program runs.'),('ALL','How do I reset password?','Contact admin to reset your password.'),('CS102','What is normalization?','Normalization is the process of organizing data to reduce redundancy.'),('ALL','When is feedback open?','Feedback periods are opened by admin.'),('CS103','What is stack?','Stack is LIFO data structure.'),('CS104','What is class?','A class is a blueprint for objects.'),('CS105','What is recursion?','Recursion is a function calling itself.'),('CS106','What is process?','A process is an instance of running program.'),('CS108','What is SDLC?','Software Development Life Cycle describes development phases.'),('ALL','How to view timetable?','Check Timetable menu for your weekly schedule.'),('CS101','What is loop?','Loops repeat statements based on condition.'),('CS102','What is primary key?','Unique identifier for a table row.'),('CS103','What is queue?','Queue is FIFO data structure.'),('CS104','What is inheritance?','Inheritance allows class to acquire properties of another.'),('CS105','What is sorting?','Arranging elements in order.'),('CS106','What is deadlock?','Deadlock is when processes wait for each other.'),('CS107','What is TCP/IP?','Network protocol suite.'),('CS108','What is testing?','Verifying software meets requirements.'),('ALL','How to ask doubt anonymously?','Use Ask a Doubt and select anonymous option.'),('ALL','How to join waitlist?','If course is full, you can join waitlist from catalogue.')")){i.executeUpdate();}}}
            try(PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM feedback_periods");ResultSet r=p.executeQuery()){if(r.next()&&r.getInt(1)==0){try(PreparedStatement i=c.prepareStatement("INSERT INTO feedback_periods(name,starts_on,ends_on,is_open,min_responses) VALUES('MID-TERM',?,?,1,3),('END-TERM',?,?,0,3)")){i.setString(1,LocalDate.now(clock).minusDays(1).toString());i.setString(2,LocalDate.now(clock).plusDays(30).toString());i.setString(3,LocalDate.now(clock).plusDays(31).toString());i.setString(4,LocalDate.now(clock).plusDays(60).toString());i.executeUpdate();}}}
        }
    }
    public List<Course> courses(User actor,String keyword,Integer semester,boolean includeInactive) throws Exception {
        require(actor,"ADMIN","FACULTY","STUDENT");includeInactive=includeInactive&&actor.role.equals("ADMIN");List<Course> out=new ArrayList<>();StringBuilder sql=new StringBuilder("SELECT c.*,u.name faculty_name,(SELECT COUNT(*) FROM registrations r WHERE r.course_code=c.code AND r.status='REGISTERED') enrolled FROM courses c JOIN users u ON u.id=c.faculty_id WHERE 1=1");List<Object> args=new ArrayList<>();if(!includeInactive)sql.append(" AND c.active=1");if(keyword!=null&&!keyword.isBlank()){sql.append(" AND (lower(c.code) LIKE ? OR lower(c.title) LIKE ? OR lower(u.name) LIKE ?)");String k="%"+keyword.toLowerCase(Locale.ROOT)+"%";args.add(k);args.add(k);args.add(k);}if(semester!=null){sql.append(" AND c.semester=?");args.add(semester);}if(actor.role.equals("FACULTY")){sql.append(" AND c.faculty_id=?");args.add(actor.id);}sql.append(" ORDER BY c.semester,c.code");try(Connection c=db.open();PreparedStatement p=c.prepareStatement(sql.toString())){for(int i=0;i<args.size();i++)p.setObject(i+1,args.get(i));try(ResultSet r=p.executeQuery()){while(r.next())out.add(course(r));}}return out;
    }
    public void createCourse(User actor,String code,String title,int credits,int capacity,int semester,String facultyId,String day,String start,String end) throws Exception {
        require(actor,"ADMIN");String normalized=code==null?"":code.trim().toUpperCase(Locale.ROOT);
        try(Connection c=db.open();PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM courses WHERE code=?")){p.setString(1,normalized);try(ResultSet r=p.executeQuery()){if(r.next()&&r.getInt(1)>0)throw new IllegalArgumentException("Course code already exists. Use Edit selected to update it.");}}
        saveCourse(actor,normalized,title,credits,capacity,semester,facultyId,day,start,end);
    }

    public void saveCourse(User actor,String code,String title,int credits,int capacity,int semester,String facultyId,String day,String start,String end) throws Exception {
        require(actor,"ADMIN");code=code.trim().toUpperCase(Locale.ROOT);if(!code.matches("^[A-Z]{2,4}[0-9]{3}$"))throw new IllegalArgumentException("Course code format: 2-4 uppercase letters followed by 3 digits (example CS101).");title=required(title,"Course title",3,100);if(credits<1||credits>6)throw new IllegalArgumentException("Credits must be 1-6.");if(capacity<1||capacity>200)throw new IllegalArgumentException("Capacity must be 1-200.");if(semester<1||semester>12)throw new IllegalArgumentException("Semester must be 1-12.");
        try(Connection c=db.open()){try(PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM users WHERE id=? AND role='FACULTY' AND active=1")){p.setString(1,facultyId);try(ResultSet r=p.executeQuery()){if(!r.next()||r.getInt(1)==0)throw new IllegalArgumentException("Choose an active faculty account.");}}
            if(day==null||day.isBlank()){day=null;start=null;end=null;}else{day=day.toUpperCase(Locale.ROOT);if(!Set.of("MON","TUE","WED","THU","FRI","SAT","SUN").contains(day))throw new IllegalArgumentException("Day must be MON through SUN.");try{if(LocalTime.parse(start).compareTo(LocalTime.parse(end))>=0)throw new IllegalArgumentException("End time must be after start time.");}catch(RuntimeException ex){throw new IllegalArgumentException("Times must use HH:mm and end after start.");}}
            int enrolled=0;try(PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM registrations WHERE course_code=? AND status='REGISTERED'")){p.setString(1,code);try(ResultSet r=p.executeQuery()){if(r.next())enrolled=r.getInt(1);}}if(capacity<enrolled)throw new IllegalArgumentException("Capacity cannot be below current enrolment ("+enrolled+").");
            try(PreparedStatement p=c.prepareStatement("INSERT INTO courses(code,title,credits,capacity,semester,faculty_id,day,start_time,end_time,active) VALUES(?,?,?,?,?,?,?,?,?,1) ON CONFLICT(code) DO UPDATE SET title=excluded.title,credits=excluded.credits,capacity=excluded.capacity,semester=excluded.semester,faculty_id=excluded.faculty_id,day=excluded.day,start_time=excluded.start_time,end_time=excluded.end_time,active=1")){p.setString(1,code);p.setString(2,title);p.setInt(3,credits);p.setInt(4,capacity);p.setInt(5,semester);p.setString(6,facultyId);p.setString(7,day);p.setString(8,start);p.setString(9,end);p.executeUpdate();}audit(c,actor.id,"COURSE_SAVED",code);}
    }
    public void deactivateCourse(User actor,String code) throws Exception {require(actor,"ADMIN");try(Connection c=db.open();PreparedStatement p=c.prepareStatement("UPDATE courses SET active=0 WHERE code=?")){p.setString(1,code);if(p.executeUpdate()==0)throw new IllegalArgumentException("Course not found.");} }
    public String register(User student,String code) throws Exception {
        require(student,"STUDENT");code=code.toUpperCase(Locale.ROOT);try(Connection c=db.open()){c.setAutoCommit(false);try {
            if(!setting(c,"registration_open").equals("1"))throw new IllegalArgumentException("Registration is currently closed.");Course course;try(PreparedStatement p=c.prepareStatement("SELECT c.*,u.name faculty_name,(SELECT COUNT(*) FROM registrations r WHERE r.course_code=c.code AND r.status='REGISTERED') enrolled FROM courses c JOIN users u ON u.id=c.faculty_id WHERE c.code=? AND c.active=1")){p.setString(1,code);try(ResultSet r=p.executeQuery()){if(!r.next())throw new IllegalArgumentException("Active course not found.");course=course(r);}}
            try(PreparedStatement p=c.prepareStatement("SELECT status FROM registrations WHERE student_id=? AND course_code=?")){p.setString(1,student.id);p.setString(2,code);try(ResultSet r=p.executeQuery()){if(r.next()&&r.getString(1).equals("REGISTERED"))throw new IllegalArgumentException("Already registered for this course.");}}
            int used=0;try(PreparedStatement p=c.prepareStatement("SELECT COALESCE(SUM(c.credits),0) FROM registrations r JOIN courses c ON c.code=r.course_code WHERE r.student_id=? AND r.status='REGISTERED'")){p.setString(1,student.id);try(ResultSet r=p.executeQuery()){if(r.next())used=r.getInt(1);}}int max=Integer.parseInt(setting(c,"max_credits"));if(used+course.credits>max)throw new IllegalArgumentException("Credit limit exceeded: current "+used+", course "+course.credits+", maximum "+max+".");
            try(PreparedStatement p=c.prepareStatement("SELECT c.code,c.day,c.start_time,c.end_time FROM registrations r JOIN courses c ON c.code=r.course_code WHERE r.student_id=? AND r.status='REGISTERED' AND c.day IS NOT NULL")){p.setString(1,student.id);try(ResultSet r=p.executeQuery()){while(r.next())if(course.day!=null&&course.day.equals(r.getString(2))&&overlap(course.start,course.end,r.getString(3),r.getString(4)))throw new IllegalArgumentException("Timetable clash with "+r.getString(1)+".");}}
            if(course.enrolled>=course.capacity){try(PreparedStatement p=c.prepareStatement("INSERT OR IGNORE INTO waitlist(student_id,course_code,created_at,status) VALUES(?,?,?,'WAITING')")){p.setString(1,student.id);p.setString(2,code);p.setString(3,LocalDateTime.now(clock).toString());p.executeUpdate();}c.commit();return "Course is full. You have been added to the FIFO waitlist.";}
            try(PreparedStatement p=c.prepareStatement("INSERT INTO registrations(student_id,course_code,status,registered_at,dropped_at) VALUES(?,?,'REGISTERED',?,NULL) ON CONFLICT(student_id,course_code) DO UPDATE SET status='REGISTERED',registered_at=excluded.registered_at,dropped_at=NULL")){p.setString(1,student.id);p.setString(2,code);p.setString(3,LocalDateTime.now(clock).toString());p.executeUpdate();}audit(c,student.id,"REGISTER",code);c.commit();return "Registration successful for "+code+".";
        }catch(Exception ex){c.rollback();throw ex;}finally{c.setAutoCommit(true);}}
    }
    public String drop(User student,String code) throws Exception {require(student,"STUDENT");try(Connection c=db.open()){c.setAutoCommit(false);try{if(!setting(c,"registration_open").equals("1"))throw new IllegalArgumentException("Dropping is disabled while registration is closed.");try(PreparedStatement p=c.prepareStatement("UPDATE registrations SET status='DROPPED',dropped_at=? WHERE student_id=? AND course_code=? AND status='REGISTERED'")){p.setString(1,LocalDateTime.now().toString());p.setString(2,student.id);p.setString(3,code);if(p.executeUpdate()==0)throw new IllegalArgumentException("No active registration found.");}String promoted=promoteWaitlist(c,code);audit(c,student.id,"DROP",code);c.commit();return "Registration dropped."+(promoted.isBlank()?"":" Waitlist promotion: "+promoted);}catch(Exception ex){c.rollback();throw ex;}finally{c.setAutoCommit(true);}} }
    private String promoteWaitlist(Connection c,String code)throws Exception {
        try(PreparedStatement q=c.prepareStatement("SELECT capacity,(SELECT COUNT(*) FROM registrations WHERE course_code=? AND status='REGISTERED') FROM courses WHERE code=? AND active=1")) {
            q.setString(1,code);q.setString(2,code);try(ResultSet r=q.executeQuery()){if(!r.next()||r.getInt(1)<=r.getInt(2))return "";}
        }
        try(PreparedStatement q=c.prepareStatement("SELECT id,student_id FROM waitlist WHERE course_code=? AND status='WAITING' ORDER BY id")) {
            q.setString(1,code);try(ResultSet r=q.executeQuery()) {
                while(r.next()) {
                    long waitId=r.getLong(1);String studentId=r.getString(2);
                    try(PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM registrations WHERE student_id=? AND course_code=? AND status='REGISTERED'")) {
                        p.setString(1,studentId);p.setString(2,code);try(ResultSet x=p.executeQuery()){if(x.next()&&x.getInt(1)>0){setWaitlistReason(c,waitId,"Already registered for this course");continue;}}
                    }
                    int currentCredits=0,courseCredits=0;
                    try(PreparedStatement p=c.prepareStatement("SELECT COALESCE(SUM(c.credits),0) FROM registrations r JOIN courses c ON c.code=r.course_code WHERE r.student_id=? AND r.status='REGISTERED'")) {
                        p.setString(1,studentId);try(ResultSet x=p.executeQuery()){if(x.next())currentCredits=x.getInt(1);}
                    }
                    try(PreparedStatement p=c.prepareStatement("SELECT credits,day,start_time,end_time FROM courses WHERE code=? AND active=1")) {
                        p.setString(1,code);try(ResultSet x=p.executeQuery()){if(!x.next()){setWaitlistReason(c,waitId,"Course is inactive");continue;}courseCredits=x.getInt(1);}
                    }
                    if(currentCredits+courseCredits>Integer.parseInt(setting(c,"max_credits"))){setWaitlistReason(c,waitId,"Credit limit would be exceeded");continue;}
                    try(PreparedStatement p=c.prepareStatement("SELECT active FROM users WHERE id=? AND role='STUDENT'")) {
                        p.setString(1,studentId);try(ResultSet x=p.executeQuery()){if(!x.next()||x.getInt(1)!=1){setWaitlistReason(c,waitId,"Student account is inactive");continue;}}
                    }
                    String day=null,start=null,end=null;
                    try(PreparedStatement p=c.prepareStatement("SELECT day,start_time,end_time FROM courses WHERE code=?")) {
                        p.setString(1,code);try(ResultSet x=p.executeQuery()){if(x.next()){day=x.getString(1);start=x.getString(2);end=x.getString(3);}}
                    }
                    String clashCourse=null;
                    if(day!=null)try(PreparedStatement p=c.prepareStatement("SELECT c.code,c.start_time,c.end_time FROM registrations r JOIN courses c ON c.code=r.course_code WHERE r.student_id=? AND r.status='REGISTERED' AND c.day=?")) {
                        p.setString(1,studentId);p.setString(2,day);try(ResultSet x=p.executeQuery()){while(x.next())if(overlap(start,end,x.getString(2),x.getString(3))){clashCourse=x.getString(1);break;}}
                    }
                    if(clashCourse!=null){setWaitlistReason(c,waitId,"Timetable clash with "+clashCourse);continue;}
                    try(PreparedStatement p=c.prepareStatement("INSERT INTO registrations(student_id,course_code,status,registered_at) VALUES(?,?,'REGISTERED',?) ON CONFLICT(student_id,course_code) DO UPDATE SET status='REGISTERED',registered_at=excluded.registered_at,dropped_at=NULL")) {
                        p.setString(1,studentId);p.setString(2,code);p.setString(3,LocalDateTime.now(clock).toString());p.executeUpdate();
                    }
                    try(PreparedStatement p=c.prepareStatement("UPDATE waitlist SET status='PROMOTED',reason='' WHERE id=?")){p.setLong(1,waitId);p.executeUpdate();}
                    notify(c,studentId,"Waitlist promotion","A seat became available in "+code+" and you were registered.");
                    return studentId+" registered from waitlist";
                }
            }
        }
        return "";
    }
    private void setWaitlistReason(Connection c,long id,String reason)throws SQLException {
        try(PreparedStatement p=c.prepareStatement("UPDATE waitlist SET reason=? WHERE id=? AND status='WAITING'")){p.setString(1,sanitize(reason));p.setLong(2,id);p.executeUpdate();}
    }
    /** Returns privacy-thresholded course rating data for the catalogue. */
    public String[] courseRating(User actor,String code) throws Exception {
        require(actor,"ADMIN","FACULTY","STUDENT");
        try(Connection c=db.open();PreparedStatement p=c.prepareStatement("SELECT COUNT(*),COALESCE(AVG(rating),0) FROM feedback WHERE course_code=?")) {
            p.setString(1,code.toUpperCase(Locale.ROOT));
            try(ResultSet r=p.executeQuery()) {
                r.next();int count=r.getInt(1);int min=Integer.parseInt(setting(c,"min_responses"));
                return count<min?new String[]{"HIDDEN","Fewer than "+min+" responses",""}:new String[]{"VISIBLE",String.valueOf(count),String.format(Locale.ROOT,"%.2f",r.getDouble(2))};
            }
        }
    }

    public List<String[]> registrations(User actor) throws Exception {require(actor,"ADMIN","FACULTY","STUDENT");String sql="SELECT r.student_id,u.name,r.course_code,c.title,c.credits,r.status,r.registered_at FROM registrations r JOIN users u ON u.id=r.student_id JOIN courses c ON c.code=r.course_code WHERE 1=1";if(actor.role.equals("STUDENT"))sql+=" AND r.student_id=?";if(actor.role.equals("FACULTY"))sql+=" AND c.faculty_id=?";sql+=" ORDER BY r.registered_at DESC";List<String[]> out=new ArrayList<>();try(Connection c=db.open();PreparedStatement p=c.prepareStatement(sql)){if(!actor.role.equals("ADMIN"))p.setString(1,actor.id);try(ResultSet r=p.executeQuery()){while(r.next())out.add(new String[]{r.getString(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5),r.getString(6),r.getString(7)});}}return out;}
    public void saveFeedback(User student,String code,int rating,String comment) throws Exception {require(student,"STUDENT");if(rating<1||rating>5)throw new IllegalArgumentException("Rating must be 1-5.");comment=sanitize(comment);if(comment.length()>300)throw new IllegalArgumentException("Comment must be at most 300 characters.");try(Connection c=db.open()){if(!setting(c,"feedback_open").equals("1"))throw new IllegalArgumentException("Course feedback is currently closed.");try(PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM registrations WHERE student_id=? AND course_code=? AND status='REGISTERED'")){p.setString(1,student.id);p.setString(2,code);try(ResultSet r=p.executeQuery()){if(!r.next()||r.getInt(1)==0)throw new IllegalArgumentException("Feedback is only allowed for registered courses.");}}try(PreparedStatement p=c.prepareStatement("INSERT INTO feedback(student_id,course_code,rating,comment,created_at) VALUES(?,?,?,?,?) ON CONFLICT(student_id,course_code) DO UPDATE SET rating=excluded.rating,comment=excluded.comment,created_at=excluded.created_at")){p.setString(1,student.id);p.setString(2,code);p.setInt(3,rating);p.setString(4,comment);p.setString(5,LocalDateTime.now().toString());p.executeUpdate();}audit(c,student.id,"FEEDBACK_SUBMITTED",code);}}
    public List<String[]> feedbackSummary(User actor,String code) throws Exception {
        require(actor,"ADMIN","FACULTY");
        Course course=findCourse(code);
        if(course==null) throw new IllegalArgumentException("Course not found.");
        if(actor.role.equals("FACULTY")&&!course.facultyId.equals(actor.id)) throw new SecurityException("You can only view feedback for your own courses.");
        List<String[]> out=new ArrayList<>();
        try(Connection c=db.open()) {
            try(PreparedStatement p=c.prepareStatement("SELECT COUNT(*),COALESCE(AVG(rating),0) FROM feedback WHERE course_code=?")) {
                p.setString(1,code);
                try(ResultSet r=p.executeQuery()) {
                    if(r.next()) {
                        int count=r.getInt(1);
                        if(count<Integer.parseInt(setting(c,"min_responses"))) out.add(new String[]{"SUPPRESSED",String.valueOf(count),""});
                        else out.add(new String[]{"VISIBLE",String.valueOf(count),String.format(Locale.ROOT,"%.2f",r.getDouble(2))});
                    }
                }
            }
            if(!out.isEmpty()&&out.get(0)[0].equals("VISIBLE")) {
                try(PreparedStatement p=c.prepareStatement("SELECT rating,comment,created_at FROM feedback WHERE course_code=? ORDER BY created_at DESC")) {
                    p.setString(1,code);
                    try(ResultSet r=p.executeQuery()) { while(r.next()) out.add(new String[]{String.valueOf(r.getInt(1)),r.getString(2),r.getString(3)}); }
                }
            }
        }
        return out;
    }
    public String askDoubt(User student,String code,String question,boolean anonymous)throws Exception {require(student,"STUDENT");question=required(question,"Doubt",5,500);try(Connection c=db.open()){try(PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM registrations WHERE student_id=? AND course_code=? AND status='REGISTERED'")){p.setString(1,student.id);p.setString(2,code);try(ResultSet r=p.executeQuery()){if(!r.next()||r.getInt(1)==0)throw new IllegalArgumentException("Ask doubts only for a currently registered course.");}}
        String answer=botAnswer(c,code,question);String status=answer.isBlank()?"OPEN":"ANSWERED_BY_BOT";try(PreparedStatement p=c.prepareStatement("INSERT INTO doubts(student_id,course_code,question,anonymous,status,answer,created_at,answered_at) VALUES(?,?,?,?,?,?,?,?)")){p.setString(1,student.id);p.setString(2,code);p.setString(3,sanitize(question));p.setInt(4,anonymous?1:0);p.setString(5,status);p.setString(6,answer);p.setString(7,LocalDateTime.now().toString());p.setString(8,answer.isBlank()?null:LocalDateTime.now().toString());p.executeUpdate();}if(answer.isBlank()){String faculty="";try(PreparedStatement p=c.prepareStatement("SELECT faculty_id FROM courses WHERE code=?")){p.setString(1,code);try(ResultSet r=p.executeQuery()){if(r.next())faculty=r.getString(1);}}notify(c,faculty,"Doubt escalated","A student has asked a question for "+code+". Ticket is open.");}return answer.isBlank()?"No matching FAQ found. Your doubt has been sent to the course faculty.":"FAQ assistant (keyword match): "+answer;}}
    private String botAnswer(Connection c,String code,String question)throws Exception {Set<String> tokens=words(question);String best="";int score=0;try(PreparedStatement p=c.prepareStatement("SELECT question,answer FROM faq_entries WHERE active=1 AND (course_code='ALL' OR course_code=?)")){p.setString(1,code);try(ResultSet r=p.executeQuery()){while(r.next()){Set<String> f=words(r.getString(1));int n=0;for(String t:tokens)if(f.contains(t))n++;int min=tokens.size()==1?1:2;if(n>=min&&n>score){score=n;best=r.getString(2);}}}}return score>=Integer.parseInt(setting(c,"bot_min_score"))?best:"";}
    public List<String[]> doubts(User actor) throws Exception {require(actor,"ADMIN","FACULTY","STUDENT");String sql="SELECT d.id,d.student_id,d.course_code,d.question,d.anonymous,d.status,d.answer,d.created_at,c.faculty_id FROM doubts d JOIN courses c ON c.code=d.course_code WHERE 1=1";if(actor.role.equals("STUDENT"))sql+=" AND d.student_id=?";if(actor.role.equals("FACULTY"))sql+=" AND c.faculty_id=?";sql+=" ORDER BY d.id DESC";List<String[]> out=new ArrayList<>();try(Connection c=db.open();PreparedStatement p=c.prepareStatement(sql)){if(!actor.role.equals("ADMIN"))p.setString(1,actor.id);try(ResultSet r=p.executeQuery()){while(r.next()){String status=r.getString(6);String created=r.getString(8);if(status.equals("OPEN")){try{if(LocalDateTime.parse(created).isBefore(LocalDateTime.now().minusDays(3)))status="OPEN - STALE (>3 DAYS)";}catch(RuntimeException ignored){/* Keep original status if legacy timestamp is malformed. */}}out.add(new String[]{String.valueOf(r.getLong(1)),r.getInt(5)==1?"Anonymous":(actor.role.equals("STUDENT")?"You":r.getString(2)),r.getString(3),r.getString(4),status,r.getString(7),created,r.getString(9)});}}}return out;}
    public void answerDoubt(User actor,long id,String answer)throws Exception {require(actor,"ADMIN","FACULTY");answer=required(answer,"Answer",2,1000);try(Connection c=db.open()){try(PreparedStatement p=c.prepareStatement("SELECT c.faculty_id,d.student_id,d.course_code FROM doubts d JOIN courses c ON c.code=d.course_code WHERE d.id=? AND d.status='OPEN'")){p.setLong(1,id);try(ResultSet r=p.executeQuery()){if(!r.next())throw new IllegalArgumentException("Open doubt not found.");if(actor.role.equals("FACULTY")&&!actor.id.equals(r.getString(1)))throw new SecurityException("You can only answer doubts for your own courses.");String student=r.getString(2),code=r.getString(3);try(PreparedStatement u=c.prepareStatement("UPDATE doubts SET answer=?,status='ANSWERED',answered_at=? WHERE id=?")){u.setString(1,sanitize(answer));u.setString(2,LocalDateTime.now().toString());u.setLong(3,id);u.executeUpdate();}notify(c,student,"Doubt answered","Your question for "+code+" has been answered.");}}}}
    /** Escalates an unhelpful FAQ answer into a faculty ticket owned by the signed-in student. */
    public void escalateDoubt(User student,long id)throws Exception {
        require(student,"STUDENT");
        try(Connection c=db.open();PreparedStatement p=c.prepareStatement("SELECT d.course_code,c.faculty_id,d.status FROM doubts d JOIN courses c ON c.code=d.course_code WHERE d.id=? AND d.student_id=?")) {
            p.setLong(1,id);p.setString(2,student.id);
            try(ResultSet r=p.executeQuery()) {
                if(!r.next())throw new IllegalArgumentException("Your doubt ticket was not found.");
                if(!r.getString(3).equals("ANSWERED_BY_BOT"))throw new IllegalArgumentException("Only a bot-answered doubt can be escalated.");
                String code=r.getString(1),faculty=r.getString(2);
                try(PreparedStatement u=c.prepareStatement("UPDATE doubts SET status='OPEN' WHERE id=?")){u.setLong(1,id);u.executeUpdate();}
                notify(c,faculty,"Doubt needs faculty review","A student marked the FAQ answer unhelpful for "+code+". Ticket "+id+" is open.");
                audit(c,student.id,"DOUBT_ESCALATED",String.valueOf(id));
            }
        }
    }
    /** Lists moderated suggestions without exposing the submitting student. */
    public List<String[]> heldSuggestions(User actor)throws Exception {
        require(actor,"ADMIN");List<String[]> out=new ArrayList<>();
        try(Connection c=db.open();PreparedStatement p=c.prepareStatement("SELECT id,course_code,period_id,suggestion,moderation_status,created_at FROM teaching_feedback WHERE moderation_status='HELD' ORDER BY id");ResultSet r=p.executeQuery()) {
            while(r.next())out.add(new String[]{r.getString(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5),r.getString(6)});
        }
        return out;
    }
    public void moderateSuggestion(User actor,long id,boolean approve)throws Exception {
        require(actor,"ADMIN");String status=approve?"VISIBLE":"REJECTED";
        try(Connection c=db.open();PreparedStatement p=c.prepareStatement("UPDATE teaching_feedback SET moderation_status=? WHERE id=? AND moderation_status='HELD'")) {
            p.setString(1,status);p.setLong(2,id);if(p.executeUpdate()!=1)throw new IllegalArgumentException("Held suggestion not found.");audit(c,actor.id,"SUGGESTION_MODERATED",id+" -> "+status);
        }
    }
    public List<String[]> improvementActions(User actor)throws Exception {
        require(actor,"ADMIN","FACULTY");String sql="SELECT a.id,a.course_code,a.faculty_id,u.name,a.advice,a.status,a.created_at FROM improvement_actions a JOIN users u ON u.id=a.faculty_id";
        if(actor.role.equals("FACULTY"))sql+=" WHERE a.faculty_id=?";sql+=" ORDER BY a.id DESC";List<String[]> out=new ArrayList<>();
        try(Connection c=db.open();PreparedStatement p=c.prepareStatement(sql)){if(actor.role.equals("FACULTY"))p.setString(1,actor.id);try(ResultSet r=p.executeQuery()){while(r.next())out.add(new String[]{r.getString(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5),r.getString(6),r.getString(7)});}}return out;
    }
    public void createImprovementAction(User actor,String code,String advice)throws Exception {
        require(actor,"ADMIN","FACULTY");Course course=findCourse(code);if(course==null)throw new IllegalArgumentException("Course not found.");if(actor.role.equals("FACULTY")&&!course.facultyId.equals(actor.id))throw new SecurityException("You can create actions only for your own courses.");
        advice=required(advice,"Improvement action",5,500);try(Connection c=db.open();PreparedStatement p=c.prepareStatement("INSERT INTO improvement_actions(course_code,faculty_id,advice,status,created_at,updated_at) VALUES(?,?,?,'OPEN',?,?)")){p.setString(1,code);p.setString(2,course.facultyId);p.setString(3,advice);p.setString(4,LocalDateTime.now().toString());p.setString(5,LocalDateTime.now().toString());p.executeUpdate();}
    }
    public void updateImprovementAction(User actor,long id,String status)throws Exception {
        require(actor,"ADMIN","FACULTY");if(!Set.of("OPEN","IN_PROGRESS","COMPLETED","CANCELLED").contains(status))throw new IllegalArgumentException("Invalid action status.");
        try(Connection c=db.open()){String sql="UPDATE improvement_actions SET status=?,updated_at=? WHERE id=?"+(actor.role.equals("FACULTY")?" AND faculty_id=?":"");try(PreparedStatement p=c.prepareStatement(sql)){p.setString(1,status);p.setString(2,LocalDateTime.now().toString());p.setLong(3,id);if(actor.role.equals("FACULTY"))p.setString(4,actor.id);if(p.executeUpdate()!=1)throw new SecurityException("Action not found or not assigned to you.");}audit(c,actor.id,"IMPROVEMENT_ACTION_UPDATED",id+" -> "+status);}
    }
    public List<String[]> faq(User actor) throws Exception {require(actor,"ADMIN","FACULTY");List<String[]> out=new ArrayList<>();try(Connection c=db.open();PreparedStatement p=c.prepareStatement("SELECT id,course_code,question,answer,active FROM faq_entries ORDER BY course_code,id");ResultSet r=p.executeQuery()){while(r.next())out.add(new String[]{r.getString(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5)});}return out;}
    public void addFaq(User actor,String course,String question,String answer)throws Exception {require(actor,"ADMIN");course=course.isBlank()?"ALL":course.toUpperCase(Locale.ROOT);question=required(question,"FAQ question",5,300);answer=required(answer,"FAQ answer",2,1000);try(Connection c=db.open();PreparedStatement p=c.prepareStatement("INSERT INTO faq_entries(course_code,question,answer) VALUES(?,?,?)")){p.setString(1,course);p.setString(2,sanitize(question));p.setString(3,sanitize(answer));p.executeUpdate();audit(c,actor.id,"FAQ_ADDED",course);}}
    public List<String[]> feedbackPeriods(User actor)throws Exception {require(actor,"ADMIN","FACULTY","STUDENT");List<String[]> out=new ArrayList<>();try(Connection c=db.open();PreparedStatement p=c.prepareStatement("SELECT id,name,starts_on,ends_on,is_open,min_responses FROM feedback_periods ORDER BY id DESC");ResultSet r=p.executeQuery()){while(r.next())out.add(new String[]{r.getString(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5),r.getString(6)});}return out;}
    public void createFeedbackPeriod(User actor,String name,String starts,String ends,int min)throws Exception {require(actor,"ADMIN");LocalDate a=LocalDate.parse(starts),b=LocalDate.parse(ends);if(b.isBefore(a))throw new IllegalArgumentException("End date must be on or after start date.");if(min<1)throw new IllegalArgumentException("Minimum responses must be at least 1.");try(Connection c=db.open();PreparedStatement p=c.prepareStatement("INSERT INTO feedback_periods(name,starts_on,ends_on,is_open,min_responses) VALUES(?,?,?,1,?)")){p.setString(1,required(name,"Period name",2,80));p.setString(2,a.toString());p.setString(3,b.toString());p.setInt(4,min);p.executeUpdate();}}
    public void submitTeachingFeedback(User student,String code,long period,int[] ratings,String tags,String suggestion)throws Exception {require(student,"STUDENT");if(ratings.length!=10)throw new IllegalArgumentException("Ten rating categories are required.");int rated=0;for(int v:ratings){if(v!=0&&(v<1||v>5))throw new IllegalArgumentException("Each category must be 1-5 or skipped.");if(v>0)rated++;}if(rated<4)throw new IllegalArgumentException("Rate at least four categories.");tags=tags==null?"":tags.trim();if(!tags.isBlank()&&tags.split(",").length>5)throw new IllegalArgumentException("Use at most five comma-separated tags.");suggestion=sanitize(suggestion==null?"":suggestion);if(suggestion.length()>300)throw new IllegalArgumentException("Suggestion must be at most 300 characters.");try(Connection c=db.open()){try(PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM registrations WHERE student_id=? AND course_code=? AND status='REGISTERED'")){p.setString(1,student.id);p.setString(2,code);try(ResultSet r=p.executeQuery()){if(!r.next()||r.getInt(1)==0)throw new IllegalArgumentException("You must be registered in the course.");}}try(PreparedStatement p=c.prepareStatement("SELECT is_open,starts_on,ends_on FROM feedback_periods WHERE id=?")){p.setLong(1,period);try(ResultSet r=p.executeQuery()){if(!r.next()||r.getInt(1)!=1||LocalDate.now().isBefore(LocalDate.parse(r.getString(2)))||LocalDate.now().isAfter(LocalDate.parse(r.getString(3))))throw new IllegalArgumentException("Feedback period is not open.");}}
        String[] blocked={"idiot","stupid","hate you","kill you"};String moderation="VISIBLE";String lower=suggestion.toLowerCase(Locale.ROOT);for(String w:blocked)if(lower.contains(w))moderation="HELD";
        String sql="INSERT INTO teaching_feedback(student_id,course_code,period_id,clarity,preparation,pace,engagement,examples,availability,fairness,materials,feedback_quality,overall,tags,suggestion,moderation_status,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT(student_id,course_code,period_id) DO UPDATE SET clarity=excluded.clarity,preparation=excluded.preparation,pace=excluded.pace,engagement=excluded.engagement,examples=excluded.examples,availability=excluded.availability,fairness=excluded.fairness,materials=excluded.materials,feedback_quality=excluded.feedback_quality,overall=excluded.overall,tags=excluded.tags,suggestion=excluded.suggestion,moderation_status=excluded.moderation_status,created_at=excluded.created_at";try(PreparedStatement p=c.prepareStatement(sql)){p.setString(1,student.id);p.setString(2,code);p.setLong(3,period);for(int i=0;i<10;i++){if(ratings[i]==0)p.setNull(4+i,java.sql.Types.INTEGER);else p.setInt(4+i,ratings[i]);}p.setString(14,sanitize(tags));p.setString(15,suggestion);p.setString(16,moderation);p.setString(17,LocalDateTime.now().toString());p.executeUpdate();}audit(c,student.id,"TEACHING_FEEDBACK",code+" period="+period);}}
    public List<String[]> teachingInsights(User actor,String code,long period)throws Exception {require(actor,"ADMIN","FACULTY");Course course=findCourse(code);if(course==null)throw new IllegalArgumentException("Course not found.");if(actor.role.equals("FACULTY")&&!actor.id.equals(course.facultyId))throw new SecurityException("Not your course.");int min=3;try(Connection c=db.open();PreparedStatement p=c.prepareStatement("SELECT min_responses FROM feedback_periods WHERE id=?")){p.setLong(1,period);try(ResultSet r=p.executeQuery()){if(!r.next())throw new IllegalArgumentException("Period not found.");min=r.getInt(1);}}List<String[]> out=new ArrayList<>();try(Connection c=db.open();PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM teaching_feedback WHERE course_code=? AND period_id=?")){p.setString(1,code);p.setLong(2,period);try(ResultSet r=p.executeQuery()){r.next();if(r.getInt(1)<min){out.add(new String[]{"SUPPRESSED","Responses below privacy threshold: "+r.getInt(1)+" / "+min,""});return out;}}}
        String[] cats={"examples","clarity","preparation","pace","engagement","availability","fairness","materials","feedback_quality","overall"};try(Connection c=db.open()){for(String cat:cats){try(PreparedStatement p=c.prepareStatement("SELECT AVG("+cat+") FROM teaching_feedback WHERE course_code=? AND period_id=?")){p.setString(1,code);p.setLong(2,period);try(ResultSet r=p.executeQuery()){if(r.next()&&r.getObject(1)!=null){double avg=r.getDouble(1);out.add(new String[]{cat,String.format(Locale.ROOT,"%.2f",avg),guidanceFor(c,cat,avg)});if(avg>=4.5)out.add(new String[]{"Strength: "+cat,String.format(Locale.ROOT,"%.2f",avg),"GUIDANCE: Maintain this practice and consider sharing it with colleagues."});}}}}if(actor.role.equals("ADMIN")||actor.role.equals("FACULTY")){try(PreparedStatement p=c.prepareStatement("SELECT suggestion,moderation_status FROM teaching_feedback WHERE course_code=? AND period_id=? AND suggestion<>'' AND moderation_status='VISIBLE'")){p.setString(1,code);p.setLong(2,period);try(ResultSet r=p.executeQuery()){while(r.next())out.add(new String[]{"Anonymous suggestion",r.getString(1),"GUIDANCE ONLY"});}}}}return out;
    }
    public List<String[]> suggestionRules(User actor) throws Exception {
        require(actor,"ADMIN");List<String[]> out=new ArrayList<>();
        try(Connection c=db.open();PreparedStatement p=c.prepareStatement("SELECT id,name,category,operator,threshold,advice,active FROM suggestion_rules ORDER BY CASE category WHEN 'examples' THEN 1 WHEN 'clarity' THEN 2 WHEN 'preparation' THEN 3 WHEN 'pace' THEN 4 WHEN 'engagement' THEN 5 ELSE 6 END,id");ResultSet r=p.executeQuery()) {
            while(r.next())out.add(new String[]{r.getString(1),r.getString(2),r.getString(3),r.getString(4),String.valueOf(r.getDouble(5)),r.getString(6),r.getString(7)});
        }return out;
    }
    public void updateSuggestionRule(User actor,long id,double threshold,String advice,boolean active) throws Exception {
        require(actor,"ADMIN");if(!Double.isFinite(threshold)||threshold<1||threshold>5)throw new IllegalArgumentException("Rule threshold must be between 1 and 5.");
        advice=required(advice,"Guidance text",5,500);
        try(Connection c=db.open();PreparedStatement p=c.prepareStatement("UPDATE suggestion_rules SET threshold=?,advice=?,active=? WHERE id=?")) {
            p.setDouble(1,threshold);p.setString(2,advice);p.setInt(3,active?1:0);p.setLong(4,id);if(p.executeUpdate()!=1)throw new IllegalArgumentException("Suggestion rule not found.");audit(c,actor.id,"SUGGESTION_RULE_UPDATED",id+" threshold="+threshold+" active="+active);
        }
    }
    private String guidanceFor(Connection c,String category,double average)throws Exception {
        try(PreparedStatement p=c.prepareStatement("SELECT operator,threshold,advice FROM suggestion_rules WHERE active=1 AND category=? ORDER BY id LIMIT 1")) {
            p.setString(1,category);try(ResultSet r=p.executeQuery()){if(r.next()){String op=r.getString(1);double threshold=r.getDouble(2);boolean match=op.equals("LT")?average<threshold:op.equals("LTE")?average<=threshold:op.equals("GT")?average>threshold:op.equals("GTE")&&average>=threshold;return match?"GUIDANCE: "+r.getString(3):"";}}
        }return average<3.5?"GUIDANCE: Review this area with students and agree on one practical improvement.":"";
    }

    /** Compares two feedback periods only after each independently meets its privacy threshold. */
    public List<String[]> compareTeachingPeriods(User actor,String code,long earlierPeriod,long laterPeriod)throws Exception {
        require(actor,"ADMIN","FACULTY");Course course=findCourse(code.toUpperCase(Locale.ROOT));
        if(course==null)throw new IllegalArgumentException("Course not found.");
        if(actor.role.equals("FACULTY")&&!actor.id.equals(course.facultyId))throw new SecurityException("Not your course.");
        if(earlierPeriod==laterPeriod)throw new IllegalArgumentException("Choose two different feedback periods.");
        String[] cats={"examples","clarity","preparation","pace","engagement","availability","fairness","materials","feedback_quality","overall"};
        List<String[]> out=new ArrayList<>();
        try(Connection c=db.open()) {
            for(long period:new long[]{earlierPeriod,laterPeriod}) {
                int minimum;try(PreparedStatement p=c.prepareStatement("SELECT min_responses FROM feedback_periods WHERE id=?")){p.setLong(1,period);try(ResultSet r=p.executeQuery()){if(!r.next())throw new IllegalArgumentException("Feedback period not found: "+period);minimum=r.getInt(1);}}
                try(PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM teaching_feedback WHERE course_code=? AND period_id=?")){p.setString(1,code.toUpperCase(Locale.ROOT));p.setLong(2,period);try(ResultSet r=p.executeQuery()){r.next();if(r.getInt(1)<minimum){out.add(new String[]{"SUPPRESSED","Period "+period+" has fewer than "+minimum+" responses",""});return out;}}}
            }
            for(String category:cats) {
                Double before,after;
                try(PreparedStatement p=c.prepareStatement("SELECT AVG("+category+") FROM teaching_feedback WHERE course_code=? AND period_id=?")){p.setString(1,code.toUpperCase(Locale.ROOT));p.setLong(2,earlierPeriod);try(ResultSet r=p.executeQuery()){r.next();before=r.getObject(1)==null?null:r.getDouble(1);}}
                try(PreparedStatement p=c.prepareStatement("SELECT AVG("+category+") FROM teaching_feedback WHERE course_code=? AND period_id=?")){p.setString(1,code.toUpperCase(Locale.ROOT));p.setLong(2,laterPeriod);try(ResultSet r=p.executeQuery()){r.next();after=r.getObject(1)==null?null:r.getDouble(1);}}
                if(before==null||after==null)out.add(new String[]{category,before==null?"—":String.format(Locale.ROOT,"%.2f",before)+" → —","Not enough ratings in this category"});
                else out.add(new String[]{category,String.format(Locale.ROOT,"%.2f → %.2f",before,after),String.format(Locale.ROOT,"%+.2f",after-before)});
            }
        }return out;
    }

    private String adviceFor(Connection c,String category)throws Exception {try(PreparedStatement p=c.prepareStatement("SELECT advice FROM suggestion_rules WHERE active=1 AND category=? LIMIT 1")){p.setString(1,category);try(ResultSet r=p.executeQuery()){if(r.next())return r.getString(1);}}return "Review this area with students and agree on one practical improvement.";}
    public void saveAnnouncement(User actor,String code,String title,String body)throws Exception {require(actor,"ADMIN","FACULTY");title=required(title,"Title",2,100);body=required(body,"Announcement",2,500);try(Connection c=db.open()){Course course=findCourse(code);if(course==null)throw new IllegalArgumentException("Course not found.");if(actor.role.equals("FACULTY")&&!actor.id.equals(course.facultyId))throw new SecurityException("Not your course.");try(PreparedStatement p=c.prepareStatement("INSERT INTO announcements(faculty_id,course_code,title,body,created_at) VALUES(?,?,?,?,?)")){p.setString(1,course.facultyId);p.setString(2,code);p.setString(3,sanitize(title));p.setString(4,sanitize(body));p.setString(5,LocalDateTime.now().toString());p.executeUpdate();}try(PreparedStatement p=c.prepareStatement("SELECT student_id FROM registrations WHERE course_code=? AND status='REGISTERED'")){p.setString(1,code);try(ResultSet r=p.executeQuery()){while(r.next())notify(c,r.getString(1),title,body);}}}}
    public List<String[]> announcements(User actor)throws Exception {require(actor,"ADMIN","FACULTY","STUDENT");String sql="SELECT a.id,a.course_code,a.title,a.body,a.created_at FROM announcements a WHERE a.active=1";if(actor.role.equals("STUDENT"))sql+=" AND EXISTS(SELECT 1 FROM registrations r WHERE r.course_code=a.course_code AND r.student_id=? AND r.status='REGISTERED')";if(actor.role.equals("FACULTY"))sql+=" AND a.faculty_id=?";sql+=" ORDER BY a.id DESC";List<String[]> out=new ArrayList<>();try(Connection c=db.open();PreparedStatement p=c.prepareStatement(sql)){if(!actor.role.equals("ADMIN"))p.setString(1,actor.id);try(ResultSet r=p.executeQuery()){while(r.next())out.add(new String[]{r.getString(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5)});}}return out;}
    public void saveResource(User actor,String code,String title,String url,String description)throws Exception {require(actor,"FACULTY","ADMIN");if(!url.matches("^https?://.+"))throw new IllegalArgumentException("Resource URL must begin with http:// or https://.");Course course=findCourse(code);if(course==null)throw new IllegalArgumentException("Course not found.");if(actor.role.equals("FACULTY")&&!actor.id.equals(course.facultyId))throw new SecurityException("Not your course.");try(Connection c=db.open();PreparedStatement p=c.prepareStatement("INSERT INTO resources(faculty_id,course_code,title,url,description,created_at) VALUES(?,?,?,?,?,?)")){p.setString(1,course.facultyId);p.setString(2,code);p.setString(3,required(title,"Title",2,100));p.setString(4,url);p.setString(5,sanitize(description));p.setString(6,LocalDateTime.now().toString());p.executeUpdate();}}
    public List<String[]> resources(User actor,String code)throws Exception {require(actor,"ADMIN","FACULTY","STUDENT");String sql="SELECT r.id,r.course_code,r.title,r.url,r.description FROM resources r WHERE 1=1";if(actor.role.equals("STUDENT"))sql+=" AND EXISTS(SELECT 1 FROM registrations x WHERE x.course_code=r.course_code AND x.student_id=? AND x.status='REGISTERED')";if(actor.role.equals("FACULTY"))sql+=" AND r.faculty_id=?";if(code!=null&&!code.isBlank())sql+=" AND r.course_code=?";sql+=" ORDER BY r.id DESC";List<String[]> out=new ArrayList<>();try(Connection c=db.open();PreparedStatement p=c.prepareStatement(sql)){int i=1;if(!actor.role.equals("ADMIN"))p.setString(i++,actor.id);if(code!=null&&!code.isBlank())p.setString(i,code);try(ResultSet r=p.executeQuery()){while(r.next())out.add(new String[]{r.getString(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5)});}}return out;}
    public List<String[]> notifications(User actor)throws Exception {require(actor,"ADMIN","FACULTY","STUDENT");List<String[]> out=new ArrayList<>();try(Connection c=db.open();PreparedStatement p=c.prepareStatement("SELECT id,title,body,created_at,is_read FROM notifications WHERE user_id=? ORDER BY id DESC")){p.setString(1,actor.id);try(ResultSet r=p.executeQuery()){while(r.next())out.add(new String[]{r.getString(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5)});}}return out;}
    public int unreadNotificationCount(User actor)throws Exception {
        require(actor,"ADMIN","FACULTY","STUDENT");
        try(Connection c=db.open();PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM notifications WHERE user_id=? AND is_read=0")){p.setString(1,actor.id);try(ResultSet r=p.executeQuery()){r.next();return r.getInt(1);}}
    }
    public void markNotificationRead(User actor,long id)throws Exception {require(actor,"ADMIN","FACULTY","STUDENT");try(Connection c=db.open();PreparedStatement p=c.prepareStatement("UPDATE notifications SET is_read=1 WHERE id=? AND user_id=?")){p.setLong(1,id);p.setString(2,actor.id);p.executeUpdate();}}
    public String setting(String key)throws Exception {try(Connection c=db.open()){return setting(c,key);}}
    public void setSetting(User actor,String key,String value)throws Exception {require(actor,"ADMIN");if(!Set.of("registration_open","feedback_open","max_credits","bot_min_score","min_responses","alert_threshold").contains(key))throw new IllegalArgumentException("Unknown setting.");if((key.equals("registration_open")||key.equals("feedback_open"))&&!Set.of("0","1").contains(value))throw new IllegalArgumentException("Use 1 for open or 0 for closed.");if(!key.equals("registration_open")&&!key.equals("feedback_open")){double n=Double.parseDouble(value);if(n<=0||n>1000)throw new IllegalArgumentException("Setting must be a positive number no greater than 1000.");if(!key.equals("alert_threshold")&&n!=Math.rint(n))throw new IllegalArgumentException("This setting must be a whole number.");if(key.equals("alert_threshold")&&n>5)throw new IllegalArgumentException("Alert threshold must be at most 5.");}try(Connection c=db.open();PreparedStatement p=c.prepareStatement("INSERT INTO settings(key,value) VALUES(?,?) ON CONFLICT(key) DO UPDATE SET value=excluded.value")){p.setString(1,key);p.setString(2,value);p.executeUpdate();}}
    public void exportCsv(User actor,String type,Path target)throws Exception {require(actor,"ADMIN");if(!Set.of("courses","registrations","feedback").contains(type))throw new IllegalArgumentException("Unsupported CSV report type.");List<String[]> rows=new ArrayList<>();String header="";try(Connection c=db.open()){String sql;if(type.equals("courses")){header="code,title,credits,capacity,enrolled,semester,faculty,active";sql="SELECT c.code,c.title,c.credits,c.capacity,(SELECT COUNT(*) FROM registrations r WHERE r.course_code=c.code AND r.status='REGISTERED'),c.semester,u.name,c.active FROM courses c JOIN users u ON u.id=c.faculty_id";}else if(type.equals("registrations")){header="student_id,student_name,course_code,course_title,credits,status,registered_at";sql="SELECT r.student_id,u.name,r.course_code,c.title,c.credits,r.status,r.registered_at FROM registrations r JOIN users u ON u.id=r.student_id JOIN courses c ON c.code=r.course_code";}else{header="course_code,response_count,average_rating";sql="SELECT c.code,COUNT(f.id),ROUND(AVG(f.rating),2) FROM courses c LEFT JOIN feedback f ON f.course_code=c.code GROUP BY c.code";}try(PreparedStatement p=c.prepareStatement(sql);ResultSet r=p.executeQuery()){int minimum=Integer.parseInt(setting(c,"min_responses"));while(r.next()){String[] row=new String[r.getMetaData().getColumnCount()];for(int i=0;i<row.length;i++)row[i]=r.getString(i+1);if(type.equals("feedback")&&r.getInt(2)<minimum){row[1]="<"+minimum;row[2]="SUPPRESSED";}rows.add(row);}}}Files.createDirectories(target.toAbsolutePath().getParent());List<String> lines=new ArrayList<>();lines.add(header);for(String[] row:rows){StringBuilder b=new StringBuilder();for(int i=0;i<row.length;i++){if(i>0)b.append(',');b.append(csvCell(row[i]));}lines.add(b.toString());}Files.write(target,lines,StandardCharsets.UTF_8);}
    /** Returns department averages only where at least three distinct faculty have eligible feedback. */
    public List<String[]> departmentBenchmarks(User actor)throws Exception {require(actor,"ADMIN");List<String[]> out=new ArrayList<>();String sql="SELECT u.department,COUNT(DISTINCT c.faculty_id),COUNT(DISTINCT c.code),COUNT(f.id),AVG(f.rating) FROM courses c JOIN users u ON u.id=c.faculty_id LEFT JOIN feedback f ON f.course_code=c.code GROUP BY u.department ORDER BY u.department";try(Connection c=db.open();PreparedStatement p=c.prepareStatement(sql);ResultSet r=p.executeQuery()){int minimum=Integer.parseInt(setting(c,"min_responses"));while(r.next()){int faculty=r.getInt(2),responses=r.getInt(4);boolean eligible=faculty>=3&&responses>=minimum;out.add(new String[]{r.getString(1),String.valueOf(faculty),String.valueOf(r.getInt(3)),String.valueOf(responses),eligible?String.format(Locale.ROOT,"%.2f",r.getDouble(5)):"SUPPRESSED",eligible?"VISIBLE":"Hidden: needs 3 faculty and "+minimum+" responses"});}}return out;}
    /** Last audit events, visible only to administrators and excluding credentials. */
    public List<String[]> auditEvents(User actor,int limit)throws Exception {require(actor,"ADMIN");if(limit<1||limit>200)throw new IllegalArgumentException("Audit limit must be between 1 and 200.");List<String[]> out=new ArrayList<>();try(Connection c=db.open();PreparedStatement p=c.prepareStatement("SELECT id,COALESCE(actor_id,''),action,detail,created_at FROM audit_log ORDER BY id DESC LIMIT ?")){p.setInt(1,limit);try(ResultSet r=p.executeQuery()){while(r.next())out.add(new String[]{r.getString(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5)});}}return out;}
    public List<String[]> qualityOverview(User actor)throws Exception {require(actor,"ADMIN");List<String[]> out=new ArrayList<>();try(Connection c=db.open();PreparedStatement p=c.prepareStatement("SELECT c.code,c.title,COUNT(f.id),COALESCE(AVG(f.rating),0),(SELECT COUNT(*) FROM doubts d WHERE d.course_code=c.code AND d.status='OPEN') FROM courses c LEFT JOIN feedback f ON f.course_code=c.code GROUP BY c.code ORDER BY AVG(f.rating) ASC,c.code");ResultSet r=p.executeQuery()){while(r.next())out.add(new String[]{String.valueOf(out.size()+1),r.getString(1),r.getString(2),String.valueOf(r.getInt(3)),String.format(Locale.ROOT,"%.2f",r.getDouble(4)),String.valueOf(r.getInt(5)),r.getDouble(4)<Double.parseDouble(setting(c,"alert_threshold"))?"ALERT":"OK"});}return out;}
    public List<String[]> waitlist(User actor)throws Exception {require(actor,"ADMIN","FACULTY");String sql="SELECT w.id,w.course_code,w.student_id,u.name,w.created_at,w.status,w.reason,c.faculty_id FROM waitlist w JOIN users u ON u.id=w.student_id JOIN courses c ON c.code=w.course_code WHERE w.status='WAITING'";if(actor.role.equals("FACULTY"))sql+=" AND c.faculty_id=?";sql+=" ORDER BY w.id";List<String[]> out=new ArrayList<>();try(Connection c=db.open();PreparedStatement p=c.prepareStatement(sql)){if(actor.role.equals("FACULTY"))p.setString(1,actor.id);try(ResultSet r=p.executeQuery()){while(r.next())out.add(new String[]{r.getString(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5),r.getString(6),r.getString(7)});}}return out;}
    public void backupDatabase(User actor,Path target)throws Exception {require(actor,"ADMIN");db.backup(target);try(Connection c=db.open()){audit(c,actor.id,"DATABASE_BACKUP",target.toString());}}
    public List<String[]> simpleStats(User actor)throws Exception {require(actor,"ADMIN");List<String[]> out=new ArrayList<>();try(Connection c=db.open();PreparedStatement p=c.prepareStatement("SELECT 'Users',COUNT(*) FROM users UNION ALL SELECT 'Active courses',COUNT(*) FROM courses WHERE active=1 UNION ALL SELECT 'Active registrations',COUNT(*) FROM registrations WHERE status='REGISTERED' UNION ALL SELECT 'Feedback responses',COUNT(*) FROM feedback UNION ALL SELECT 'Open doubts',COUNT(*) FROM doubts WHERE status='OPEN' UNION ALL SELECT 'Waiting students',COUNT(*) FROM waitlist WHERE status='WAITING' UNION ALL SELECT 'Unread notifications',COUNT(*) FROM notifications WHERE is_read=0");ResultSet r=p.executeQuery()){while(r.next())out.add(new String[]{r.getString(1),r.getString(2)});}return out;}
    private Course findCourse(String code)throws Exception {try(Connection c=db.open();PreparedStatement p=c.prepareStatement("SELECT c.*,u.name faculty_name,(SELECT COUNT(*) FROM registrations r WHERE r.course_code=c.code AND r.status='REGISTERED') enrolled FROM courses c JOIN users u ON u.id=c.faculty_id WHERE c.code=?")){p.setString(1,code);try(ResultSet r=p.executeQuery()){return r.next()?course(r):null;}}}
    private static User user(ResultSet r)throws SQLException{return new User(r.getString("id"),r.getString("name"),r.getString("email"),r.getString("role"),r.getString("department"),r.getString("roll_or_employee"),r.getString("designation"),r.getInt("semester"),r.getInt("active")==1,r.getInt("must_change")==1);}
    private static Course course(ResultSet r)throws SQLException{return new Course(r.getString("code"),r.getString("title"),r.getInt("credits"),r.getInt("capacity"),r.getInt("semester"),r.getString("faculty_id"),r.getString("faculty_name"),r.getString("day"),r.getString("start_time"),r.getString("end_time"),r.getInt("enrolled"),r.getInt("active")==1);}
    private static String required(String s,String field,int min,int max){s=s==null?"":s.trim();if(s.length()<min||s.length()>max)throw new IllegalArgumentException(field+" must contain "+min+"-"+max+" characters.");return sanitize(s);}
    private static String sanitize(String s){return s==null?"":s.replace('|',' ').replace('\r',' ').replace('\n',' ').trim();}
    /** Quotes formula-looking fields for spreadsheet safety before CSV serialization. */
    private static String csvCell(String value){String v=value==null?"":value;if(!v.isEmpty()&&"=+-@\t".indexOf(v.charAt(0))>=0)v="'"+v;v=v.replace("\"","\"\"");return v.contains(",")||v.contains("\n")||v.contains("\"")?'"'+v+'"':v;}
    private static void validatePassword(String p,String id){if(p==null||p.length()<8||p.length()>64||!p.matches(".*[A-Za-z].*")||!p.matches(".*[0-9].*")||p.equalsIgnoreCase(id))throw new IllegalArgumentException("Password must be 8-64 characters, contain a letter and digit, and differ from user ID.");}
    private static void require(User actor,String... roles){if(actor==null||!actor.active||actor.mustChange||Arrays.stream(roles).noneMatch(r->r.equals(actor.role)))throw new SecurityException("Access denied. Sign in with an active account and complete any required password change first.");}
    private static boolean overlap(String a,String b,String c,String d){return LocalTime.parse(a).isBefore(LocalTime.parse(d))&&LocalTime.parse(c).isBefore(LocalTime.parse(b));}
    private static Set<String> words(String s){Set<String> out=new HashSet<>();for(String w:s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]"," ").split("\\s+"))if(w.length()>2&&!Set.of("the","and","for","are","how","what","can","you","does","with","this","that","from","when","where","why","who","not","have","has","will","course").contains(w))out.add(w);return out;}
    private static String setting(Connection c,String key)throws SQLException {try(PreparedStatement p=c.prepareStatement("SELECT value FROM settings WHERE key=?")){p.setString(1,key);try(ResultSet r=p.executeQuery()){return r.next()?r.getString(1):"";}}}
    private static void notify(Connection c,String user,String title,String body)throws SQLException {if(user==null||user.isBlank())return;try(PreparedStatement p=c.prepareStatement("INSERT INTO notifications(user_id,title,body,created_at) VALUES(?,?,?,?)")){p.setString(1,user);p.setString(2,sanitize(title));p.setString(3,sanitize(body));p.setString(4,LocalDateTime.now().toString());p.executeUpdate();}}
    private static void audit(Connection c,String actor,String action,String detail)throws SQLException {try(PreparedStatement p=c.prepareStatement("INSERT INTO audit_log(actor_id,action,detail,created_at) VALUES(?,?,?,?)")){p.setString(1,actor);p.setString(2,action);p.setString(3,sanitize(detail));p.setString(4,LocalDateTime.now().toString());p.executeUpdate();}}
}
