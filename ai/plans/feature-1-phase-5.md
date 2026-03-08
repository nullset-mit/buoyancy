# Feature 1 — Phase 5 Plan

## Goal

Wire the classifier, repository, and a new use case together behind a CLI
adapter, delivering the ability to classify a single transaction from the
command line.

---

## Architecture Overview

```
Buoyancy.java (composition root)
  └─ wires everything, calls adapter.run()

adapter/cli/
  CommandLineInputAdapter  ──calls──▶  CategorizationUseCase
       (implements InputAdapter)            (application layer)

application/
  CategorizationUseCase
    - TransactionClassifier   (injected)

  port/
    InputAdapter              (interface: run())

infrastructure/persistence/
  YamlFileClassificationRuleRepository   (already exists)
```

Dependencies always point inward. The adapter knows about the use case.
The use case knows about domain objects and application-layer interfaces.
Neither knows about the other's implementation details.

---

## New Classes

### 1. `InputAdapter` — interface

**Package:** `application.port`

**Contract:**
- `void run()` — starts the adapter's interaction loop

**Purpose:** Allows the composition root to start any adapter uniformly without
knowing whether it's CLI, web, or something else.

### 2. `CategorizationUseCase`

**Package:** `application`

**Injected dependencies:**
- `TransactionClassifier` — to match transactions against rules

**Methods:**
- `Counterparty classify(UnclassifiedTransaction)` — delegates to the
  classifier; returns a Counterparty on match, null on miss

**Role:** Orchestrates the business logic. Knows *what* steps are needed but not
*how* classification or persistence work.

### 3. `CommandLineInputAdapter`

**Package:** `adapter.cli`

**Implements:** `InputAdapter`

**Injected dependencies:**
- `CategorizationUseCase` — the use case to call
- `String[] args` — command-line arguments
- `PrintStream` — for output (testable)

**CLI interface:**
```
buoyancy classify -i "03/12/2026 320*WHLFDS Grocery 57.32"
```

The `-i` flag accepts a single transaction string in the format
`mm/dd/yyyy memo amount`. CSV file input (`-csv`) deferred to Phase 7.

**Behavior (`run()`):**
1. Parses the `-i` argument into an `UnclassifiedTransaction`
   (date format: mm/dd/yyyy, memo: everything between date and amount,
   amount: final token)
2. Calls `useCase.classify(transaction)`
3. If matched: displays the counterparty/category to the user
4. If unmatched: displays "no match" to the user

Rule creation sub-workflow deferred to a later phase.

---

## Modified Classes

### 4. `Buoyancy.java` — composition root

Updated `main()` to wire and launch the application:

```
repo       = new YamlFileClassificationRuleRepository(path)
classifier = new RuleBasedTransactionClassifier(repo)
classifier.loadRules()
useCase    = new CategorizationUseCase(classifier)
adapter    = new CommandLineInputAdapter(useCase, args, System.out)
adapter.run()
```

The rules file path can initially be a hardcoded default (e.g.,
`~/.buoyancy/rules.yaml`), refined in a later phase.

### 5. Remove YAML DTO classes

Delete `CategoryYaml.java`, `CounterpartyYaml.java`, and `RuleYaml.java` from
`infrastructure.persistence`. The repository validates YAML structure on read,
and domain constructors validate required fields. The intermediate DTO layer is
unnecessary indirection.

Update `YamlFileClassificationRuleRepository` to deserialize directly into
domain objects (or use Maps/JsonNode for parsing).

---

## Package Layout After Phase 5

```
net.mossworks.buoyancy/
  Buoyancy.java
  domain/
    Category.java
    ClassificationRule.java
    Counterparty.java
    Transaction.java
  application/
    CategorizationUseCase.java
    RuleBasedTransactionClassifier.java
    TransactionClassifier.java
    dto/
      UnclassifiedTransaction.java
    port/
      InputAdapter.java
    repository/
      ClassificationRuleRepository.java
  adapter/
    cli/
      CommandLineInputAdapter.java
  infrastructure/
    persistence/
      YamlFileClassificationRuleRepository.java
```

---

## Implementation Steps

1. Remove `CategoryYaml`, `CounterpartyYaml`, `RuleYaml` and update
   `YamlFileClassificationRuleRepository` to work without them
2. Create `InputAdapter` interface in `application.port`
3. Create `CategorizationUseCase` in `application` with `classify()`, plus
   unit tests
4. Implement `RuleBasedTransactionClassifier.classify()` — the method exists
   but is currently blank; implement the memo-pattern matching logic, plus
   unit tests
5. Create `CommandLineInputAdapter` in `adapter.cli` implementing the
   interaction described above
6. Update `Buoyancy.java` to wire all components and call `adapter.run()`
7. Write an integration test that exercises the full flow with a `-i` argument
   and a pre-loaded rules file
