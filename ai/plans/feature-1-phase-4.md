# Feature 1 — Phase 4 Plan

## Pre-work: ClassificationRule package correction

During review, it was discovered that `ClassificationRule` was placed in
`net.mossworks.buoyancy.application` despite being designed as a domain entity
throughout the original session. All session imports referenced
`net.mossworks.buoyancy.domain.ClassificationRule`; the file on disk did not
match.

**Correction:** Move `ClassificationRule` to `net.mossworks.buoyancy.domain`.

### Implementation outcomes

- `domain.ClassificationRule` — rule entity; holds a memo pattern, priority,
  `AmountType` (CREDIT/DEBIT/BOTH), and a `Counterparty`. Category is resolved
  via `Counterparty.defaultCategory`.

### Known pre-existing issue

`RuleBasedTransactionClassifierIntegrationTest` calls `classifier.getRules()`,
which does not exist — `rules` has `@Setter` but no `@Getter`. The test itself
carries a `// TODO` comment acknowledging this. To be resolved in Phase 4.

---

## Phase 4 — Transaction Classification Rule Repository

### Pre-existing fix: getRules()

Add `@Getter` to the `rules` field in `RuleBasedTransactionClassifier` so the
integration test compiles. The field is already annotated `@Setter`; adding
`@Getter` is consistent and sufficient.

File: `src/java/net/mossworks/buoyancy/application/RuleBasedTransactionClassifier.java`

### Steps 1–3 status

Already completed in the previous session:
- `ClassificationRuleRepository` interface exists in `application.repository`
- `RuleBasedTransactionClassifier` accepts the repository in its constructor and has `loadRules()`
- `@Setter` on `rules` is in place

### Steps 4 & 5 — YamlFileClassificationRuleRepository

**Package:** `src/java/net/mossworks/buoyancy/infrastructure/persistence/`

**YAML library:** Add `jackson-dataformat-yaml` 2.15.2 to `pom.xml`.

**Domain annotation concern:** Domain classes must not carry Jackson annotations.
Instead, create package-private YAML DTOs in the infrastructure package and map
between them in the repository:
- `RuleYaml` — id, memoPattern, priority, amountType, counterparty
- `CounterpartyYaml` — id, name, defaultCategory
- `CategoryYaml` — id, name, subCategory

**YAML file format:**
```yaml
rules:
  - id: "550e8400-e29b-41d4-a716-446655440000"
    memoPattern: "GROCERY%"
    priority: 0
    amountType: DEBIT
    counterparty:
      id: "550e8400-e29b-41d4-a716-446655440001"
      name: "Grocery Store"
      defaultCategory:
        id: "550e8400-e29b-41d4-a716-446655440002"
        name: "Groceries"
        subCategory: null
```

**Class design:**
```
YamlFileClassificationRuleRepository implements ClassificationRuleRepository
  - final Path filePath           (mandatory constructor arg)
  - ObjectMapper mapper           (YAMLFactory, initialised in constructor)
  + List<ClassificationRule> loadRules()
  + void writeRule(ClassificationRule rule)
```

`loadRules()` — reads YAML, deserializes to DTOs, maps to domain objects.
Returns empty list if file does not exist.

`writeRule(ClassificationRule)` — loads current rules, appends new rule,
writes full list back. Creates file if it does not exist.

`writeRule` is also added to the `ClassificationRuleRepository` interface so
the application layer can depend on the abstraction (anticipates Phase 5).

### Step 6 — Unit tests

File: `src/test/java/net/mossworks/buoyancy/infrastructure/persistence/YamlFileClassificationRuleRepositoryTest.java`

Test cases:
1. `loadRules_returnsEmptyList_whenFileDoesNotExist`
2. `loadRules_returnsRules_fromValidYamlFile`
3. `writeRule_createsFile_andWritesRule`
4. `writeRule_appendsRule_toExistingFile`

Use JUnit 5 `@TempDir` to avoid file system side effects.

### Implementation outcomes

- `infrastructure.persistence.YamlFileClassificationRuleRepository` — YAML-backed
  implementation of `ClassificationRuleRepository`; mandatory `filePath`
- `infrastructure.persistence.RuleYaml`, `CounterpartyYaml`, `CategoryYaml` —
  package-private Jackson DTOs for YAML mapping
- `application.repository.ClassificationRuleRepository` — `writeRule` added
- `application.RuleBasedTransactionClassifier` — `@Getter` added to `rules`
- `pom.xml` — `jackson-dataformat-yaml` 2.15.2 added; Surefire plugin upgraded to
  2.22.2 (required for JUnit 5 support — was missing from previous session setup)
