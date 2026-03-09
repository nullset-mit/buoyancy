# Feature 1 — Phase 6 Plan: SQLite DB Integration

## Goal

Replace the YAML-backed rule repository with a SQLite-backed one, keeping the
same end-user CLI behavior (`classify -i`). The YAML repository remains in the
codebase as an alternative implementation.

---

## Decisions

- **jOOQ 3.20 OSS** + **xerial sqlite-jdbc** (already decided)
- **Java 21** (already upgraded — pom.xml and Lombok updated)
- **Schema as a static `.sql` file**, executed by the repository on startup via
  `CREATE TABLE IF NOT EXISTS`
- **Schema initialization lives inside the repository constructor** — no
  separate initializer class
- **Single schema file** with tables in dependency order

---

## Step 1 — Add jOOQ and sqlite-jdbc to pom.xml

**File:** `pom.xml`

Add dependencies:
- `org.jooq:jooq:3.20.x` (verify latest 3.20 at implementation time)
- `org.xerial:sqlite-jdbc:3.x` (verify latest at implementation time)

Both are default (compile) scope.

## Step 2 — Create schema.sql

**File:** `src/resources/schema.sql`

Three tables in dependency order:

```sql
CREATE TABLE IF NOT EXISTS category (
    id           TEXT PRIMARY KEY,
    name         TEXT NOT NULL,
    sub_category TEXT
);

CREATE TABLE IF NOT EXISTS counterparty (
    id                  TEXT PRIMARY KEY,
    name                TEXT NOT NULL,
    default_category_id TEXT NOT NULL,
    FOREIGN KEY (default_category_id) REFERENCES category(id)
);

CREATE TABLE IF NOT EXISTS classification_rule (
    id               TEXT PRIMARY KEY,
    memo_pattern     TEXT    NOT NULL,
    priority         INTEGER NOT NULL DEFAULT 0,
    counterparty_id  TEXT    NOT NULL,
    amount_type      TEXT    NOT NULL,
    FOREIGN KEY (counterparty_id) REFERENCES counterparty(id)
);
```

## Step 3 — Create BuoyancySchema.java

**File:** `src/java/net/mossworks/buoyancy/infrastructure/persistence/BuoyancySchema.java`

Package-private utility class. Defines jOOQ `Table<Record>` and typed `Field<T>`
constants for all three tables using `DSL.field(DSL.name("table", "column"), type)`
form for correct table-qualification in JOINs.

## Step 4 — Create SQLiteClassificationRuleRepository.java

**File:** `src/java/net/mossworks/buoyancy/infrastructure/persistence/SQLiteClassificationRuleRepository.java`

Implements `ClassificationRuleRepository`.

**Constructor (Path dbPath):**
1. Opens JDBC connection to the `.db` file
2. Sets `PRAGMA foreign_keys = ON`
3. Creates `DSLContext` with `SQLDialect.SQLITE`
4. Reads `schema.sql` from classpath and executes it

**Package-private constructor (Connection):**
For in-memory SQLite testing. Same steps 2–4, skips connection creation.

**loadRules():**
Three-way JOIN (classification_rule → counterparty → category) using jOOQ DSL
and BuoyancySchema constants. Maps `Record` → `Category` → `Counterparty` →
`ClassificationRule`. Returns domain objects only — no jOOQ types cross the
layer boundary.

**writeRule(ClassificationRule):**
Checks for duplicate rule ID (throws `IllegalArgumentException`). Within a
transaction: `ensureCategoryExists()` → `ensureCounterpartyExists()` → INSERT
rule. The ensure methods use `INSERT OR IGNORE` semantics via
`onDuplicateKeyIgnore()`.

Both `ensureCategoryExists` and `ensureCounterpartyExists` are private — they
are persistence implementation details, not part of the repository interface.

## Step 5 — Write unit tests

**File:** `src/test/java/net/mossworks/buoyancy/infrastructure/persistence/SQLiteClassificationRuleRepositoryTest.java`

Uses `jdbc:sqlite::memory:` with fresh connection per test via `@BeforeEach`.

Test cases:
1. `loadRules_returnsEmptyList_whenNoRulesExist`
2. `writeRule_persistsRule_andLoadReturnsIt`
3. `writeRule_appendsRule_toExistingRules`
4. `writeRule_throwsException_whenDuplicateRuleId`
5. `writeRule_reusesCategoryAndCounterparty_whenAlreadyPersisted`
6. `writeRule_handlesNullSubCategory`
7. `writeRule_handlesNonNullSubCategory`
8. `loadRules_roundTrips_allAmountTypes` (CREDIT, DEBIT, BOTH)

## Step 6 — Update Buoyancy.java

**File:** `src/java/net/mossworks/buoyancy/Buoyancy.java`

Switch to SQLite as the default repository. Use `~/.buoyancy/buoyancy.db` as
the database path (replacing the YAML path). The `repo` variable is typed as
`ClassificationRuleRepository`, so downstream wiring is unchanged.

## Step 7 — Add database files to .gitignore

**File:** `.gitignore`

Add `*.db` and `*.db-journal` to prevent SQLite database files from being
committed.

## Step 8 — Run full test suite

Verify all existing tests (30) plus new SQLite tests pass. Existing YAML repo
and its tests remain untouched and functional.

---

## Files Changed

| Action | File |
|--------|------|
| Modify | `pom.xml` |
| Create | `src/resources/schema.sql` |
| Create | `src/java/.../infrastructure/persistence/BuoyancySchema.java` |
| Create | `src/java/.../infrastructure/persistence/SQLiteClassificationRuleRepository.java` |
| Create | `src/test/java/.../infrastructure/persistence/SQLiteClassificationRuleRepositoryTest.java` |
| Modify | `src/java/.../Buoyancy.java` |
| Modify | `.gitignore` |

## Potential Pitfalls

- **jOOQ DDL vs raw SQL for schema init**: schema.sql is executed as raw SQL,
  so jOOQ DDL builder quirks with SQLite FKs are avoided
- **`onDuplicateKeyIgnore()` on SQLite**: verify jOOQ emits `INSERT OR IGNORE`;
  fall back to raw SQL if not
- **jOOQ field qualification in JOINs**: must use `DSL.name("table", "column")`
  form
- **Lombok 1.18.34**: already upgraded in prior step
