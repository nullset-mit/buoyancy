package net.mossworks.buoyancy.application;

import net.mossworks.buoyancy.application.dto.UnclassifiedTransaction;
import net.mossworks.buoyancy.domain.Counterparty;

/**
 * Orchestrates the expense categorization business logic.
 * Delegates classification to the injected TransactionClassifier.
 */
public class CategorizationUseCase {

    private final TransactionClassifier classifier;

    public CategorizationUseCase(TransactionClassifier classifier) {
        if (classifier == null) {
            throw new IllegalArgumentException("Classifier cannot be null");
        }
        this.classifier = classifier;
    }

    /**
     * Classifies a transaction by matching it against known rules.
     *
     * @param transaction the unclassified transaction
     * @return the matched Counterparty, or null if no rule matched
     */
    public Counterparty classify(UnclassifiedTransaction transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        return classifier.classify(transaction);
    }
}
