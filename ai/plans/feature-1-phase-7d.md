# Feature 1 — Phase 7d Plan: Counterparty Selection/Creation, Widget Create Mode

**Status: complete**

## Context

Phase 7c delivered a reusable `SelectionWidget<T>` and wired it into category
selection. However the workflow ends immediately after a category is chosen — no
counterparty is selected or created. Phase 7d adds counterparty
selection/creation using the same widget pattern, and enhances the widget with
an inline create mode so entity creation happens inside the widget itself.

The existing application structure (AppShell → MainMenuController →
ClassifyController) is preserved. The goal is to gather user feedback on the
selection/creation widget, not to restructure the application flow.

---

## Housekeeping (pre-work)

Three items to address before the main feature work:

### 1. Extract `readLine` to a base class

The `readLine()` method is duplicated in `MainMenuController`,
`ClassifyController`, and `CategoryCreationWorkflow`. Extract it to an abstract
base class that all CLI controllers extend.

**File:** `adapter/cli/AbstractController.java`

```java
public abstract class AbstractController {
    protected final Terminal terminal;

    protected AbstractController(Terminal terminal) {
        this.terminal = terminal;
    }

    protected String readLine() {
        // existing readLine implementation
    }
}
```

`MainMenuController`, `ClassifyController`, and `CategoryCreationWorkflow`
extend `AbstractController` and remove their local `readLine()` methods and
`terminal` fields.

### 2. Move `TransactionParser` to util package

`TransactionParser` is a pure parsing utility with no CLI or adapter
dependencies. Move it from `adapter/cli/classify/` to
`net.mossworks.buoyancy.util/`.

**From:** `adapter/cli/classify/TransactionParser.java`
**To:** `util/TransactionParser.java`

Update the import in `ClassifyController`.

### 3. Fix cuddled else statements

Change `} else {` to uncuddled style across the codebase:

```java
// Before:
} else {

// After:
}
else {
```

Affected files (production code only — no test files have this pattern):
- `SelectionWidget.java` (1 occurrence)
- `ClassifyController.java` (2 occurrences)

---

## User-Facing Flow

### Overall loop (unchanged)

```
Welcome to Buoyancy.

What would you like to do?
  1. Classify a transaction
  2. Exit
> 1

Enter transaction (mm/dd/yyyy memo amount): 03/15/2026 WALMART GROCERY 57.32

No matching rule found. Let's create a classification rule.

  [Category SelectionWidget appears]
  → user selects "Groceries" (or creates new via create mode)

  [Counterparty SelectionWidget appears]
  → user selects "Walmart" (or creates new via create mode)

  Counterparty: Walmart
  Category: Groceries

What would you like to do?
  ...
```

The MainMenuController and AppShell remain as-is. ClassifyController gains
counterparty selection after category selection.

### SelectionWidget — Create Mode (new behavior)

The current widget has three panes: INPUT | LIST | CREATE. When the user moves
focus to the CREATE pane and presses Enter, the widget currently returns
`SelectionResult.Create(inputText)` and the *caller* runs a separate
`CategoryCreationWorkflow` prompt. Phase 7d changes this so that entity creation
happens *inside* the widget itself:

1. When the user selects `<Create New {entity}>` and presses Enter, the widget
   transitions to **create mode**:
   - Left pane is focused (for the user to type/edit the entity name).
   - Middle pane is blank (the filtered list is hidden).
   - Right pane text changes to:
     ```
     Press Enter
     to Save.

     Esc to Cancel.
     ```
     (Each sentence on its own line, word-wrapped within the pane width, with
     a blank line between the two instructions.)

2. If the user presses **Enter** while in create mode:
   - The widget calls a supplied **save callback** (`Function<String, SaveResult>`)
     with the current input text.
   - On success: the widget returns `SelectionResult.Created(newItem)`.
   - On duplicate: the left pane border turns **red** (`\033[31m`), the middle pane
     displays `"{entity} '{name}' already exists"`, and Enter is blocked until the
     user changes the name.

3. If the user presses **Esc** while in create mode, the widget returns to
   normal selection mode (left pane focused, list restored, right pane shows
   `<Create New {entity}>`).

4. If the user changes the input text while the duplicate error is showing, the
   pane border reverts to orange and the error message clears.

### SelectionResult — Extended

```java
public sealed interface SelectionResult<T> {
    record Selected<T>(T item) implements SelectionResult<T> {}
    record Created<T>(T item) implements SelectionResult<T> {}
}
```

`Create(String input)` is removed. The old call sites that handled `Create` by
delegating to `CategoryCreationWorkflow` are replaced — the widget now handles
creation internally and returns a `Created(T)` with the persisted domain object.

---

## Design

### 1. `SelectionWidget<T>` — Create Mode Extension

**File:** `adapter/cli/widget/SelectionWidget.java`

New internal state:
- `boolean createMode = false` — true when in create mode
- `boolean duplicateError = false` — true when save was rejected as duplicate

New constructor parameter:
- `Function<String, SaveResult<T>> saveFn` — callback invoked when user confirms
  creation. Returns either `SaveResult.ok(T)` or `SaveResult.duplicate(message)`.

New sealed interface (in its own file in the widget package):

**File:** `adapter/cli/widget/SaveResult.java`

```java
public sealed interface SaveResult<T> {
    record Ok<T>(T item) implements SaveResult<T> {}
    record Duplicate<T>(String message) implements SaveResult<T> {}
}
```

**Behavior changes:**

| State | Enter | Esc | Typing |
|-------|-------|-----|--------|
| Normal, CREATE focused | Enter create mode | (ignored) | (ignored — typing only in INPUT) |
| Create mode, no error | Call saveFn → Ok → return Created; Duplicate → show error | Return to normal mode | Append to inputText; clear error if present |
| Create mode, duplicate | Blocked (Enter does nothing) | Return to normal mode | Append to inputText; clear error |

**Rendering in create mode:**
- Left pane: same as normal INPUT (shows `> {inputText}` with cursor), but border
  is **red** when `duplicateError` is true, otherwise orange.
- Middle pane: empty when no error; shows error message when duplicate.
- Right pane: shows "Press Enter\nto Save.\n\nEsc to Cancel." wrapped across
  content rows, highlighted (inverse+bold).

### 2. `CounterpartyRepository` — New Repository Interface

**File:** `application/repository/CounterpartyRepository.java`

```java
public interface CounterpartyRepository {
    void writeCounterparty(Counterparty counterparty);
    List<Counterparty> listCounterpartiesByCategory(Category category);
}
```

`listCounterpartiesByCategory` returns counterparties whose `defaultCategory`
matches the given category. This ensures the counterparty widget only shows
counterparties relevant to the selected category.

### 3. `DuplicateCounterpartyException`

**File:** `application/DuplicateCounterpartyException.java`

```java
public class DuplicateCounterpartyException extends RuntimeException {
    public DuplicateCounterpartyException(String name) {
        super("A counterparty with name '" + name + "' already exists");
    }
}
```

### 4. `SQLiteClassificationRuleRepository` — New Methods

**File:** `infrastructure/persistence/SQLiteClassificationRuleRepository.java`

Implement `CounterpartyRepository`:

- `writeCounterparty(Counterparty)`: inserts into counterparty table. Check for
  duplicate name (case-insensitive) and throw `DuplicateCounterpartyException`.
- `listCounterpartiesByCategory(Category)`: SELECT joined with category, filtered
  by category name (case-insensitive), ordered by counterparty name.

Also add a unique index on counterparty name to `schema.sql`:

```sql
CREATE UNIQUE INDEX IF NOT EXISTS idx_counterparty_name_unique
    ON counterparty (LOWER(name));
```

### 5. `RuleCreationUseCase` — Expanded

**File:** `application/RuleCreationUseCase.java`

Add `CounterpartyRepository` as a second constructor dependency. New methods:

```java
public Counterparty createCounterparty(String name, Category category) {
    Counterparty cp = new Counterparty(name, category);
    counterpartyRepo.writeCounterparty(cp);
    return cp;
}

public List<Counterparty> listCounterpartiesByCategory(Category category) {
    return counterpartyRepo.listCounterpartiesByCategory(category);
}
```

### 6. `ClassifyController` — Counterparty Selection Added

**File:** `adapter/cli/classify/ClassifyController.java`

`run()` keeps its existing structure. The no-match branch is extended:

```
// existing:
category = selectOrCreateCategory()
// new — after category is selected:
counterparty = selectOrCreateCounterparty(category)
print "Counterparty: {name}"
print "Category: {name}"
```

`selectOrCreateCategory()` changes:
- Always shows the widget (even if list is empty — the user can create inline).
- Passes a `saveFn` that calls `ruleCreationUseCase.createCategory(name)`,
  catching `DuplicateCategoryException` and returning `SaveResult.Duplicate`.
- On `Selected` → return the item.
- On `Created` → return the created item.

New `selectOrCreateCounterparty(Category category)`:
- Loads counterparties via `ruleCreationUseCase.listCounterpartiesByCategory(category)`.
- Shows `SelectionWidget<Counterparty>` with type name "Counterparty" and
  display function `Counterparty::getName`.
- Passes a `saveFn` that calls `ruleCreationUseCase.createCounterparty(name, category)`,
  catching `DuplicateCounterpartyException` and returning `SaveResult.Duplicate`.
- Same `Selected`/`Created` handling.

### 7. `CategoryCreationWorkflow` — Deleted

Its responsibilities are absorbed by the widget's create mode. The
`DuplicateCategoryException` re-prompt loop is now handled by the widget's
duplicate-error state.

### 8. `MainMenuController` / `AppShell` — Unchanged

Both remain as-is. `MainMenuController` continues to dispatch to
`ClassifyController` via the menu. The application's landing page and
navigation structure are preserved.

### 9. `Buoyancy.java` — Updated Wiring

```java
SQLiteClassificationRuleRepository repo = new SQLiteClassificationRuleRepository(dbPath);
RuleCreationUseCase ruleCreationUseCase = new RuleCreationUseCase(repo, repo); // categoryRepo, counterpartyRepo
CategorizationUseCase categorizationUseCase = ...;
ClassifyController classifyController = new ClassifyController(terminal, categorizationUseCase, ruleCreationUseCase);
MainMenuController mainMenuController = new MainMenuController(terminal, classifyController);
AppShell shell = new AppShell(terminal, mainMenuController);
```

### 10. Delete `InputAdapter`

**File:** `application/port/InputAdapter.java`

Dead interface — no implementations or references anywhere in the codebase.
Remove the file and the `port` package if it becomes empty.

---

## Schema Changes

**File:** `src/resources/schema.sql`

Add unique index on counterparty name:

```sql
CREATE UNIQUE INDEX IF NOT EXISTS idx_counterparty_name_unique
    ON counterparty (LOWER(name));
```

---

## Files Changed

| Action | File |
|--------|------|
| Create | `adapter/cli/AbstractController.java` — base class with `readLine()` |
| Move   | `adapter/cli/classify/TransactionParser.java` → `util/TransactionParser.java` |
| Delete | `application/port/InputAdapter.java` — dead interface |
| Modify | `src/resources/schema.sql` — add counterparty name unique index |
| Modify | `adapter/cli/widget/SelectionWidget.java` — create mode, saveFn, uncuddled else |
| Create | `adapter/cli/widget/SaveResult.java` — Ok / Duplicate sealed interface |
| Modify | `adapter/cli/widget/SelectionResult.java` — Replace Create with Created(T) |
| Create | `application/repository/CounterpartyRepository.java` |
| Create | `application/DuplicateCounterpartyException.java` |
| Modify | `infrastructure/persistence/SQLiteClassificationRuleRepository.java` — implement CounterpartyRepository |
| Modify | `application/RuleCreationUseCase.java` — add CounterpartyRepository, new methods |
| Modify | `adapter/cli/classify/ClassifyController.java` — counterparty selection, extend AbstractController, uncuddled else |
| Modify | `adapter/cli/MainMenuController.java` — extend AbstractController |
| Delete | `adapter/cli/classify/CategoryCreationWorkflow.java` |
| Modify | `Buoyancy.java` — updated wiring |
| Modify | tests (see Testing Strategy) |

---

## Implementation Order

1. Housekeeping: extract `AbstractController`, move `TransactionParser`, fix
   cuddled else, delete `InputAdapter`
2. Schema: add counterparty unique index
3. `SelectionResult` — replace `Create(String)` with `Created(T)`; create `SaveResult`
4. `SelectionWidget` — add create mode (saveFn, create-mode state, rendering,
   key handling)
5. `CounterpartyRepository` interface + `DuplicateCounterpartyException`
6. `SQLiteClassificationRuleRepository` — implement `CounterpartyRepository` methods
7. `RuleCreationUseCase` — add counterparty methods
8. `ClassifyController` — integrate widget create mode for categories, add
   counterparty selection
9. Delete `CategoryCreationWorkflow`
10. `Buoyancy.java` — update wiring
11. Tests throughout

Step 1 is independent housekeeping. Steps 2–4 are the widget evolution.
Steps 5–7 are the data layer. Steps 8–10 tie the new flow together.

---

## Testing Strategy

### SelectionWidget — Create Mode Tests

**File:** `SelectionWidgetTest.java` — add / modify tests:

1. `run_entersCreateMode_whenCreatePaneSelectedAndEnterPressed` — verify right pane
   shows save/cancel text, middle pane is blank
2. `run_returnsCreated_whenSaveSucceeds` — type name, Enter in create mode, saveFn
   returns Ok → verify `Created(item)` returned
3. `run_showsDuplicateError_whenSaveReturnsDuplicate` — saveFn returns Duplicate →
   verify error shown, Enter blocked
4. `run_clearsErrorAndAllowsSave_afterEditingName` — type new name after duplicate
   error → verify error clears, Enter works
5. `run_returnsToNormalMode_onEscInCreateMode` — Esc in create mode → back to
   normal selection
6. Existing tests updated: `run_returnsCreate_*` tests replaced with `Created`
   equivalents

### CounterpartyRepository Tests

**File:** `SQLiteClassificationRuleRepositoryTest.java` — add:

1. `writeCounterparty_insertsCounterparty`
2. `writeCounterparty_throwsOnDuplicateName`
3. `listCounterpartiesByCategory_returnsMatchingCounterparties`
4. `listCounterpartiesByCategory_returnsEmptyWhenNoneMatch`

### RuleCreationUseCase Tests

**File:** `RuleCreationUseCaseTest.java` — add:

1. `createCounterparty_persistsAndReturns`
2. `listCounterpartiesByCategory_delegatesToRepo`

### ClassifyController Tests

**File:** `ClassifyControllerTest.java` — update:

1. Existing match/no-match tests updated to reflect new counterparty selection step
2. `run_showsCounterpartyWidget_afterCategorySelected` — no-match flow proceeds
   to counterparty widget after category is chosen

### Deleted Test Files

- `CategoryCreationWorkflowTest.java` — deleted with `CategoryCreationWorkflow`

### Tests Unchanged

- `MainMenuControllerTest.java` — MainMenuController is preserved
- `AppShellTest.java` — AppShell is preserved

---

## Verification

1. `mvn test` — all tests pass
2. Run the CLI:
   - Main menu appears with Classify / Exit options
   - Choose Classify, enter a transaction with no matching rule
   - Category widget appears → select existing or create new via create mode
   - Counterparty widget appears → select existing or create new
   - Confirmation printed, returns to main menu
   - Verify create mode: duplicate name shows red border and error, editing
     name clears the error, Esc returns to selection mode

---

## Open Questions / Deferred

- **Classification rule creation** (memo pattern, amount type, persistence) is
  specified in the feature file but not yet part of this phase. Phase 7d
  establishes the counterparty+category selection pipeline. A subsequent phase
  will prompt for the memo pattern and persist the full `ClassificationRule`.
- **Sub-category creation** — feature file says "For now, sub-categories will be
  blank." No change needed.

---

## Responses to Developer's Notes

### MainMenuController

Agreed — MainMenuController is retained. The original plan was over-aggressive in
removing it. The application needs a landing page, and as more features are added
(budget management, savings goals) the main menu will grow. ClassifyController
remains a sub-controller launched from the menu.

### readLine base class

Agreed — added as housekeeping step 1. `AbstractController` provides `readLine()`
and holds the `Terminal` reference. All three controllers that currently duplicate
this method will extend it.

### TransactionParser location

Agreed — added as housekeeping step 1. It's a pure parsing utility with no
adapter dependencies. Moving to `net.mossworks.buoyancy.util` makes it available
to any adapter without implying a CLI dependency.

### Cuddled else

Agreed — added as housekeeping step 1. Three occurrences in production code
(SelectionWidget, ClassifyController x2). No occurrences in test code. Will be
fixed and adopted as a project style rule going forward.

### Scope

Understood. The plan is revised to preserve the existing application structure
(MainMenuController, AppShell) and focus narrowly on: (a) enhancing the widget
with create mode, (b) adding counterparty selection/creation to the no-match
flow, and (c) the housekeeping items.
