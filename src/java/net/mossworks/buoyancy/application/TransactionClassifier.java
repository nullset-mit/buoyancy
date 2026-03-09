package net.mossworks.buoyancy.application;

import net.mossworks.buoyancy.application.dto.UnclassifiedTransaction;
import net.mossworks.buoyancy.domain.Counterparty;

/**
 * Abstract base class for transaction classifiers.
 * Provides a common interface for different classification strategies.
 */
public abstract class TransactionClassifier {
    
    /**
     * Classifies an unclassified transaction by determining the appropriate counterparty.
     * The counterparty contains the default category for the transaction.
     *
     * @param transaction The unclassified transaction to classify
     * @return The counterparty determined for this transaction, or null if no match found
     */
    public abstract Counterparty classify(UnclassifiedTransaction transaction);
}
