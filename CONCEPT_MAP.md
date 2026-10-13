# CONCEPT_MAP.md

Mapping OOP/Java concepts to actual classes in this codebase.

## Classes & Objects
- LoginFrame, MainFrame, Main: Swing UI classes (objects instantiated at runtime).
- CrfmsService: service object encapsulating business logic and data access coordination.
- Database: manages SQLite connections; initializes schema.
- Security: utility class for PBKDF2 hashing/verification (static methods).
- Models: User, Course (static inner classes in CrfmsService, immutable value holders).

## Encapsulation
- Private fields and helpers; operations go through service methods. Access control via require().

## Static/final
- final classes/methods where appropriate; static helpers (sanitize, words, setting, notify, audit). Private constructors prevent instantiation.

## Inheritance/Polymorphism
- JFrame extended by LoginFrame and MainFrame. Interfaces planned (SuggestionRule, DAO interfaces).

## Exceptions & Validation
- IllegalArgumentException/SecurityException for validation/authorization; SQLException handled properly.

## Collections/Generics
- ArrayList, HashMap, HashSet, List, Map, Set with generics.

## Enums (planned)
- Role/status currently string-checked; enums to be introduced in refactor for type safety.

## JDBC & Persistence
- Connection, PreparedStatement, ResultSet with parameterized queries.

## Date/Time
- LocalDate, LocalDateTime, LocalTime for validation/timestamps.

## Design Patterns
- DAO pattern planned (interfaces + SQLite implementations), Service layer, Dependency injection via constructor (db passed in), Facade via CrfmsService.

## OOP Principles
- Encapsulation, Abstraction, Polymorphism, Responsibility separation (UI/service/data). Packages organize by feature.
