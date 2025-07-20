package net.mossworks.buoyancy.application;

import net.mossworks.buoyancy.application.dto.UnclassifiedTransaction;
import net.mossworks.buoyancy.infrastructure.persistence.TestClassificationRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RuleBasedTransactionClassifierIntegrationTest {

    private RuleBasedTransactionClassifier classifier;
    
    @BeforeEach
    public void setUp() {
        // Create classifier with the test repository and load rules
        classifier = new RuleBasedTransactionClassifier(new TestClassificationRuleRepository());
        classifier.loadRules();
    }

    // TODO - getRules doesn't exist
    @Test
    public void testClassifierHasRules() {
        // Simple test to verify rules were loaded
        assertNotNull(classifier.getRules());
        assertFalse(classifier.getRules().isEmpty());
    }
    
    // Additional tests will be added once classify() is implemented
}
