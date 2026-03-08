# Feature 1 — Phase 7a Plan: Category Creation Workflow

**Status: approved — implementing**

## Context

When `classify -i` finds no matching rule, the current behavior prints "no match" and exits.
Phase 7 replaces this with an interactive workflow to create a classification rule from scratch.
Phase 7a builds the innermost piece: creating a `Category`. Subsequent parts (7b–7e) add
category selection, counterparty creation/selection, and the final rule assembly.

---

## User-Facing Flow (7a)

# EDITED - Updated copy. #

```
No matching rule found.

<New Category>
Enter category name: Shopping
Category 'Shopping' created.
```

# End Edit #


After creation the app exits. The workflow is intentionally incomplete at this stage.
No sub-category prompt (deferred until there is a reason to add it).

---

## Call Stack

```
Buoyancy.main(args)
  └─ CommandLineInputAdapter.run()
       ├─ useCase.classify(transaction)          // returns null (no match)
       │
       ├─ out.println("No matching rule found.") // --- adapter: I/O ---
       ├─ out.println()
       ├─ out.println("<New Category>")
       ├─ out.print("Enter category name: ")
       ├─ name = in.nextLine()                   // blocks for user input
       │
       ├─ categoryCreation.create(name)          // --- use case ---
       │    ├─ validate name
       │    ├─ new Category(name)                //     domain
       │    ├─ repo.writeCategory(category)      //     persistence
       │    └─ return category
       │
       └─ out.printf("Category '%s' created.")   // --- adapter: I/O ---
```

## Interaction Diagram

```
 User          CLI Adapter         UseCase: Classify    UseCase: Create    Repository
  │                │                      │                   │                │
  │ classify -i    │                      │                   │                │
  │───────────────>│                      │                   │                │
  │                │  classify(txn)       │                   │                │
  │                │─────────────────────>│                   │                │
  │                │          null        │                   │                │
  │                │<─────────────────────│                   │                │
  │                │                      │                   │                │
  │ "No matching   │                      │                   │                │
  │  rule found."  │                      │                   │                │
  │<───────────────│                      │                   │                │
  │                │                      │                   │                │
  │ "Enter name: " │                      │                   │                │
  │<───────────────│                      │                   │                │
  │                │                      │                   │                │
  │ "Shopping"     │                      │                   │                │
  │───────────────>│                      │                   │                │
  │                │                      │  create("Shopping")│                │
  │                │                      │  ────────────────>│                │
  │                │                      │                   │ writeCategory()│
  │                │                      │                   │───────────────>│
  │                │                      │                   │            ok  │
  │                │                      │                   │<───────────────│
  │                │         category     │                   │                │
  │                │<─────────────────────────────────────────│                │
  │                │                      │                   │                │
  │ "Category      │                      │                   │                │
  │  'Shopping'    │                      │                   │                │
  │  created."     │                      │                   │                │
  │<───────────────│                      │                   │                │
```

---

## Layer Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│  Composition Root                                                   │
│  Buoyancy.main()                                                    │
│    constructs all objects, wires dependencies, calls adapter.run()   │
└──────────────────────────────┬──────────────────────────────────────┘
                               │ creates & injects
         ┌─────────────────────┼──────────────────────┐
         ▼                     ▼                      ▼
┌─────────────────┐  ┌──────────────────┐  ┌──────────────────────────┐
│  adapter/cli    │  │  application     │  │  infrastructure          │
│                 │  │                  │  │  /persistence             │
│ CommandLine     │  │ Categorization   │  │                          │
│ InputAdapter    │──│►UseCase          │  │ SQLiteClassification     │
│                 │  │                  │  │ RuleRepository           │
│  fields:        │  │ CategoryCreation │  │  implements:             │
│   useCase ──────│─►│►UseCase ─────────│─►│   ClassificationRule     │
│   categoryCreate│  │  field:          │  │    Repository            │
│   in (Scanner)  │  │   repo ──────────│─►│   CategoryRepository     │
│   out (Print)   │  │                  │  │                          │
└─────────────────┘  └──────────────────┘  └──────────────────────────┘
     I/O only            business logic        persistence only
 (prompts, reads         (validate, create     (SQL, jOOQ)
  input, formats          domain objects,
  output)                 call repos)
```

**Dependency rule:** arrows point inward. The adapter depends on use cases (interfaces in
`application/`). Use cases depend on repository interfaces (ports in `application/repository/`).
Infrastructure implements the ports. No layer reaches outward.

**Adapter invocation pattern:** The adapter interleaves I/O with use case calls. It never
contains business logic — it only decides _when_ to prompt and _what to display_. The use
case decides _what to do_ with the input.

```
adapter.run():
  [I/O]  ← print, read       ← adapter concern
  [CALL] ← useCase.method()  ← delegates to application layer
  [I/O]  ← print result      ← adapter concern
```

### Acknowledged trade-off: workflow orchestration in the adapter

In 7a the adapter only calls one use case on the no-match path, so no orchestration logic
leaks in. Starting in 7b, the adapter will contain branching logic like "if category selection
returns null, invoke category creation." This is workflow orchestration — an application
concern — living in the adapter. Left unchecked this leads to the **smart UI anti-pattern**,
where the adapter gradually accumulates business decisions.

**Accepted for 7a.** When the orchestrator is introduced (7b+), it should be an
application-layer use case (e.g., `RuleCreationUseCase`) that owns the workflow sequencing
and branching. The adapter would be reduced to a dumb I/O pass-through, calling the
orchestrator and relaying prompts/responses. This keeps the decision logic ("what step comes
next?") in the application layer where it can be reused across adapters (CLI, web, etc.)
without duplicating the workflow.

---

## Design

### 1. New interface — `CategoryRepository`

**File:** `src/java/net/mossworks/buoyancy/application/repository/CategoryRepository.java`

```java
public interface CategoryRepository {
    void writeCategory(Category category);
}
```

Throws `IllegalArgumentException` on duplicate ID (consistent with `ClassificationRuleRepository.writeRule`).
`listCategories()` is deferred to 7b.

### 2. `SQLiteClassificationRuleRepository` implements `CategoryRepository`

**File:** `src/java/.../infrastructure/persistence/SQLiteClassificationRuleRepository.java`

Add `public void writeCategory(Category category)`:
- Plain `INSERT INTO category` using jOOQ DSL (same schema constants as existing code)
- Throws `IllegalArgumentException("Category with ID ... already exists")` on duplicate
- The existing private `ensureCategoryExists()` is unchanged (uses `onDuplicateKeyIgnore()`)

### 3. New use case — `CategoryCreationUseCase`

**File:** `src/java/net/mossworks/buoyancy/application/CategoryCreationUseCase.java`

```java
public class CategoryCreationUseCase {
    public CategoryCreationUseCase(CategoryRepository repo) { ... }
    public Category create(String name) { ... }
}
```

`create(name)` steps:
1. Validate name is not null/blank
2. Create `new Category(name)`
3. Call `repo.writeCategory(category)`
4. Return the category

### 4. Modify `CommandLineInputAdapter`

**File:** `src/java/.../adapter/cli/CommandLineInputAdapter.java`

**New constructor signature:**
```java
public CommandLineInputAdapter(
    CategorizationUseCase useCase,           // existing
    CategoryCreationUseCase categoryCreation, // new
    String[] args,                           // existing
    Scanner in,                              // new
    PrintStream out                          // existing
)
```

**Updated `run()` — no-match branch:**
```java
// ADAPTER I/O — prompt the user
out.println("No matching rule found.");
out.println();
out.println("<New Category>");
out.print("Enter category name: ");

// ADAPTER I/O — read user input
String name = in.nextLine();

// USE CASE — delegate business logic (validate, create, persist)
Category category = categoryCreation.create(name);

// ADAPTER I/O — display result
out.printf("Category '%s' created.%n", category.getName());
```

The adapter's role is strictly I/O orchestration:
1. It decides _when_ to prompt (after a null classify result)
2. It reads raw input (Scanner)
3. It passes the input to the use case (no transformation — the string goes directly)
4. It formats the use case's return value for display

The adapter never validates the category name or constructs domain objects — that's
the use case's job.

### 5. Modify `Buoyancy.java`

**File:** `src/java/net/mossworks/buoyancy/Buoyancy.java`

- Declare `repo` as `SQLiteClassificationRuleRepository` (it implements both interfaces)
- Add `Scanner scanner = new Scanner(System.in)`
- Create `CategoryCreationUseCase categoryCreation = new CategoryCreationUseCase(repo)`
- Pass `scanner` and `categoryCreation` to `CommandLineInputAdapter`

---

## Tests

### Extend `SQLiteClassificationRuleRepositoryTest`

**File:** `src/test/java/.../infrastructure/persistence/SQLiteClassificationRuleRepositoryTest.java`

Add two tests:
1. `writeCategory_succeeds_withoutException` — writes a category, no exception thrown
2. `writeCategory_throwsException_onDuplicateId` — writes same category twice, second call throws

### New `CategoryCreationUseCaseTest`

**File:** `src/test/java/net/mossworks/buoyancy/application/CategoryCreationUseCaseTest.java`

Stub `CategoryRepository` as an anonymous class capturing written categories.

Tests:
1. `create_returnsCategory_withGivenName` — returned `Category.getName()` equals input
2. `create_persistsCategory` — stub repo records the written category
3. `create_throwsException_whenNameIsBlank` — null/blank name rejected

### Update `CommandLineInputAdapterIntegrationTest`

The existing `classify_printsNoMatch_whenNoRuleMatches` test must be updated:
- Constructor now requires `Scanner` and `CategoryRepository`
- Provide a `Scanner` pre-loaded via `ByteArrayInputStream` with a category name
- Use an in-memory stub `CategoryRepository`
- Assert output contains "No matching rule found" and "created."

---

## Files Changed

| Action | File |
|--------|------|
| Create | `src/java/.../application/repository/CategoryRepository.java` |
| Modify | `src/java/.../infrastructure/persistence/SQLiteClassificationRuleRepository.java` |
| Create | `src/java/.../application/CategoryCreationUseCase.java` |
| Modify | `src/java/.../adapter/cli/CommandLineInputAdapter.java` |
| Modify | `src/java/.../Buoyancy.java` |
| Modify | `src/test/java/.../infrastructure/persistence/SQLiteClassificationRuleRepositoryTest.java` |
| Create | `src/test/java/.../application/CategoryCreationUseCaseTest.java` |
| Modify | `src/test/java/.../adapter/cli/CommandLineInputAdapterIntegrationTest.java` |

---

## Verification

1. Run `mvn test` — all 38 existing tests plus new tests should pass
2. Build the jar and run:
   `java -jar target/buoyancy.jar classify -i "01/15/2026 AMAZON 50.00"`
   Expected (no existing rules): prompted for category name, confirmation printed
3. Check `~/.buoyancy/buoyancy.db` — `category` table contains the new row
