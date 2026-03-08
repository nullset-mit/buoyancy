# Feature 1 — Phase 7b Plan: Persistent CLI and Controller Architecture

## Context

Phase 7a introduced `CategoryCreationWorkflow` in the adapter layer, mixing I/O prompts
with direct repository calls. The current app is a one-shot CLI: parse args, classify, exit.

Phase 7b introduces:
1. A persistent interactive CLI (AppShell + MainMenuController)
2. The controller layer that separates I/O from orchestration
3. `RuleCreationUseCase` in the application layer to own business logic

This is a structural refactoring that sets up the framework for the remaining Phase 7 sub-phases
(counterparty creation, rule assembly, etc.) without accumulating SmartUI debt.

---

## User-Facing Flow (7b)

```
Welcome to Buoyancy.

What would you like to do?
  1. Classify a transaction
  2. Exit
> 1

Enter transaction (mm/dd/yyyy memo amount): 01/15/2026 AMAZON 50.00

No matching rule found. Let's create a classification rule.
Enter category name: Shopping
Category 'Shopping' created.

What would you like to do?
  1. Classify a transaction
  2. Exit
> 2

Goodbye.
```

After the category creation the app returns to the main menu (workflow is still incomplete —
counterparty and rule creation come in later sub-phases).

---

## Architecture

### SmartUI Boundary

- **Adapter (controllers/workflows):** what to prompt, in what order, what text to display
- **Application (use cases):** validation, domain object construction, persistence decisions

The controller gathers raw strings via prompts, then hands them to the use case in a single
call. A web adapter would collect the same strings from a form and make the same call.

### Layer Diagram

```
┌────────────────────────────────────────────────────────────────┐
│  Composition Root                                              │
│  Buoyancy.main() → constructs AppShell, calls shell.run()      │
└────────────────────────────────────────────────────────────────┘
                              │
┌────────────────────────────────────────────────────────────────┐
│  Adapter (CLI)                                                 │
│                                                                │
│  AppShell                    — welcome/goodbye, delegates to    │
│    └── MainMenuController      MainMenuController              │
│          └── ClassifyController                                │
│                └── CategoryCreationWorkflow  (gathers input)   │
├────────────────────────────────────────────────────────────────┤
│  Application                                                   │
│                                                                │
│  CategorizationUseCase        RuleCreationUseCase              │
│    "does this match?"           "create category/rule"          │
│         │                            │                         │
│         ▼                            ▼                         │
│  ClassificationRuleRepository   CategoryRepository             │
├────────────────────────────────────────────────────────────────┤
│  Infrastructure                                                │
│                                                                │
│  SQLiteClassificationRuleRepository (implements both repos)    │
└────────────────────────────────────────────────────────────────┘
```

### Sequence Diagram — No-Match Flow

```
User     MainMenu   ClassifyCtrl   CategorizationUC   RuleCreationUC   Repo
 │          │            │               │                  │            │
 │  "1"     │            │               │                  │            │
 │─────────>│            │               │                  │            │
 │          │  run(txn)  │               │                  │            │
 │          │───────────>│  classify(tx)  │                  │            │
 │          │            │──────────────>│                  │            │
 │          │            │      null     │                  │            │
 │          │            │<──────────────│                  │            │
 │          │            │               │                  │            │
 │ "No matching rule"    │               │                  │            │
 │<──────────────────────│               │                  │            │
 │ "Enter category:"    │               │                  │            │
 │<──────────────────────│               │                  │            │
 │ "Shopping"            │               │                  │            │
 │──────────────────────>│               │                  │            │
 │          │            │  createCategory("Shopping")      │            │
 │          │            │─────────────────────────────────>│            │
 │          │            │               │                  │  write()   │
 │          │            │               │                  │───────────>│
 │          │            │          category                │            │
 │          │            │<─────────────────────────────────│            │
 │ "Category created."  │               │                  │            │
 │<──────────────────────│               │                  │            │
 │          │   return   │               │                  │            │
 │          │<───────────│               │                  │            │
 │ "What would you like" │               │                  │            │
 │<─────────│            │               │                  │            │
```

---

## Design

### 1. New class — `RuleCreationUseCase`

**File:** `src/java/net/mossworks/buoyancy/application/RuleCreationUseCase.java`

```java
public class RuleCreationUseCase {
    public RuleCreationUseCase(CategoryRepository categoryRepo) { ... }
    public Category createCategory(String name) { ... }
}
```

`createCategory(name)`:
1. Create `new Category(name)` — domain constructor validates name
2. Call `categoryRepo.writeCategory(category)`
3. Return the category

Additional methods (`createCounterparty`, `createRule`) will be added in later sub-phases.
The use case grows as the workflow grows, but all business logic stays here.

### 2. Refactor `CategoryCreationWorkflow`

**File:** `src/java/net/mossworks/buoyancy/adapter/cli/classify/CategoryCreationWorkflow.java`

Move from `adapter/cli/` to `adapter/cli/classify/` subpackage.

Change constructor to accept `RuleCreationUseCase` instead of `CategoryRepository`:

```java
public class CategoryCreationWorkflow {
    public CategoryCreationWorkflow(PrintStream out, Scanner in, RuleCreationUseCase useCase) { ... }
    public Category run() { ... }
}
```

`run()`:
1. `out.print("Enter category name: ")`
2. `String name = in.nextLine()`
3. `Category category = useCase.createCategory(name)` — delegates business logic
4. `out.println("Category '" + name + "' created.")`
5. Return the category

### 3. New class — `TransactionParser`

**File:** `src/java/net/mossworks/buoyancy/adapter/cli/classify/TransactionParser.java`

Extracted from `CommandLineInputAdapter.parse()`:

```java
public class TransactionParser {
    private static final DateTimeFormatter DATE_FORMAT =
        DateTimeFormatter.ofPattern("MM/dd/yyyy");

    public static UnclassifiedTransaction parse(String input) { ... }
}
```

Static utility method. Same parsing logic currently in `CommandLineInputAdapter`:
date (first token), amount (last token), memo (everything between).
Throws `IllegalArgumentException` on invalid input.

### 4. New class — `ClassifyController`

**File:** `src/java/net/mossworks/buoyancy/adapter/cli/classify/ClassifyController.java`

```java
public class ClassifyController {
    public ClassifyController(PrintStream out, Scanner in,
                              CategorizationUseCase categorizationUseCase,
                              RuleCreationUseCase ruleCreationUseCase) { ... }
    public void run() { ... }
}
```

`run()`:
1. `out.print("Enter transaction (mm/dd/yyyy memo amount): ")`
2. Read line, parse via `TransactionParser.parse(input)` into `UnclassifiedTransaction`
3. `Counterparty result = categorizationUseCase.classify(transaction)`
4. If match: print counterparty/category
5. If no match: print "No matching rule found.", run `CategoryCreationWorkflow`

### 5. New class — `MainMenuController`

**File:** `src/java/net/mossworks/buoyancy/adapter/cli/MainMenuController.java`

```java
public class MainMenuController {
    public MainMenuController(PrintStream out, Scanner in,
                              ClassifyController classifyController) { ... }
    public void run() { ... }
}
```

`run()` is a loop:
1. Print menu options (Classify / Exit)
2. Read choice
3. Dispatch to `classifyController.run()` or return
4. Loop back to menu after controller returns

### 6. New class — `AppShell`

**File:** `src/java/net/mossworks/buoyancy/adapter/cli/AppShell.java`

```java
public class AppShell {
    public AppShell(PrintStream out, MainMenuController mainMenuController) { ... }
    public void run() { ... }
}
```

`run()`:
1. Print welcome message
2. Call `mainMenuController.run()`
3. Print goodbye on return

### 7. Modify `Buoyancy.java`

**File:** `src/java/net/mossworks/buoyancy/Buoyancy.java`

Replace the current `CommandLineInputAdapter` wiring with:
1. Construct `SQLiteClassificationRuleRepository`
2. Construct `RuleBasedTransactionClassifier`, load rules
3. Construct `CategorizationUseCase`
4. Construct `RuleCreationUseCase`
5. Construct `ClassifyController` with use cases, `Scanner`, `PrintStream`
6. Construct `MainMenuController` with `ClassifyController`, `Scanner`, `PrintStream`
7. Construct `AppShell` with `PrintStream` and `MainMenuController`
8. Call `shell.run()`

### 8. Retire `CommandLineInputAdapter`

**File:** `src/java/net/mossworks/buoyancy/adapter/cli/CommandLineInputAdapter.java`

Delete. Its responsibilities are split:
- Transaction parsing → `TransactionParser`
- No-match workflow → `ClassifyController` + `CategoryCreationWorkflow`
- I/O → distributed across controllers
- `InputAdapter` interface is no longer used (can remain for now)

---

## Tests

### New `RuleCreationUseCaseTest`

**File:** `src/test/java/net/mossworks/buoyancy/application/RuleCreationUseCaseTest.java`

Stub `CategoryRepository` as lambda capturing written categories.

Tests:
1. `createCategory_returnsCategory_withGivenName`
2. `createCategory_persistsCategory`
3. `createCategory_throwsException_whenNameIsBlank` — domain constructor rejects

### Update `CategoryCreationWorkflowTest`

**File:** `src/test/java/net/mossworks/buoyancy/adapter/cli/classify/CategoryCreationWorkflowTest.java`

Move to `classify/` subpackage. Update to inject `RuleCreationUseCase` (with stubbed repo)
instead of raw `CategoryRepository`. Same four tests, updated constructor.

### New `TransactionParserTest`

**File:** `src/test/java/net/mossworks/buoyancy/adapter/cli/classify/TransactionParserTest.java`

Migrated from `CommandLineInputAdapterTest` — same parsing tests:
1. `parse_extractsDateMemoAndAmount`
2. `parse_handlesMultiWordMemo`
3. `parse_throwsException_forInvalidDate`
4. `parse_throwsException_forInvalidAmount`
5. `parse_throwsException_forTooFewTokens`
6. `parse_throwsException_forBlankInput`

### New `ClassifyControllerTest`

**File:** `src/test/java/net/mossworks/buoyancy/adapter/cli/classify/ClassifyControllerTest.java`

Uses `ByteArrayInputStream`/`ByteArrayOutputStream` for I/O. Stub use cases.

Tests:
1. `run_printsCounterpartyAndCategory_whenRuleMatches`
2. `run_startsRuleCreationWorkflow_whenNoMatch`
3. `run_printsError_whenTransactionFormatInvalid`

### New `MainMenuControllerTest`

**File:** `src/test/java/net/mossworks/buoyancy/adapter/cli/MainMenuControllerTest.java`

Tests:
1. `run_exitsOnExitChoice`
2. `run_dispatchesToClassify_onClassifyChoice`
3. `run_printsUnknownOption_onInvalidChoice`
4. `run_loopsBackToMenu_afterControllerReturns`

### New `AppShellTest`

**File:** `src/test/java/net/mossworks/buoyancy/adapter/cli/AppShellTest.java`

Tests:
1. `run_printsWelcomeAndGoodbye`

### Delete old tests

- Delete `CommandLineInputAdapterTest.java`
- Delete `CommandLineInputAdapterIntegrationTest.java`

The parse tests move to `TransactionParserTest`. The integration scenarios move to
`MainMenuControllerTest` / `ClassifyControllerTest`.

---

## Files Changed

| Action | File |
|--------|------|
| Create | `src/java/.../application/RuleCreationUseCase.java` |
| Create | `src/java/.../adapter/cli/AppShell.java` |
| Create | `src/java/.../adapter/cli/MainMenuController.java` |
| Create | `src/java/.../adapter/cli/classify/ClassifyController.java` |
| Create | `src/java/.../adapter/cli/classify/TransactionParser.java` |
| Move   | `src/java/.../adapter/cli/CategoryCreationWorkflow.java` → `.../classify/` |
| Modify | `src/java/.../Buoyancy.java` |
| Delete | `src/java/.../adapter/cli/CommandLineInputAdapter.java` |
| Create | `src/test/.../application/RuleCreationUseCaseTest.java` |
| Create | `src/test/.../adapter/cli/AppShellTest.java` |
| Create | `src/test/.../adapter/cli/MainMenuControllerTest.java` |
| Create | `src/test/.../adapter/cli/classify/TransactionParserTest.java` |
| Create | `src/test/.../adapter/cli/classify/ClassifyControllerTest.java` |
| Move   | `src/test/.../adapter/cli/CategoryCreationWorkflowTest.java` → `.../classify/` |
| Delete | `src/test/.../adapter/cli/CommandLineInputAdapterTest.java` |
| Delete | `src/test/.../adapter/cli/CommandLineInputAdapterIntegrationTest.java` |

---

## Verification

1. `mvn test` — all tests pass (old test count replaced, not additive)
2. `mvn compile exec:java` — interactive menu appears, classify triggers no-match workflow,
   exit returns cleanly
3. Verify `~/.buoyancy/buoyancy.db` → `category` table has new row after workflow
