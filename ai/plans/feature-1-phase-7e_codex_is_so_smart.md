# Feature 1 — Phase 7e Plan: Complete Rule Creation Workflow and Architectural Reset

## Status

Proposed replacement plan.

---

## Purpose

Feature 1 is not complete until an unmatched transaction can produce a persisted
classification rule. The earlier phase plans added infrastructure, controllers,
and terminal UI pieces, but they drifted away from the actual product outcome.

This plan resets the architecture around the real workflow:

1. classify a transaction
2. if matched, show the result
3. if unmatched, guide the user through creating a new rule
4. persist that rule
5. make the new rule immediately effective for future classifications

This document should be treated as the authoritative direction for the rest of
Feature 1.

---

## Confirmed Product Constraints

These constraints are treated as settled for this feature:

- The current persistent terminal shell is the authoritative product surface.
- Feature 1 must classify and persist new rules end-to-end.
- SQLite is the only persistence target that matters for future architecture.
- YAML is vestigial and should not influence future design decisions.
- Category names are unique business keys.
- Counterparty names are unique business keys.
- A counterparty is globally unique by name.
- A counterparty has exactly one default category.
- Classification derives category through `Counterparty.defaultCategory`.
- This model is intentionally approximate; reality may be more complex, but this
  is sufficient for summarizing expenses.
- `amountType` is inferred automatically from the transaction sign:
  - positive amount => `DEBIT`
  - negative amount => `CREDIT`
- Category and counterparty creation are not standalone product capabilities yet;
  they are subordinate steps inside rule creation.
- Terminal UX should stay in a middle path: interactive and decent, but not
  overbuilt at the expense of core workflow completion.

---

## Core Architectural Correction

The application must be organized around a single business workflow, not around
separate CRUD-like use cases or an expanding collection of UI components.

### Wrong center of gravity

The previous plans centered the work on:

- adapter churn (`InputAdapter` -> controllers -> widget)
- table-shaped repository interfaces
- partial workflow slices (category only, then category picker, then
  counterparty picker)

That approach creates architectural motion without delivering the feature.

### Correct center of gravity

The system should instead center on one application workflow:

`ClassifyTransactionOrCreateRule`

This workflow owns the business sequence for the no-match path.

The adapter should gather input and display output.
The application layer should own decisions, sequencing, validation, and save
behavior.
The persistence layer should support that workflow atomically.

---

## Target Architecture

### Composition Root

`Buoyancy.main()`

Responsibilities:

- bootstrap the database/schema
- construct repositories/services/controllers
- construct the terminal shell
- start the shell

### Adapter Layer

Keep:

- `AppShell`
- `MainMenuController`
- `ClassifyController`

Adapter responsibilities:

- prompt for transaction input
- display matches
- present options when no match exists
- gather user choices and free-text input
- display success/failure messages

Adapter non-responsibilities:

- deciding business sequencing beyond user interaction flow
- constructing domain policy
- managing persistence invariants

### Application Layer

Introduce one workflow-oriented service as the primary orchestration boundary.

Recommended name:

- `RuleCreationWorkflowService`

This service should own the no-match business flow. It may expose smaller
methods for the controller to call step-by-step, but those methods must be
organized around the workflow, not around tables.

Possible responsibilities:

- list existing categories
- create category if needed
- list existing counterparties relevant to a chosen category
- create counterparty if needed
- derive a proposed memo pattern from the unmatched transaction memo
- infer `amountType` from the transaction amount
- persist the final `ClassificationRule`
- refresh in-memory classifier state if the classifier remains cached

Retain `CategorizationUseCase` or rename it if desired, but its role remains
simple: classify a transaction against existing rules.

### Persistence Layer

Stop optimizing the architecture around separate repositories for each table.
The application should depend on persistence capabilities that match the
workflow.

Recommended direction:

- keep repository interfaces if useful
- but shape them around business operations, not schema boundaries

Examples:

- `findCategoryByName`
- `listCategories`
- `findCounterpartyByName`
- `listCounterpartiesForCategory`
- `saveClassificationRule`

The persistence implementation can still be backed by SQLite and jOOQ, but the
application layer should not be forced to think in terms of table-by-table write
calls.

---

## Domain Model Direction

For Feature 1, keep the current simplified domain model:

- `Category`
  - unique by name
- `Counterparty`
  - unique by name
  - has one `defaultCategory`
- `ClassificationRule`
  - points to a `Counterparty`
  - category is derived from the counterparty's default category
  - includes `memoPattern`, `priority`, and `amountType`

This means the model deliberately does **not** support one counterparty mapping
to multiple categories. That is accepted product scope for now.

The plan should explicitly acknowledge that limitation instead of pretending the
current schema supports both behaviors.

---

## Workflow Design

### Happy Path: Match Exists

1. User chooses "Classify a transaction"
2. User enters transaction text
3. App parses transaction
4. `CategorizationUseCase` classifies it
5. App displays counterparty and category
6. Control returns to main menu

### No-Match Path: Create Rule

1. User enters a transaction
2. Classification returns no match
3. App enters rule creation workflow
4. User selects or creates a category
5. User selects or creates a counterparty for that category
6. App derives a proposed memo pattern from the transaction memo
7. App infers `amountType` from the transaction sign
8. User confirms or edits the memo pattern
9. App persists the new `ClassificationRule`
10. App refreshes classifier state or otherwise makes the new rule visible
11. App displays confirmation
12. Control returns to main menu

### Memo Pattern Rule

For this phase, the system should derive an initial memo pattern from the
transaction memo and allow the user to accept or edit it before save.

The derivation does not need to be sophisticated yet. A simple, deterministic
initial pattern is sufficient for Phase 7e.

Examples of acceptable first-pass behavior:

- use the raw memo as the initial pattern
- or append/prepend `%` in a consistent way

The key point is that the user should not be forced to type the pattern from
scratch when the source transaction already contains the seed value.

### Amount Type Rule

For this phase:

- amount > 0 => `DEBIT`
- amount < 0 => `CREDIT`

No prompt is required.

---

## Structural Changes Required

### 1. Replace the current `RuleCreationUseCase` shape

The current class is too close to a pass-through CRUD façade.

Replace or refactor it into a workflow-oriented service that reflects the
business process.

Bad shape:

- `createCategory(name)`
- `listCategories()`
- `createCounterparty(name, category)`
- `listCounterpartiesByCategory(category)`

Better shape:

- methods grouped around rule creation
- explicit save/finalize step
- workflow state kept coherent at the application layer

### 2. Add explicit rule persistence to the no-match flow

The current implementation stops after category/counterparty selection.
That is not acceptable for Feature 1.

The controller/workflow must continue until a `ClassificationRule` is persisted.

### 3. Move startup concerns out of repository construction

Database bootstrap concerns should not keep accumulating in repository
constructors.

Create an explicit bootstrap step in `Buoyancy.main()` or a small helper used by
the composition root.

Bootstrap responsibilities:

- ensure database directory exists
- open/create database
- initialize schema
- set any jOOQ-related startup configuration

Repository responsibilities:

- execute persistence operations

### 4. Stop planning around YAML

Do not design future application architecture around the YAML repository.
If ports remain, they remain because they are useful boundaries, not because
YAML still matters.

### 5. Treat category/counterparty creation as internal sub-steps

Do not plan separate product slices around "category creation" and
"counterparty creation" as if they were independent features.

For now they exist only to support rule creation.

### 6. Defer further widget ambition until the workflow is complete

The existing selection widget may remain if it is already implemented and
working, but no additional terminal UI complexity should be prioritized ahead of
the full save path.

Rule:

- no new widget features until end-to-end rule creation works

---

## Concrete Implementation Plan

### Step 1 — Write an architecture note into the codebase

Add a short design note or update an existing planning document to state:

- the persistent shell is the current target
- the one-counterparty-one-default-category limitation is intentional
- Feature 1 is only complete when rule creation persists a rule
- SQLite is the only persistence target that matters now

This prevents future plan drift.

### Step 2 — Add application workflow support for finalizing a new rule

Introduce a workflow-oriented application service that can:

- accept the unmatched transaction context
- accept the chosen/created category
- accept the chosen/created counterparty
- derive `amountType`
- propose and accept a final memo pattern
- persist the resulting rule

This is the key architectural change.

### Step 3 — Add repository capability to save the final rule cleanly

Ensure the persistence layer supports the final save path in one coherent
operation.

Requirements:

- use business-key lookups by name where appropriate
- preserve counterparty uniqueness by name
- preserve category uniqueness by name
- save `ClassificationRule`
- ensure the saved rule is visible to subsequent classification calls

### Step 4 — Extend the no-match controller flow to completion

Modify `ClassifyController` so the no-match branch does all of the following:

- select/create category
- select/create counterparty
- present derived memo pattern for confirmation or editing
- save rule
- show success message

Do not stop after printing category and counterparty.

### Step 5 — Refresh classifier state after save

If `RuleBasedTransactionClassifier` remains an in-memory cache of rules, reload
it after the rule is saved.

Alternative:

- remove the need for manual reload by changing how classification reads rules

For Phase 7e, a simple reload is acceptable.

### Step 6 — Rationalize application interfaces

After the full workflow works, clean up ports/use cases so they reflect the
actual architecture.

This may include:

- deleting or reshaping redundant table-level repository interfaces
- renaming `RuleCreationUseCase`
- narrowing controller dependencies

Do this after functionality is complete, not before.

### Step 7 — Only then decide what to do about the widget

Once the end-to-end workflow is complete:

- keep the widget if it materially improves selection UX
- otherwise simplify to plain prompts or numbered choices

The widget is an adapter detail, not the architectural center of Feature 1.

---

## Suggested File-Level Direction

### Keep

- `src/java/net/mossworks/buoyancy/Buoyancy.java`
- `src/java/net/mossworks/buoyancy/adapter/cli/AppShell.java`
- `src/java/net/mossworks/buoyancy/adapter/cli/MainMenuController.java`
- `src/java/net/mossworks/buoyancy/adapter/cli/classify/ClassifyController.java`
- `src/java/net/mossworks/buoyancy/application/CategorizationUseCase.java`
- `src/java/net/mossworks/buoyancy/application/RuleBasedTransactionClassifier.java`
- `src/java/net/mossworks/buoyancy/infrastructure/persistence/SQLiteClassificationRuleRepository.java`

### Refactor

- `src/java/net/mossworks/buoyancy/application/RuleCreationUseCase.java`
  - refactor into workflow-oriented application service

- `src/java/net/mossworks/buoyancy/infrastructure/persistence/SQLiteClassificationRuleRepository.java`
  - support final rule save path coherently
  - remove growing startup/bootstrap concerns from constructor

- `src/java/net/mossworks/buoyancy/adapter/cli/classify/ClassifyController.java`
  - finish the no-match workflow through rule persistence

### Do Not Prioritize Further Right Now

- more widget sophistication
- YAML support work
- standalone category management
- standalone counterparty management

---

## Testing Strategy

Tests should now align with real feature completion, not partial scaffolding.

### Application Tests

Add tests for the workflow-oriented rule creation service:

- creates a rule from unmatched transaction context
- infers `amountType` correctly from sign
- uses chosen category/counterparty correctly
- persists rule successfully
- rejects invalid memo pattern input if the user clears it

### Persistence Tests

Ensure repository tests cover:

- category uniqueness by name
- counterparty uniqueness by name
- saving a new rule after creating/selecting category and counterparty
- reloading rules after save
- classification works using the newly saved rule

### Controller Tests

The no-match integration path should verify the complete flow:

1. parse transaction
2. classify => no match
3. choose/create category
4. choose/create counterparty
5. confirm/edit memo pattern
6. save rule
7. success message shown

### End-to-End Test

Add at least one integration test for the full user outcome:

- start with empty or non-matching rules
- create a rule from an unmatched transaction
- classify the same transaction again
- verify it now matches

That is the real acceptance test for Feature 1.

---

## Definition of Done for Feature 1

Feature 1 is done when all of the following are true:

- the app can classify a transaction from the persistent shell
- when no rule matches, the app can guide the user through creating one
- the new rule is persisted to SQLite
- the new rule becomes effective for future classification
- the architecture reflects a workflow-centered application design rather than
  UI-centered orchestration

Anything short of that is partial progress, not feature completion.

---

## Non-Goals for Phase 7e

These items are explicitly out of scope unless needed to complete the rule save
path:

- web adapter design
- agent/autonomous orchestration design
- schema generalization for future unknown use cases
- rich category/counterparty management screens
- advanced memo pattern generation heuristics
- modeling one counterparty mapping to multiple categories

---

## Summary

The corrected direction is simple:

- stop expanding the shell before the feature exists
- put the business workflow in the application layer
- persist the rule end-to-end
- keep terminal UX pragmatic
- treat SQLite as the real persistence target
- make the system coherent before making it fancy

