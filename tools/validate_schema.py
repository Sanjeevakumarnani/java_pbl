#!/usr/bin/env python3
"""Offline SQLite schema and constraint smoke tests for CRFMS.
Uses Python's standard-library SQLite engine; it does not replace JDBC integration tests.
"""
from pathlib import Path
import re
import sqlite3
import sys

ROOT = Path(__file__).resolve().parents[1]
SCHEMA = ROOT / "docs" / "schema.sql"
JAVA_DB = ROOT / "src" / "main" / "java" / "crfms" / "Database.java"

def check(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)

conn = sqlite3.connect(":memory:")
conn.execute("PRAGMA foreign_keys=ON")
conn.executescript(SCHEMA.read_text(encoding="utf-8"))
tables = {row[0] for row in conn.execute("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'")}
expected = {"users", "courses", "registrations", "feedback", "faq_entries", "doubts", "feedback_periods", "teaching_feedback", "waitlist", "notifications", "announcements", "resources", "suggestion_rules", "improvement_actions", "settings", "audit_log"}
check(tables == expected, f"Schema table mismatch: missing={expected-tables}, extra={tables-expected}")
check("reason" in {row[1] for row in conn.execute("PRAGMA table_info(waitlist)")}, "waitlist eligibility reason column")
java_tables = set(re.findall(r"CREATE TABLE IF NOT EXISTS ([a-z_]+)", JAVA_DB.read_text(encoding="utf-8")))
check(java_tables == expected, f"Java initialization table mismatch: missing={expected-java_tables}, extra={java_tables-expected}")
settings = dict(conn.execute("SELECT key,value FROM settings"))
check(settings.get("registration_open") == "1", "registration_open default")
check(settings.get("feedback_open") == "1", "feedback_open default")
check(settings.get("max_credits") == "24", "max_credits default")
check(settings.get("bot_min_score") == "1", "bot_min_score default")
check(settings.get("min_responses") == "3", "min_responses default")
check(settings.get("alert_threshold") == "3.5", "alert_threshold default")

# Seed minimum valid parent rows, then verify constraints and foreign keys.
conn.execute("INSERT INTO users(id,name,email,role,department,roll_or_employee,salt,password_hash) VALUES(?,?,?,?,?,?,?,?)",
             ("fac101", "Test Faculty", "faculty@test.example", "FACULTY", "CSE", "EMP101", "salt", "hash"))
conn.execute("INSERT INTO users(id,name,email,role,department,roll_or_employee,salt,password_hash) VALUES(?,?,?,?,?,?,?,?)",
             ("stu101", "Test Student", "student@test.example", "STUDENT", "CSE", "ROLL101", "salt", "hash"))
conn.execute("INSERT INTO courses(code,title,credits,capacity,semester,faculty_id) VALUES(?,?,?,?,?,?)",
             ("CS101", "Programming Fundamentals", 3, 1, 3, "fac101"))
conn.execute("INSERT INTO registrations(student_id,course_code,status,registered_at) VALUES(?,?,?,?)",
             ("stu101", "CS101", "REGISTERED", "2026-10-11T10:00:00"))

def must_fail(sql: str, args: tuple, label: str) -> None:
    try:
        conn.execute(sql, args)
    except sqlite3.IntegrityError:
        return
    raise AssertionError(f"Expected SQLite constraint to reject: {label}")

must_fail("INSERT INTO users(id,name,email,role,department,salt,password_hash) VALUES(?,?,?,?,?,?,?)",
          ("stu102", "Duplicate email", "student@test.example", "STUDENT", "CSE", "salt", "hash"), "unique email")
must_fail("INSERT INTO courses(code,title,credits,capacity,semester,faculty_id) VALUES(?,?,?,?,?,?)",
          ("CS102", "Bad credits", 9, 10, 3, "fac101"), "credit range")
must_fail("INSERT INTO registrations(student_id,course_code,status,registered_at) VALUES(?,?,?,?)",
          ("stu101", "CS101", "REGISTERED", "2026-10-11T10:01:00"), "unique student/course registration")
must_fail("INSERT INTO registrations(student_id,course_code,status,registered_at) VALUES(?,?,?,?)",
          ("ghost", "CS101", "REGISTERED", "2026-10-11T10:01:00"), "foreign key")
check(conn.execute("PRAGMA foreign_key_check").fetchall() == [], "foreign key check clean")
print("SchemaValidation: 14 checks passed (16 tables, Java/schema parity, defaults, uniqueness, range, foreign keys).")
conn.close()
