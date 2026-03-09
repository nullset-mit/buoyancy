package net.mossworks.buoyancy.application.repository;

import net.mossworks.buoyancy.domain.ClassificationRule;

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

    /**
     * Persists a single classification rule to the repository.
     * Implementations must reject rules whose ID already exists in the repository.
     *
     * @param rule The rule to write
     * @throws IllegalArgumentException if a rule with the same ID already exists
     */
    void writeRule(ClassificationRule rule);
}
