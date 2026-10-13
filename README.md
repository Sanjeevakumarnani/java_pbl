# CRFMS Desktop

Course Registration & Feedback Management System (CRFMS) is a Java Swing desktop application for managing course registration, faculty/student workflows, feedback collection, FAQ and doubt handling, announcements, and reports.

This project is designed to run locally with SQLite, without needing a separate database server.

## Features

- Role-based access for admin, faculty, and students
- User authentication and password reset flow
- Course creation, assignment, registration, and waitlist handling
- Credit limit, capacity, and timetable checks
- Feedback collection and teaching-quality analysis
- FAQ and doubt ticket workflow
- Announcements, resource sharing, and notifications
- CSV export for reports and backup support
- Persistent local database stored in `data/crfms.db`

## Prerequisites

Before running the app, make sure you have:

- Java 17 or newer installed
- A terminal or command prompt available
- Internet access for the first launch so the SQLite JDBC and SLF4J jars can be downloaded automatically

Check your Java installation:

Windows:
```bat
java -version
javac -version
```

Linux/macOS:
```bash
java -version
javac -version
```

If Java is not installed, install JDK 17 or later before continuing.

## Repository layout

```text
.
├── src/main/java/            Java source files for the desktop application
├── src/test/java/            Test classes
├── docs/                     Project documentation, schema, and reports
├── lib/                      Runtime jars downloaded automatically
├── data/                     Local SQLite database and generated app data
├── exports/                  CSV exports created at runtime
├── backups/                  Database backup snapshots
├── logs/                     Application logs
├── run.sh                   Linux/macOS launcher
├── run.bat                  Windows launcher
├── build.sh                 Compile-only script
├── build.bat                Windows compile script
├── test.sh                  Test script
├── test.bat                 Windows test script
├── pom.xml                  Maven project definition
├── README.md                Project documentation
├── docs/schema.sql          SQLite schema
└── .gitignore
```

## Quick start

### Windows

1. Install JDK 17 or later.
2. Open Command Prompt in the project folder.
3. Run:

```bat
run.bat
```

The app will compile the project and download required runtime jars if they are missing.

### Linux / macOS

1. Open a terminal in the project folder.
2. Make the scripts executable:

```bash
chmod +x run.sh build.sh test.sh
```

3. Start the application:

```bash
./run.sh
```

If dependencies are missing, the launcher downloads them automatically from Maven Central.

## Demo mode

To seed demo accounts and sample data:

Windows:
```bat
run.bat --demo
```

Linux/macOS:
```bash
./run.sh --demo
```

This creates fictional sample accounts for testing.

## Default demo credentials

The demo dataset includes these accounts:

- Admin
  - User ID: `admin`
  - Password: `Admin@123`
- Faculty
  - User ID: `fac101`
  - Password: `Welcome123`
- Student
  - User ID: `stu1001`
  - Password: `Welcome123`

Important:
- All seeded accounts require a password change on first login.
- Demo data is fictional and should not be used for real institutional records.

## Building the project

### Linux/macOS
```bash
./build.sh
```

### Windows
```bat
build.bat
```

The build script compiles the Java source into the `out/` directory.

## Running tests

### Linux/macOS
```bash
./test.sh
```

### Windows
```bat
test.bat
```

The test script runs:

- Java compilation checks
- Security tests
- schema validation (if Python 3 is installed)
- integration tests that require SQLite JDBC files in `lib/`

## Manual dependency note

The project is set up to download runtime dependencies automatically on first run.

If the app cannot reach Maven Central, install the following jars manually in the `lib/` folder:

- `sqlite-jdbc-3.50.3.0.jar`
- `slf4j-api-2.0.17.jar`
- `slf4j-nop-2.0.17.jar`

Then run the launcher again.

## Maven option

This repository also includes a `pom.xml` for Maven-based builds:

```bash
mvn clean package
```

## Database and data files

The app stores data locally in SQLite.

- Main database: `data/crfms.db`
- Database schema: `docs/schema.sql`
- Backup files: `backups/`
- CSV exports: `exports/`

Back up the database before making manual changes.

## Troubleshooting

### Java version error

If you see errors about unsupported class files or Java version mismatch, install JDK 17 and confirm:

```bash
java -version
javac -version
```

### Dependency download failed

Check your internet connection and rerun:

```bash
./run.sh
```

or on Windows:

```bat
run.bat
```

### App does not start after build

Make sure the required libraries exist in `lib/` and rerun the launcher.

## Notes

- This is a desktop GUI application, not a web app.
- The program runs on the local machine and uses a local SQLite database.
- Passwords are hashed and stored securely; they are not kept as plain text.
- Demo identities are intended for testing and feature exploration only.

## License and usage

This project is intended for academic and learning use. Please treat the demo data and credentials as sample-only content.

## Summary

If you just want to run the system quickly:

```bash
chmod +x run.sh build.sh test.sh
./run.sh
```

Or on Windows:

```bat
run.bat
```

Then sign in with the admin account and complete the first-time password change flow.
