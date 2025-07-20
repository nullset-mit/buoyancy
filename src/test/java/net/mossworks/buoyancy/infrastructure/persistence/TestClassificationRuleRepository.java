package net.mossworks.buoyancy.infrastructure.persistence;

import net.mossworks.buoyancy.application.repository.ClassificationRuleRepository;
import net.mossworks.buoyancy.application.ClassificationRule;
import net.mossworks.buoyancy.domain.Category;
import net.mossworks.buoyancy.domain.Counterparty;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A test implementation of ClassificationRuleRepository that provides 
 * hard-coded rules for testing purposes.
 */
public class TestClassificationRuleRepository implements ClassificationRuleRepository {

    @Override
    public List<ClassificationRule> loadRules() {
        List<ClassificationRule> rules = new ArrayList<>();
        
        // Create some test categories
        Category groceriesCategory = new Category("Groceries");
        Category diningCategory = new Category("Dining");
        Category utilitiesCategory = new Category("Utilities");
        
        // Create some test counterparties with their default categories
        Counterparty groceryStore = new Counterparty("Grocery Store", groceriesCategory);
        Counterparty restaurant = new Counterparty("Local Restaurant", diningCategory);
        Counterparty electricCompany = new Counterparty("Electric Co.", utilitiesCategory);
        
        // Create classification rules
        rules.add(new ClassificationRule("GROCERY%", groceryStore, ClassificationRule.AmountType.DEBIT));
        rules.add(new ClassificationRule("RESTAURANT%", restaurant, ClassificationRule.AmountType.DEBIT));
        rules.add(new ClassificationRule("ELECTRIC BILL%", electricCompany, ClassificationRule.AmountType.DEBIT));
        
        return rules;
    }
}
