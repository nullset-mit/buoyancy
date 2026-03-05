package net.mossworks.buoyancy.infrastructure.persistence;

import net.mossworks.buoyancy.domain.Category;
import net.mossworks.buoyancy.domain.ClassificationRule;
import net.mossworks.buoyancy.domain.Counterparty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class SQLiteClassificationRuleRepositoryTest {

    private SQLiteClassificationRuleRepository repo;

    @BeforeEach
    void setUp() throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        repo = new SQLiteClassificationRuleRepository(connection);
    }

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
    void loadRules_returnsEmptyList_whenNoRulesExist() {
        List<ClassificationRule> rules = repo.loadRules();

        assertNotNull(rules);
        assertTrue(rules.isEmpty());
    }

    @Test
    void writeRule_persistsRule_andLoadReturnsIt() {
        ClassificationRule rule = sampleRule("GROCERY%");

        repo.writeRule(rule);

        List<ClassificationRule> loaded = repo.loadRules();
        assertEquals(1, loaded.size());
        ClassificationRule result = loaded.get(0);
        assertEquals(rule.getId(), result.getId());
        assertEquals(rule.getMemoPattern(), result.getMemoPattern());
        assertEquals(rule.getPriority(), result.getPriority());
        assertEquals(rule.getAmountType(), result.getAmountType());
        assertEquals(rule.getCounterparty().getName(), result.getCounterparty().getName());
        assertEquals(rule.getCounterparty().getId(), result.getCounterparty().getId());
        assertEquals(rule.getCategory().getName(), result.getCategory().getName());
        assertEquals(rule.getCategory().getId(), result.getCategory().getId());
    }

    @Test
    void writeRule_appendsRule_toExistingRules() {
        ClassificationRule rule1 = sampleRule("GROCERY%");
        ClassificationRule rule2 = sampleRule("RESTAURANT%");

        repo.writeRule(rule1);
        repo.writeRule(rule2);

        List<ClassificationRule> loaded = repo.loadRules();
        assertEquals(2, loaded.size());
        assertTrue(loaded.stream().anyMatch(r -> r.getId().equals(rule1.getId())));
        assertTrue(loaded.stream().anyMatch(r -> r.getId().equals(rule2.getId())));
    }

    @Test
    void writeRule_throwsException_whenDuplicateRuleId() {
        UUID sharedId = UUID.randomUUID();
        ClassificationRule rule1 = sampleRuleWithId(sharedId, "GROCERY%");
        ClassificationRule rule2 = sampleRuleWithId(sharedId, "RESTAURANT%");

        repo.writeRule(rule1);
        assertThrows(IllegalArgumentException.class, () -> repo.writeRule(rule2));

        assertEquals(1, repo.loadRules().size());
    }

    @Test
    void writeRule_reusesCategoryAndCounterparty_whenAlreadyPersisted() {
        Category sharedCategory = new Category("Groceries");
        Counterparty sharedCounterparty = new Counterparty("Grocery Store", sharedCategory);
        ClassificationRule rule1 = new ClassificationRule("GROCERY%", sharedCounterparty, ClassificationRule.AmountType.DEBIT);
        ClassificationRule rule2 = new ClassificationRule("SUPERMARKET%", sharedCounterparty, ClassificationRule.AmountType.DEBIT);

        repo.writeRule(rule1);
        repo.writeRule(rule2);

        List<ClassificationRule> loaded = repo.loadRules();
        assertEquals(2, loaded.size());
        assertEquals(sharedCounterparty.getId(), loaded.get(0).getCounterparty().getId());
        assertEquals(sharedCounterparty.getId(), loaded.get(1).getCounterparty().getId());
    }

    @Test
    void writeRule_handlesNullSubCategory() {
        Category category = new Category(UUID.randomUUID(), "Groceries", null);
        Counterparty counterparty = new Counterparty("Grocery Store", category);
        ClassificationRule rule = new ClassificationRule("GROCERY%", counterparty, ClassificationRule.AmountType.DEBIT);

        repo.writeRule(rule);

        ClassificationRule loaded = repo.loadRules().get(0);
        assertNull(loaded.getCategory().getSubCategory());
    }

    @Test
    void writeRule_handlesNonNullSubCategory() {
        Category category = new Category(UUID.randomUUID(), "Food", "Groceries");
        Counterparty counterparty = new Counterparty("Grocery Store", category);
        ClassificationRule rule = new ClassificationRule("GROCERY%", counterparty, ClassificationRule.AmountType.DEBIT);

        repo.writeRule(rule);

        ClassificationRule loaded = repo.loadRules().get(0);
        assertEquals("Groceries", loaded.getCategory().getSubCategory());
    }

    @Test
    void writeCategory_succeeds_withoutException() {
        Category category = new Category("Shopping");
        assertDoesNotThrow(() -> repo.writeCategory(category));
    }

    @Test
    void writeCategory_throwsException_onDuplicateId() {
        Category category = new Category("Shopping");
        repo.writeCategory(category);
        assertThrows(IllegalArgumentException.class, () -> repo.writeCategory(category));
    }

    @Test
    void loadRules_roundTrips_allAmountTypes() {
        Category cat = new Category("General");
        ClassificationRule creditRule = new ClassificationRule(
            "PAYROLL%", new Counterparty("Employer", cat), ClassificationRule.AmountType.CREDIT);
        ClassificationRule debitRule = new ClassificationRule(
            "GROCERY%", new Counterparty("Store", new Category("Food")), ClassificationRule.AmountType.DEBIT);
        ClassificationRule bothRule = new ClassificationRule(
            "TRANSFER%", new Counterparty("Bank", new Category("Transfers")), ClassificationRule.AmountType.BOTH);

        repo.writeRule(creditRule);
        repo.writeRule(debitRule);
        repo.writeRule(bothRule);

        List<ClassificationRule> loaded = repo.loadRules();
        assertEquals(3, loaded.size());
        assertTrue(loaded.stream().anyMatch(r -> r.getAmountType() == ClassificationRule.AmountType.CREDIT));
        assertTrue(loaded.stream().anyMatch(r -> r.getAmountType() == ClassificationRule.AmountType.DEBIT));
        assertTrue(loaded.stream().anyMatch(r -> r.getAmountType() == ClassificationRule.AmountType.BOTH));
    }
}
