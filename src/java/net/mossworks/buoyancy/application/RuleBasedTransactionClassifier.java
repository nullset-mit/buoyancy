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
     * Rules are evaluated in descending priority order. The first match wins.
     * A rule matches when its memo pattern (using % as a wildcard) matches the
     * transaction memo (case-insensitive) and its AmountType is compatible with
     * whether the transaction is a credit or debit.
     *
     * @param transaction The unclassified transaction to classify
     * @return The counterparty determined for this transaction, or null if no matching rule found
     */
    @Override
    public Counterparty classify(UnclassifiedTransaction transaction) {
        if (rules == null || rules.isEmpty()) {
            return null;
        }
        return rules.stream()
            .filter(rule -> amountTypeMatches(rule, transaction))
            .filter(rule -> memoMatches(rule.getMemoPattern(), transaction.getMemo()))
            .max(java.util.Comparator.comparingInt(ClassificationRule::getPriority))
            .map(ClassificationRule::getCounterparty)
            .orElse(null);
    }

    private boolean amountTypeMatches(ClassificationRule rule, UnclassifiedTransaction transaction) {
        switch (rule.getAmountType()) {
            case CREDIT: return transaction.isCredit();
            case DEBIT:  return transaction.isDebit();
            default:     return true;
        }
    }

    /**
     * Converts a SQL-style % wildcard pattern to a regex and tests it against the memo.
     * Matching is case-insensitive.
     */
    private boolean memoMatches(String pattern, String memo) {
        String regex = "(?i)" + java.util.regex.Pattern.quote(pattern)
            .replace("%", "\\E.*\\Q");
        return memo.matches(regex);
    }
}
