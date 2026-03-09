package net.mossworks.buoyancy.application;

import net.mossworks.buoyancy.application.dto.UnclassifiedTransaction;
import net.mossworks.buoyancy.domain.Category;
import net.mossworks.buoyancy.domain.ClassificationRule;
import net.mossworks.buoyancy.domain.Counterparty;
import net.mossworks.buoyancy.infrastructure.persistence.TestClassificationRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RuleBasedTransactionClassifierIntegrationTest {

    private RuleBasedTransactionClassifier classifier;
    private static final LocalDate TODAY = LocalDate.of(2026, 3, 2);

    @BeforeEach
    public void setUp() {
        classifier = new RuleBasedTransactionClassifier(new TestClassificationRuleRepository());
        classifier.loadRules();
    }

    private UnclassifiedTransaction debit(String memo) {
        return new UnclassifiedTransaction(memo, new BigDecimal("-57.32"), TODAY);
    }

    private UnclassifiedTransaction credit(String memo) {
        return new UnclassifiedTransaction(memo, new BigDecimal("100.00"), TODAY);
    }

    @Test
    public void testClassifierHasRules() {
        assertNotNull(classifier.getRules());
        assertFalse(classifier.getRules().isEmpty());
    }

    @Test
    public void classify_returnsCounterparty_whenMemoMatchesPrefix() {
        Counterparty result = classifier.classify(debit("GROCERY MART 12345"));

        assertNotNull(result);
        assertEquals("Grocery Store", result.getName());
        assertEquals("Groceries", result.getDefaultCategory().getName());
    }

    @Test
    public void classify_returnsCounterparty_caseInsensitive() {
        Counterparty result = classifier.classify(debit("grocery daily shop"));

        assertNotNull(result);
        assertEquals("Grocery Store", result.getName());
    }

    @Test
    public void classify_returnsNull_whenNoRuleMatches() {
        Counterparty result = classifier.classify(debit("UNKNOWN VENDOR XYZ"));

        assertNull(result);
    }

    @Test
    public void classify_returnsNull_whenAmountTypeDoesNotMatch() {
        // All test rules are DEBIT; a credit should not match
        Counterparty result = classifier.classify(credit("GROCERY STORE REFUND"));

        assertNull(result);
    }

    @Test
    public void classify_returnsNull_whenRulesNotLoaded() {
        RuleBasedTransactionClassifier fresh =
            new RuleBasedTransactionClassifier(new TestClassificationRuleRepository());
        // loadRules() intentionally not called

        Counterparty result = fresh.classify(debit("GROCERY MART"));

        assertNull(result);
    }

    @Test
    public void classify_respectsPriority_returnsHighestPriorityMatch() {
        Category cat = new Category("Supermarket");
        Counterparty highPriority = new Counterparty("Premium Grocer", cat);
        ClassificationRule lowRule = new ClassificationRule("GROCERY%", 0,
            new Counterparty("Generic Grocer", new Category("Groceries")),
            ClassificationRule.AmountType.DEBIT);
        ClassificationRule highRule = new ClassificationRule("GROCERY%", 10,
            highPriority, ClassificationRule.AmountType.DEBIT);

        classifier.setRules(List.of(lowRule, highRule));

        Counterparty result = classifier.classify(debit("GROCERY MART"));

        assertNotNull(result);
        assertEquals("Premium Grocer", result.getName());
    }
}
