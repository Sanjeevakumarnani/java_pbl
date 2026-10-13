package crfms;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/** Opens and initializes the local SQLite database. */
public final class Database {
    private final String url;
    private final Path dbFile;
    public Database(Path root) throws Exception {
        Files.createDirectories(root);
        this.dbFile = root.resolve("crfms.db").toAbsolutePath();
        this.url = "jdbc:sqlite:" + dbFile;
        Class.forName("org.sqlite.JDBC");
        initialize();
    }
    /** Opens a new connection; SQLite enforces foreign keys per connection. */
    public Connection open() throws SQLException {
        Connection c = DriverManager.getConnection(url);
        try (Statement s = c.createStatement()) { s.execute("PRAGMA foreign_keys=ON"); s.execute("PRAGMA busy_timeout=5000"); }
        return c;
    }
    /** Creates a consistent file backup after asking SQLite to checkpoint its write-ahead log. */
    public void backup(Path target) throws Exception {
        try(Connection c=open();Statement s=c.createStatement()){s.execute("PRAGMA wal_checkpoint(FULL)");}
        Files.createDirectories(target.toAbsolutePath().getParent());
        Files.copy(dbFile,target,StandardCopyOption.REPLACE_EXISTING);
    }
    private void initialize() throws SQLException {
        String[] ddl = {
            "CREATE TABLE IF NOT EXISTS users(id TEXT PRIMARY KEY, name TEXT NOT NULL, email TEXT NOT NULL UNIQUE COLLATE NOCASE, role TEXT NOT NULL CHECK(role IN ('ADMIN','FACULTY','STUDENT')), department TEXT NOT NULL, roll_or_employee TEXT UNIQUE, semester INTEGER, designation TEXT, salt TEXT NOT NULL, password_hash TEXT NOT NULL, active INTEGER NOT NULL DEFAULT 1, must_change INTEGER NOT NULL DEFAULT 1, created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)",
            "CREATE TABLE IF NOT EXISTS courses(code TEXT PRIMARY KEY, title TEXT NOT NULL, credits INTEGER NOT NULL CHECK(credits BETWEEN 1 AND 6), capacity INTEGER NOT NULL CHECK(capacity BETWEEN 1 AND 200), semester INTEGER NOT NULL CHECK(semester BETWEEN 1 AND 12), faculty_id TEXT NOT NULL REFERENCES users(id), day TEXT, start_time TEXT, end_time TEXT, active INTEGER NOT NULL DEFAULT 1)",
            "CREATE TABLE IF NOT EXISTS registrations(id INTEGER PRIMARY KEY AUTOINCREMENT, student_id TEXT NOT NULL REFERENCES users(id), course_code TEXT NOT NULL REFERENCES courses(code), status TEXT NOT NULL CHECK(status IN ('REGISTERED','DROPPED')), registered_at TEXT NOT NULL, dropped_at TEXT, UNIQUE(student_id,course_code))",
            "CREATE TABLE IF NOT EXISTS feedback(id INTEGER PRIMARY KEY AUTOINCREMENT, student_id TEXT NOT NULL REFERENCES users(id), course_code TEXT NOT NULL REFERENCES courses(code), rating INTEGER NOT NULL CHECK(rating BETWEEN 1 AND 5), comment TEXT NOT NULL DEFAULT '', created_at TEXT NOT NULL, UNIQUE(student_id,course_code))",
            "CREATE TABLE IF NOT EXISTS faq_entries(id INTEGER PRIMARY KEY AUTOINCREMENT, course_code TEXT NOT NULL DEFAULT 'ALL', question TEXT NOT NULL, answer TEXT NOT NULL, active INTEGER NOT NULL DEFAULT 1)",
            "CREATE TABLE IF NOT EXISTS doubts(id INTEGER PRIMARY KEY AUTOINCREMENT, student_id TEXT NOT NULL REFERENCES users(id), course_code TEXT NOT NULL REFERENCES courses(code), question TEXT NOT NULL, anonymous INTEGER NOT NULL DEFAULT 0, status TEXT NOT NULL DEFAULT 'OPEN', answer TEXT NOT NULL DEFAULT '', created_at TEXT NOT NULL, answered_at TEXT)",
            "CREATE TABLE IF NOT EXISTS feedback_periods(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, starts_on TEXT NOT NULL, ends_on TEXT NOT NULL, is_open INTEGER NOT NULL DEFAULT 1, min_responses INTEGER NOT NULL DEFAULT 3)",
            "CREATE TABLE IF NOT EXISTS teaching_feedback(id INTEGER PRIMARY KEY AUTOINCREMENT, student_id TEXT NOT NULL REFERENCES users(id), course_code TEXT NOT NULL REFERENCES courses(code), period_id INTEGER NOT NULL REFERENCES feedback_periods(id), clarity INTEGER, preparation INTEGER, pace INTEGER, engagement INTEGER, examples INTEGER, availability INTEGER, fairness INTEGER, materials INTEGER, feedback_quality INTEGER, overall INTEGER, tags TEXT NOT NULL DEFAULT '', suggestion TEXT NOT NULL DEFAULT '', moderation_status TEXT NOT NULL DEFAULT 'VISIBLE', created_at TEXT NOT NULL, UNIQUE(student_id,course_code,period_id))",
            "CREATE TABLE IF NOT EXISTS waitlist(id INTEGER PRIMARY KEY AUTOINCREMENT, student_id TEXT NOT NULL REFERENCES users(id), course_code TEXT NOT NULL REFERENCES courses(code), created_at TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'WAITING', reason TEXT NOT NULL DEFAULT '', UNIQUE(student_id,course_code))",
            "CREATE TABLE IF NOT EXISTS notifications(id INTEGER PRIMARY KEY AUTOINCREMENT, user_id TEXT NOT NULL REFERENCES users(id), title TEXT NOT NULL, body TEXT NOT NULL, is_read INTEGER NOT NULL DEFAULT 0, created_at TEXT NOT NULL)",
            "CREATE TABLE IF NOT EXISTS announcements(id INTEGER PRIMARY KEY AUTOINCREMENT, faculty_id TEXT NOT NULL REFERENCES users(id), course_code TEXT NOT NULL REFERENCES courses(code), title TEXT NOT NULL, body TEXT NOT NULL, created_at TEXT NOT NULL, active INTEGER NOT NULL DEFAULT 1)",
            "CREATE TABLE IF NOT EXISTS resources(id INTEGER PRIMARY KEY AUTOINCREMENT, faculty_id TEXT NOT NULL REFERENCES users(id), course_code TEXT NOT NULL REFERENCES courses(code), title TEXT NOT NULL, url TEXT NOT NULL, description TEXT NOT NULL DEFAULT '', created_at TEXT NOT NULL)",
            "CREATE TABLE IF NOT EXISTS suggestion_rules(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, category TEXT NOT NULL, operator TEXT NOT NULL, threshold REAL NOT NULL, advice TEXT NOT NULL, active INTEGER NOT NULL DEFAULT 1)",
            "CREATE TABLE IF NOT EXISTS improvement_actions(id INTEGER PRIMARY KEY AUTOINCREMENT, course_code TEXT NOT NULL REFERENCES courses(code), faculty_id TEXT NOT NULL REFERENCES users(id), advice TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'OPEN', created_at TEXT NOT NULL, updated_at TEXT NOT NULL)",
            "CREATE TABLE IF NOT EXISTS settings(key TEXT PRIMARY KEY, value TEXT NOT NULL)",
            "CREATE TABLE IF NOT EXISTS audit_log(id INTEGER PRIMARY KEY AUTOINCREMENT, actor_id TEXT, action TEXT NOT NULL, detail TEXT NOT NULL, created_at TEXT NOT NULL)"
        };
        try (Connection c = open(); Statement s = c.createStatement()) {
            for (String sql : ddl) s.execute(sql);
            ensureColumn(c, "waitlist", "reason", "TEXT NOT NULL DEFAULT ''");
            putDefault(s, "registration_open", "1"); putDefault(s, "feedback_open", "1"); putDefault(s, "max_credits", "24"); putDefault(s, "bot_min_score", "1"); putDefault(s, "min_responses", "3"); putDefault(s, "alert_threshold", "3.5");
        }
    }
    private void ensureColumn(Connection c, String table, String column, String definition) throws SQLException {
        boolean found=false;
        try(Statement s=c.createStatement();java.sql.ResultSet r=s.executeQuery("PRAGMA table_info("+table+")")) {
            while(r.next())if(column.equalsIgnoreCase(r.getString("name")))found=true;
        }
        if(!found)try(Statement s=c.createStatement()){s.execute("ALTER TABLE "+table+" ADD COLUMN "+column+" "+definition);}
    }
    private void putDefault(Statement s, String key, String value) throws SQLException {
        s.executeUpdate("INSERT OR IGNORE INTO settings(key,value) VALUES('" + key + "','" + value + "')");
    }
}
