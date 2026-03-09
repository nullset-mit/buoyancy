# Feature 1 — Phase 7c Pre-work: UX Fixes

**Status: draft**

## Context

Before starting Phase 7c proper, two UX/data-integrity issues surfaced during manual testing of the persistent CLI. This plan addresses them as pre-work.

---

## Fix 1 — Suppress jOOQ Banner

### Problem

jOOQ OSS prints a logo and "tip of the day" to stdout on first `DSLContext` use. This floods the interactive CLI with noise unrelated to the application.

### Solution

Set the `Settings` object on the `DSLContext` to disable the logo and tips. This is done in `createDslContext()` inside `SQLiteClassificationRuleRepository`.

### Change

**File:** `src/java/.../infrastructure/persistence/SQLiteClassificationRuleRepository.java`

In `createDslContext()`, replace:
```java
return DSL.using(connection, SQLDialect.SQLITE);
```
with:
```java
Settings settings = new Settings().withRenderSchema(false);
return DSL.using(connection, SQLDialect.SQLITE, settings);
```

And set the system properties before creating the context:
```java
System.setProperty("org.jooq.no-logo", "true");
System.setProperty("org.jooq.no-tips", "true");
```

Place the system property calls at the top of `createDslContext()`, before the PRAGMA statement. System properties are global and only need to be set once, but setting them in the repository constructor keeps the jOOQ concern co-located with jOOQ usage.

### Tests

No new tests needed. Existing tests still pass — the properties are harmless in test context.

---

## Fix 2 — Unique Category Name (Case-Insensitive)

### Problem

The current schema allows inserting multiple categories with the same name (e.g., "Shopping" and "shopping"). This is a data integrity issue — categories should be unique by name regardless of case.

### Solution

Add a unique index on `LOWER(name)` to the `category` table in `schema.sql`. Using `CREATE UNIQUE INDEX IF NOT EXISTS` keeps it idempotent with the existing `CREATE TABLE IF NOT EXISTS` pattern.

### Schema Change

**File:** `src/resources/schema.sql`

Add after the `category` table definition:
```sql
CREATE UNIQUE INDEX IF NOT EXISTS idx_category_name_unique
    ON category (LOWER(name));
```

### New Exception — `DuplicateCategoryException`

**File:** `src/java/net/mossworks/buoyancy/application/DuplicateCategoryException.java`

An unchecked (runtime) exception in the application layer. This is a domain-meaningful error that the controller can catch and handle gracefully — not a generic `IllegalArgumentException`.

```java
public class DuplicateCategoryException extends RuntimeException {
    public DuplicateCategoryException(String name) {
        super("A category with name '" + name + "' already exists");
    }
}
```

### Error Handling Chain

```
User enters "shopping"
  → CategoryCreationWorkflow calls RuleCreationUseCase.createCategory("shopping")
    → RuleCreationUseCase calls repo.writeCategory(category)
      → SQLiteRepo detects duplicate name → throws DuplicateCategoryException
    → DuplicateCategoryException propagates up
  → CategoryCreationWorkflow catches DuplicateCategoryException
  → Prints "A category with name 'shopping' already exists. Please choose a different name."
  → Re-prompts for category name
```

### Impact on Existing Code

- `writeCategory()` — replace the existing duplicate-ID check with a case-insensitive name check. Throw `DuplicateCategoryException` (not `IllegalArgumentException`) when a category with the same name already exists.
- `ensureCategoryExists()` — uses `onDuplicateKeyIgnore()` which keys on the PRIMARY KEY (id). A same-name-different-id insert will now fail on the unique index. Update to a SELECT-by-LOWER(name)-then-INSERT pattern: if a category with the same name exists, reuse it (return the existing row's ID); only insert if truly new.
- `CategoryCreationWorkflow.run()` — wrap the `useCase.createCategory()` call in a try/catch for `DuplicateCategoryException`. Print the error message and loop back to re-prompt.
- Existing tests that assert `IllegalArgumentException` on duplicate category ID — update to assert `DuplicateCategoryException` instead.

### Changes

**File:** `src/resources/schema.sql`
- Add the unique index statement.

**File:** `src/java/net/mossworks/buoyancy/application/DuplicateCategoryException.java`
- New file.

**File:** `src/java/.../infrastructure/persistence/SQLiteClassificationRuleRepository.java`
- `writeCategory()`: Replace duplicate-ID check with case-insensitive name check. Throw `DuplicateCategoryException`.
- `ensureCategoryExists()`: Change from `onDuplicateKeyIgnore()` to SELECT-by-name-then-INSERT pattern.

**File:** `src/java/.../adapter/cli/classify/CategoryCreationWorkflow.java`
- Catch `DuplicateCategoryException` in `run()`, print message, re-prompt.

### New Tests

**File:** `src/test/java/.../infrastructure/persistence/SQLiteClassificationRuleRepositoryTest.java`

Add:
1. `writeCategory_throwsException_onDuplicateName_caseInsensitive` — write "Shopping", then write "shopping" (different UUID), expect `DuplicateCategoryException`.
2. `writeRule_reusesCategoryByName_caseInsensitive` — write a rule with category "Groceries", then write another rule whose counterparty has category "groceries" (different UUID). The second write should reuse the existing category row rather than failing.

Update:
3. `writeCategory_throwsException_onDuplicateId` — change expected exception from `IllegalArgumentException` to `DuplicateCategoryException` (same ID implies same name since the first write succeeded).

**File:** `src/test/java/.../adapter/cli/classify/CategoryCreationWorkflowTest.java`

Add:
4. `run_reprompts_whenCategoryNameAlreadyExists` — stub use case to throw `DuplicateCategoryException` on first call, succeed on second. Assert output contains the error message and that the workflow completes successfully.

---

## Files Changed

| Action | File |
|--------|------|
| Modify | `src/java/.../infrastructure/persistence/SQLiteClassificationRuleRepository.java` |
| Modify | `src/resources/schema.sql` |
| Create | `src/java/.../application/DuplicateCategoryException.java` |
| Modify | `src/java/.../adapter/cli/classify/CategoryCreationWorkflow.java` |
| Modify | `src/test/java/.../infrastructure/persistence/SQLiteClassificationRuleRepositoryTest.java` |
| Modify | `src/test/java/.../adapter/cli/classify/CategoryCreationWorkflowTest.java` |

## Verification

1. `mvn test` — all 49 existing tests plus 3 new tests pass (updated count reflects 1 modified + 2 new repo tests + 1 new workflow test).
2. Run the CLI, create "Shopping", then try to create "shopping" — should see a friendly error message and a re-prompt, not a crash.
3. `sqlite3 ~/.buoyancy/buoyancy.db "SELECT * FROM category;"` — no duplicate names.

## Migration Note

The existing `~/.buoyancy/buoyancy.db` may already have duplicate-name categories. The `CREATE UNIQUE INDEX IF NOT EXISTS` will fail if duplicates exist. For a dev database this is acceptable — delete and recreate. If this were production, we'd need a migration script to deduplicate first.


## Developer's Notes
1. The database might already have a category called 'Shopping'. For the test we should delete the entire table, create our test data, then roll back the transaction at the end of the test.
   We may also consider just creating a new database file from scratch that we clean up at the end of our tests.
   > **Response:** Not an issue. Every test gets a fresh in-memory SQLite database (`jdbc:sqlite::memory:`) created in `@BeforeEach`. Each test starts with an empty schema — no collision with real data is possible. The in-memory DB is discarded when the connection goes out of scope. No cleanup needed.
2. This brings up another point: how are we cleaning up data after the tests? Are you sure that we clean it up (even on test failure)? Are we testing on a brand-new DB, or whatever is currently in the db file?
   > **Response:** Confirmed — we test on a brand-new DB every time. `@BeforeEach` in `SQLiteClassificationRuleRepositoryTest` creates a new `DriverManager.getConnection("jdbc:sqlite::memory:")` and a new `SQLiteClassificationRuleRepository(connection)`. Each test is fully isolated. Even on test failure, there is nothing to clean up — the in-memory DB lives only for the duration of the test method.
3. When we enter a category which already exists, we don't want to crash the application with an uncaught exception. The repository needs to catch the exception and re-throw one that makes more sense for the use case, such as a CategoryAlreadyExists exception, which will dictate to the controller that we need to tell the user to pick a different name.
   > **Response:** Agreed. Updated plan below.
