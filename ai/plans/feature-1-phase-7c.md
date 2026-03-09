# Feature 1 — Phase 7c Plan: Category Selection Workflow

**Status: complete**

## Context

Phase 7b established a persistent CLI with AppShell, MainMenuController, and ClassifyController.
Currently, the no-match flow only creates a new category. Phase 7c introduces a selection
widget that lets the user either pick an existing category or create a new one.

The selection widget is designed to be reusable — the same component will be used for
counterparty selection in a later sub-phase.

---

## User-Facing Flow

```
Select a Category:
┌────────────────────┬────────────────────┬──────────────────────┐
│ > gro              │   Groceries        │ <Create New Category>│
│                    │   Household        │                      │
│                    │                    │                      │
│                    │                    │                      │
└────────────────────┴────────────────────┴──────────────────────┘
[Type to filter | Shift+Right/Left: move focus | Enter: confirm]
```

### Three Panes

| Pane   | Purpose | Content |
|--------|---------|---------|
| Left   | Text input | Cursor where user types to filter |
| Middle | Item list | Filtered list of existing items |
| Right  | Create action | Single entry: `<Create New {TypeName}>` |

### Interaction Rules

1. User sees a three-pane prompt. Left pane has a text cursor; middle pane lists all items; right pane shows `<Create New {TypeName}>`.
2. As the user types, the middle pane filters to items matching the input (word-level, any order).
3. Shift+Right moves pane focus rightward. Shift+Left moves focus leftward. Focus wraps: left → middle → right → left.
4. When the middle pane is focused, Up/Down arrow keys navigate the highlighted item.
5. Enter confirms the current selection based on focused pane:
   - Left pane focused: selects the first item in the filtered middle pane list (if any).
   - Middle pane focused: selects the highlighted item.
   - Right pane focused: returns `Create` result.
6. If the middle pane list is empty, Enter triggers `Create` regardless of which pane is focused.
7. Typing only affects the left pane. Keystrokes are ignored (for text input purposes) when the middle or right pane is focused.
8. Focus is indicated by pane border/text color changing to orange. Unfocused panes use white/green.
9. Focused-pane visual feedback:
   - Left pane: blinking cursor
   - Middle pane: highlighted (inverse/bold) selection bar on the current item
   - Right pane: highlighted `<Create New {TypeName}>` entry
---

## JLine 3 Integration

### Why JLine

The current I/O model (`Scanner` + `PrintStream`) is line-buffered — we can't intercept
individual keystrokes, reposition the cursor, or redraw portions of the screen. JLine 3
provides raw terminal access, key binding, and ANSI rendering needed for the selection widget.

### Maven Dependency

```xml
<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline</artifactId>
    <version>3.30.0</version>
</dependency>
```

### Architecture — How JLine Fits Into the Layers

```
┌────────────────────────────────────────────────────────────────┐
│  Composition Root                                              │
│  Buoyancy.main()                                               │
│    - builds Terminal (JLine)                                   │
│    - passes Terminal to adapter layer                          │
│    - use cases / repos unchanged                               │
└────────────────────────────────────────────────────────────────┘
                              │
┌────────────────────────────────────────────────────────────────┐
│  Adapter (CLI)                                                 │
│                                                                │
│  AppShell ─── MainMenuController ─── ClassifyController        │
│                                        └── CategoryWorkflow    │
│                                              └── SelectionWidget │
│                                                                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  adapter/cli/widget/                                     │  │
│  │                                                          │  │
│  │  SelectableType<T>      — display metadata record        │  │
│  │    - String typeName    — e.g. "Category"                │  │
│  │    - Function<T,String> displayFn  — e.g. Category::getName│ │
│  │                                                          │  │
│  │  SelectionWidget<T>     — generic three-pane picker      │  │
│  │    - Terminal terminal  — JLine terminal (raw mode)      │  │
│  │    - List<T> items      — full item list                 │  │
│  │    - SelectableType<T> type  — display + type name       │  │
│  │    + SelectionResult<T> run()                            │  │
│  │                                                          │  │
│  │  SelectionResult<T>     — union: Selected(T) | Create(s) │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                │
│  Scanner/PrintStream → replaced with Terminal in controllers   │
│  that need the widget. Simple text I/O controllers can keep    │
│  using Terminal.writer() (a PrintWriter) for basic output.     │
├────────────────────────────────────────────────────────────────┤
│  Application                                                   │
│                                                                │
│  CategorizationUseCase         RuleCreationUseCase             │
│                                  + createCategory()            │
│                                  + listCategories() [NEW]      │
│                                                                │
│  ClassificationRuleRepository  CategoryRepository              │
│                                  + writeCategory()             │
│                                  + listCategories() [NEW]      │
├────────────────────────────────────────────────────────────────┤
│  Infrastructure                                                │
│                                                                │
│  SQLiteClassificationRuleRepository                            │
│    + listCategories()  [NEW]                                   │
└────────────────────────────────────────────────────────────────┘
```

**Key principle:** JLine `Terminal` is an adapter concern. It is created in the composition
root and injected into the adapter layer only. The application and domain layers have no
knowledge of JLine. The `SelectionWidget` is a reusable CLI component in the adapter layer.

### Scanner/PrintStream Migration

JLine's `Terminal` provides `terminal.reader()` (for raw key input) and `terminal.writer()`
(a `PrintWriter` for output). The controllers currently accept `Scanner` + `PrintStream`.

**Migration approach:**
- Replace `Scanner in` and `PrintStream out` with `Terminal terminal` in controllers
  that need the widget or raw input.
- For simple text output, use `terminal.writer()` (which is a `PrintWriter`, compatible
  with the same `println`/`printf` calls).
- For simple line input (e.g., MainMenuController menu choice), use JLine's `LineReader`
  or a `Scanner` wrapping `terminal.input()`.
- The `SelectionWidget` uses `terminal.reader()` directly for raw keystroke handling.

---

## Design

### 1. `SelectableType<T>` — Display Metadata

**File:** `src/java/net/mossworks/buoyancy/adapter/cli/widget/SelectableType.java`

Bundles the two display concerns for a selectable type into one object: how to render
an individual item, and what to call the type (for the right pane's `<Create New ...>`).

```java
public record SelectableType<T>(String typeName, Function<T, String> displayFn) {}
```

Usage at call sites:
```java
var categoryType = new SelectableType<>("Category", Category::getName);
var counterpartyType = new SelectableType<>("Counterparty", Counterparty::getName);
```

This keeps display concerns in the adapter layer. Domain objects don't need to implement
any interface — the `displayFn` extracts what the widget needs.

### 2. `SelectionWidget<T>` — Generic Three-Pane Picker

**File:** `src/java/net/mossworks/buoyancy/adapter/cli/widget/SelectionWidget.java`

A generic, reusable component. It knows nothing about categories or counterparties — it
works with any `List<T>` and a `SelectableType<T>` that describes how to display items.

```java
public class SelectionWidget<T> {
    public SelectionWidget(Terminal terminal, List<T> items,
                           SelectableType<T> type) { ... }
    public SelectionResult<T> run() { ... }
}
```

**Internal state:**
- `String inputText` — current text in the left pane
- `List<T> filteredItems` — items matching the current input
- `Pane focusedPane` — enum: `INPUT`, `LIST`, `CREATE` (left, middle, right)
- `int highlightIndex` — currently highlighted item in the middle pane

**Responsibilities:**
- Renders the three-pane UI using ANSI escape sequences
- Reads raw keystrokes via `terminal.reader()`
- Filters items as user types (word-level matching, case-insensitive, any order)
- Manages focus across three panes with Shift+Right/Left
- Returns `SelectionResult.Selected(item)` or `SelectionResult.Create(inputText)`

**Key bindings:**
| Key | Action |
|-----|--------|
| Printable chars | Append to inputText (only when INPUT pane focused) |
| Backspace | Delete last char from inputText (only when INPUT focused) |
| Shift+Right | Move focus to next pane (wraps: INPUT → LIST → CREATE → INPUT) |
| Shift+Left | Move focus to previous pane (wraps) |
| Up/Down | Move highlight in LIST pane (only when LIST focused) |
| Enter | Confirm selection (behavior depends on focused pane — see interaction rules) |

**Filtering logic (built-in):**
- Split user input into words (whitespace-delimited)
- An item matches if its display string contains all words (case-insensitive, any order)
- e.g., input "house hold" matches "Household Expenses" and "Hold Household"
- Empty input matches all items

**Rendering:**
- Title line: `Select a {typeName}:`
- Three bordered panes side by side, dividing terminal width roughly into thirds
- Focused pane border/text rendered in orange (ANSI color); unfocused in white/green
- Middle pane: highlighted item shown with inverse/bold when LIST is focused
- Right pane: `<Create New {typeName}>` shown highlighted when CREATE is focused
- Left pane: blinking cursor when INPUT is focused
- Status bar below: `[Type to filter | Shift+Right/Left: move focus | Enter: confirm]`
- Full redraw on each keystroke (clear widget region, repaint)

### 3. `SelectionResult<T>` — Result Union

**File:** `src/java/net/mossworks/buoyancy/adapter/cli/widget/SelectionResult.java`

```java
public sealed interface SelectionResult<T> {
    record Selected<T>(T item) implements SelectionResult<T> {}
    record Create<T>(String input) implements SelectionResult<T> {}
}
```

`Selected` — user picked an existing item from the list.
`Create` — user chose to create a new item. `input` contains whatever text was
in the left pane (may be empty).

### 3. `CategoryRepository.listCategories()` — New Repository Method

**File:** `src/java/net/mossworks/buoyancy/application/repository/CategoryRepository.java`

Add:
```java
List<Category> listCategories();
```

### 4. `SQLiteClassificationRuleRepository.listCategories()` — Implementation

**File:** `src/java/.../infrastructure/persistence/SQLiteClassificationRuleRepository.java`

```java
@Override
public List<Category> listCategories() {
    return dsl.selectFrom(CATEGORY)
        .orderBy(CATEGORY_NAME)
        .fetch()
        .stream()
        .map(r -> new Category(
            UUID.fromString(r.get(CATEGORY_ID)),
            r.get(CATEGORY_NAME),
            r.get(CATEGORY_SUB_CATEGORY)))
        .toList();
}
```

### 5. `RuleCreationUseCase.listCategories()` — New Use Case Method

**File:** `src/java/net/mossworks/buoyancy/application/RuleCreationUseCase.java`

```java
public List<Category> listCategories() {
    return categoryRepo.listCategories();
}
```

### 6. Refactor `ClassifyController` — Use Terminal, New Category Flow

**File:** `src/java/.../adapter/cli/classify/ClassifyController.java`

- Replace `Scanner in, PrintStream out` with `Terminal terminal`
- On no-match: load categories via `ruleCreationUseCase.listCategories()`
- If categories exist, create `SelectionWidget<Category>` and run it
- Handle `SelectionResult.Selected` (category chosen) vs `SelectionResult.Create` (new name)
- On `Create`, delegate to `CategoryCreationWorkflow`

### 7. Refactor `CategoryCreationWorkflow` — Accept Name Optionally

**File:** `src/java/.../adapter/cli/classify/CategoryCreationWorkflow.java`

Add an overload `run(String name)` that skips the prompt and directly creates
the category with the given name. This supports the flow where the user typed
a name in the selection widget that didn't match anything.

### 8. Update `MainMenuController`, `AppShell` — Use Terminal

**File:** `src/java/.../adapter/cli/MainMenuController.java`
**File:** `src/java/.../adapter/cli/AppShell.java`

Replace `Scanner in, PrintStream out` with `Terminal terminal`. Use
`terminal.writer()` for output and a `LineReader` or buffered reader for
simple line input (menu choices).

### 9. Update `Buoyancy.java` — Create Terminal

**File:** `src/java/net/mossworks/buoyancy/Buoyancy.java`

```java
Terminal terminal = TerminalBuilder.builder().system(true).build();
// ... pass terminal to controllers instead of Scanner/PrintStream
```

---

## Testing Strategy

### SelectionWidget Testing

**Note (deviation from plan):** `TerminalBuilder.builder().dumb(true)` and `.system(false)`
both create a `PosixPtyTerminal` backed by `FileOutputStream` when run in a real TTY
environment — our `ByteArrayOutputStream` is ignored. Fix: instantiate `DumbTerminal`
directly: `new org.jline.terminal.impl.DumbTerminal("test", Terminal.TYPE_DUMB, in, out, UTF_8)`.

**Note (deviation from plan):** `reader.peek(100)` in `SelectionWidget.handleEscape()` had a
race condition — JLine's background reader thread hadn't yet populated the peek buffer for the
first escape byte after entering raw mode. Fix: replaced `peek(100)` + `read()` with
`reader.read(100L)` which blocks for up to 100ms and correctly consumes the character.

JLine's `DumbTerminal` instantiated directly lets us simulate keystrokes and assert rendered output.

**File:** `src/test/java/.../adapter/cli/widget/SelectionWidgetTest.java`

Tests:
1. `run_returnsSelected_whenUserPicksFromList` — simulate Shift+Right, Enter
2. `run_returnsCreate_whenUserTypesNewName` — simulate typing + Enter
3. `run_filtersItems_asUserTypes` — verify filtered list matches input
4. `run_filtersItems_wordLevelAnyOrder` — "hold house" matches "Household"
5. `run_returnsCreate_whenListIsEmpty` — no items, user types name, Enter

### Controller Tests

Existing tests use `ByteArrayInputStream`/`ByteArrayOutputStream` with `Scanner`/`PrintStream`.
These will be updated to use JLine's dumb terminal with piped streams — same pattern, different
wrapper.

### Repository Tests

`listCategories()` tests go in the existing `SQLiteClassificationRuleRepositoryTest`:
1. `listCategories_returnsEmptyList_whenNoCategoriesExist`
2. `listCategories_returnsAllCategories_orderedByName`

---

## Files Changed

| Action | File |
|--------|------|
| Modify | `pom.xml` — add JLine 3.30.0 |
| Create | `src/java/.../adapter/cli/widget/SelectableType.java` |
| Create | `src/java/.../adapter/cli/widget/SelectionWidget.java` |
| Create | `src/java/.../adapter/cli/widget/SelectionResult.java` |
| Modify | `src/java/.../application/repository/CategoryRepository.java` — add listCategories() |
| Modify | `src/java/.../infrastructure/persistence/SQLiteClassificationRuleRepository.java` — add listCategories() |
| Modify | `src/java/.../application/RuleCreationUseCase.java` — add listCategories() |
| Modify | `src/java/.../adapter/cli/classify/ClassifyController.java` — Terminal, widget |
| Modify | `src/java/.../adapter/cli/classify/CategoryCreationWorkflow.java` — add run(String) |
| Modify | `src/java/.../adapter/cli/MainMenuController.java` — Terminal |
| Modify | `src/java/.../adapter/cli/AppShell.java` — Terminal |
| Modify | `src/java/.../Buoyancy.java` — create Terminal |
| Create | `src/test/.../adapter/cli/widget/SelectionWidgetTest.java` |
| Modify | `src/test/.../infrastructure/persistence/SQLiteClassificationRuleRepositoryTest.java` |
| Modify | `src/test/.../adapter/cli/classify/ClassifyControllerTest.java` |
| Modify | `src/test/.../adapter/cli/classify/CategoryCreationWorkflowTest.java` |
| Modify | `src/test/.../adapter/cli/MainMenuControllerTest.java` |
| Modify | `src/test/.../adapter/cli/AppShellTest.java` |

---

## Implementation Order

1. Add JLine dependency to pom.xml
2. Create `SelectableType<T>` (record, no dependencies)
3. Create `SelectionResult<T>` (sealed interface, no dependencies)
4. Create `SelectionWidget<T>` (depends on JLine Terminal, SelectableType, SelectionResult)
5. Add `listCategories()` to CategoryRepository, SQLiteRepo, RuleCreationUseCase
6. Migrate AppShell and MainMenuController from Scanner/PrintStream to Terminal
7. Migrate ClassifyController to Terminal, integrate SelectionWidget
8. Update CategoryCreationWorkflow with `run(String)` overload
9. Update Buoyancy.java to create Terminal
10. Update all tests

Steps 2-5 are independent and can be done in parallel. Steps 6-8 depend on JLine being
available. Step 9 ties it together. Step 10 runs throughout.

---

## Verification

1. `mvn test` — all tests pass
2. Run the CLI. Enter a transaction with no matching rule.
   - If categories exist: selection widget appears, filter works, Shift+Right selects
   - If no categories exist: falls through to creation workflow
3. Verify the widget is responsive and redraws correctly on keystroke input
