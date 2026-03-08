package net.mossworks.buoyancy.infrastructure.persistence;

import net.mossworks.buoyancy.application.DuplicateCategoryException;
import net.mossworks.buoyancy.application.DuplicateCounterpartyException;
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
        Counterparty counterparty = new Counterparty("Store-" + memoPattern, category);
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
        assertThrows(DuplicateCategoryException.class, () -> repo.writeCategory(category));
    }

    @Test
    void writeCategory_throwsException_onDuplicateName_caseInsensitive() {
        repo.writeCategory(new Category("Shopping"));
        assertThrows(DuplicateCategoryException.class, () -> repo.writeCategory(new Category("shopping")));
    }

    @Test
    void writeRule_reusesCategoryByName_caseInsensitive() {
        Category cat1 = new Category("Groceries");
        Counterparty cp1 = new Counterparty("Store A", cat1);
        ClassificationRule rule1 = new ClassificationRule("STOREA%", cp1, ClassificationRule.AmountType.DEBIT);
        repo.writeRule(rule1);

        Category cat2 = new Category("groceries");
        Counterparty cp2 = new Counterparty("Store B", cat2);
        ClassificationRule rule2 = new ClassificationRule("STOREB%", cp2, ClassificationRule.AmountType.DEBIT);
        repo.writeRule(rule2);

        List<ClassificationRule> loaded = repo.loadRules();
        assertEquals(2, loaded.size());
        assertEquals(loaded.get(0).getCategory().getId(), loaded.get(1).getCategory().getId());
    }

    @Test
    void listCategories_returnsEmptyList_whenNoCategoriesExist() {
        assertTrue(repo.listCategories().isEmpty());
    }

    @Test
    void listCategories_returnsAllCategories_orderedByName() {
        repo.writeCategory(new Category("Groceries"));
        repo.writeCategory(new Category("Entertainment"));
        repo.writeCategory(new Category("Shopping"));

        List<Category> result = repo.listCategories();
        assertEquals(3, result.size());
        assertEquals("Entertainment", result.get(0).getName());
        assertEquals("Groceries", result.get(1).getName());
        assertEquals("Shopping", result.get(2).getName());
    }

    @Test
    void writeCounterparty_insertsCounterparty() {
        Category category = new Category("Groceries");
        repo.writeCategory(category);
        Counterparty cp = new Counterparty("Walmart", category);
        assertDoesNotThrow(() -> repo.writeCounterparty(cp));
    }

    @Test
    void writeCounterparty_throwsOnDuplicateName() {
        Category category = new Category("Groceries");
        repo.writeCategory(category);
        repo.writeCounterparty(new Counterparty("Walmart", category));
        assertThrows(DuplicateCounterpartyException.class,
            () -> repo.writeCounterparty(new Counterparty("walmart", category)));
    }

    @Test
    void listCounterpartiesByCategory_returnsMatchingCounterparties() {
        Category groceries = new Category("Groceries");
        Category shopping  = new Category("Shopping");
        repo.writeCategory(groceries);
        repo.writeCategory(shopping);
        Counterparty walmart  = new Counterparty("Walmart", groceries);
        Counterparty target   = new Counterparty("Target", shopping);
        Counterparty kroger   = new Counterparty("Kroger", groceries);
        repo.writeCounterparty(walmart);
        repo.writeCounterparty(target);
        repo.writeCounterparty(kroger);

        List<Counterparty> result = repo.listCounterpartiesByCategory(groceries);
        assertEquals(2, result.size());
        List<String> names = result.stream().map(Counterparty::getName).toList();
        assertTrue(names.contains("Walmart"));
        assertTrue(names.contains("Kroger"));
    }

    @Test
    void listCounterpartiesByCategory_returnsEmptyWhenNoneMatch() {
        Category groceries = new Category("Groceries");
        Category shopping  = new Category("Shopping");
        repo.writeCategory(groceries);
        repo.writeCategory(shopping);
        repo.writeCounterparty(new Counterparty("Target", shopping));

        List<Counterparty> result = repo.listCounterpartiesByCategory(groceries);
        assertTrue(result.isEmpty());
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
