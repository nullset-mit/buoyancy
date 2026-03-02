package net.mossworks.buoyancy.infrastructure.persistence;

import net.mossworks.buoyancy.domain.Category;
import net.mossworks.buoyancy.domain.ClassificationRule;
import net.mossworks.buoyancy.domain.Counterparty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class YamlFileClassificationRuleRepositoryTest {

    @TempDir
    Path tempDir;

    private ClassificationRule sampleRule(String memoPattern) {
        Category category = new Category("Groceries");
        Counterparty counterparty = new Counterparty("Grocery Store", category);
        return new ClassificationRule(memoPattern, counterparty, ClassificationRule.AmountType.DEBIT);
    }

    private ClassificationRule sampleRuleWithId(UUID id, String memoPattern) {
        Category category = new Category("Groceries");
        Counterparty counterparty = new Counterparty("Grocery Store", category);
        return new ClassificationRule(id, memoPattern, 0, counterparty, ClassificationRule.AmountType.DEBIT);
    }

    @Test
    public void loadRules_returnsEmptyList_whenFileDoesNotExist() {
        Path nonExistent = tempDir.resolve("no-such-file.yaml");
        YamlFileClassificationRuleRepository repo = new YamlFileClassificationRuleRepository(nonExistent);

        List<ClassificationRule> rules = repo.loadRules();

        assertNotNull(rules);
        assertTrue(rules.isEmpty());
    }

    @Test
    public void writeRule_createsFile_andWritesRule() {
        Path file = tempDir.resolve("rules.yaml");
        YamlFileClassificationRuleRepository repo = new YamlFileClassificationRuleRepository(file);
        ClassificationRule rule = sampleRule("GROCERY%");

        repo.writeRule(rule);

        assertTrue(file.toFile().exists());
        List<ClassificationRule> loaded = repo.loadRules();
        assertEquals(1, loaded.size());
        assertEquals(rule.getId(), loaded.get(0).getId());
        assertEquals(rule.getMemoPattern(), loaded.get(0).getMemoPattern());
        assertEquals(rule.getAmountType(), loaded.get(0).getAmountType());
        assertEquals(rule.getCounterparty().getName(), loaded.get(0).getCounterparty().getName());
        assertEquals(rule.getCounterparty().getDefaultCategory().getName(),
                loaded.get(0).getCounterparty().getDefaultCategory().getName());
    }

    @Test
    public void writeRule_appendsRule_toExistingFile() {
        Path file = tempDir.resolve("rules.yaml");
        YamlFileClassificationRuleRepository repo = new YamlFileClassificationRuleRepository(file);
        ClassificationRule rule1 = sampleRule("GROCERY%");
        ClassificationRule rule2 = sampleRule("RESTAURANT%");

        repo.writeRule(rule1);
        repo.writeRule(rule2);

        List<ClassificationRule> loaded = repo.loadRules();
        assertEquals(2, loaded.size());
        assertEquals(rule1.getId(), loaded.get(0).getId());
        assertEquals(rule2.getId(), loaded.get(1).getId());
    }

    @Test
    public void loadRules_returnsRules_fromValidYamlFile() {
        Path file = tempDir.resolve("rules.yaml");
        YamlFileClassificationRuleRepository repo = new YamlFileClassificationRuleRepository(file);
        ClassificationRule rule = sampleRule("ELECTRIC BILL%");
        repo.writeRule(rule);

        // Load with a fresh repository instance to confirm data round-trips via file
        YamlFileClassificationRuleRepository freshRepo = new YamlFileClassificationRuleRepository(file);
        List<ClassificationRule> loaded = freshRepo.loadRules();

        assertEquals(1, loaded.size());
        assertEquals(rule.getId(), loaded.get(0).getId());
        assertEquals(rule.getMemoPattern(), loaded.get(0).getMemoPattern());
        assertEquals(rule.getPriority(), loaded.get(0).getPriority());
        assertEquals(rule.getAmountType(), loaded.get(0).getAmountType());
        assertEquals(rule.getCounterparty().getId(), loaded.get(0).getCounterparty().getId());
        assertEquals(rule.getCounterparty().getDefaultCategory().getId(),
                loaded.get(0).getCounterparty().getDefaultCategory().getId());
    }

    @Test
    public void writeRule_throwsException_whenDuplicateId() {
        Path file = tempDir.resolve("rules.yaml");
        YamlFileClassificationRuleRepository repo = new YamlFileClassificationRuleRepository(file);
        UUID sharedId = UUID.randomUUID();
        ClassificationRule rule1 = sampleRuleWithId(sharedId, "GROCERY%");
        ClassificationRule rule2 = sampleRuleWithId(sharedId, "RESTAURANT%");

        repo.writeRule(rule1);
        assertThrows(IllegalArgumentException.class, () -> repo.writeRule(rule2));

        List<ClassificationRule> loaded = repo.loadRules();
        assertEquals(1, loaded.size());
    }

    @Test
    public void loadRules_throwsException_whenRequiredFieldMissing() throws IOException {
        Path file = tempDir.resolve("rules.yaml");
        // Rule missing memoPattern field
        Files.writeString(file, String.join("\n",
            "rules:",
            "  - id: \"550e8400-e29b-41d4-a716-446655440000\"",
            "    priority: 0",
            "    amountType: DEBIT",
            "    counterparty:",
            "      id: \"550e8400-e29b-41d4-a716-446655440001\"",
            "      name: \"Store\"",
            "      defaultCategory:",
            "        id: \"550e8400-e29b-41d4-a716-446655440002\"",
            "        name: \"Groceries\""
        ));
        YamlFileClassificationRuleRepository repo = new YamlFileClassificationRuleRepository(file);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, repo::loadRules);
        assertTrue(ex.getMessage().contains("memoPattern"), "Error should identify the missing field");
        assertTrue(ex.getMessage().contains("Rule at index 0"), "Error should identify the rule index");
    }

    @Test
    public void loadRules_throwsException_whenFieldHasWrongType() throws IOException {
        Path file = tempDir.resolve("rules.yaml");
        // priority should be a number, not text
        Files.writeString(file, String.join("\n",
            "rules:",
            "  - id: \"550e8400-e29b-41d4-a716-446655440000\"",
            "    memoPattern: \"GROCERY%\"",
            "    priority: \"not-a-number\"",
            "    amountType: DEBIT",
            "    counterparty:",
            "      id: \"550e8400-e29b-41d4-a716-446655440001\"",
            "      name: \"Store\"",
            "      defaultCategory:",
            "        id: \"550e8400-e29b-41d4-a716-446655440002\"",
            "        name: \"Groceries\""
        ));
        YamlFileClassificationRuleRepository repo = new YamlFileClassificationRuleRepository(file);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, repo::loadRules);
        assertTrue(ex.getMessage().contains("priority"), "Error should identify the invalid field");
    }
}
