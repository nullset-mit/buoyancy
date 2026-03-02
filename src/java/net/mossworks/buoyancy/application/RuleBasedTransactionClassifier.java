package net.mossworks.buoyancy.application;

import lombok.Getter;
import lombok.Setter;
import net.mossworks.buoyancy.application.dto.UnclassifiedTransaction;
import net.mossworks.buoyancy.application.repository.ClassificationRuleRepository;
import net.mossworks.buoyancy.domain.ClassificationRule;
import net.mossworks.buoyancy.domain.Counterparty;

import java.util.List;

/**
 * A transaction classifier that uses a list of rules to determine the appropriate
 * counterparty for an unclassified transaction.
 */
public class RuleBasedTransactionClassifier extends TransactionClassifier {

    private final ClassificationRuleRepository ruleRepository;

    /**
     * The list of classification rules used to classify transactions.
     * Can be updated after construction.
     */
    @Getter
    @Setter
    private List<ClassificationRule> rules;

    /**
     * Constructs a new RuleBasedTransactionClassifier with the specified repository.
     * 
     * @param ruleRepository The repository to load classification rules from
     */
    public RuleBasedTransactionClassifier(ClassificationRuleRepository ruleRepository) {
        if (ruleRepository == null) {
            throw new IllegalArgumentException("Rule repository cannot be null");
        }
        this.ruleRepository = ruleRepository;
    }    

    /**
     * Loads the rules from the repository and updates the classifier's rule set.
     */
    public void loadRules() {
        this.rules = ruleRepository.loadRules();
    }

    /**
     * Classifies an unclassified transaction by finding a matching rule
     * and returning its associated counterparty.
     *
     * @param transaction The unclassified transaction to classify
     * @return The counterparty determined for this transaction, or null if no matching rule found
     */
    @Override
    public Counterparty classify(UnclassifiedTransaction transaction) {
        // Initially blank as per requirements
        return null;
    }
}
