package net.mossworks.buoyancy.application.repository;

import net.mossworks.buoyancy.application.ClassificationRule;

import java.util.List;

/**
 * Repository interface for accessing classification rules.
 * This interface defines the contract for retrieving classification rules
 * from a persistence store.
 */
public interface ClassificationRuleRepository {
    
    /**
     * Loads all classification rules from the repository.
     *
     * @return A list of classification rules
     */
    List<ClassificationRule> loadRules();
}
