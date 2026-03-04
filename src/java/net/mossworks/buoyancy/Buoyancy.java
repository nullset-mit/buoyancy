package net.mossworks.buoyancy;

import net.mossworks.buoyancy.adapter.cli.CommandLineInputAdapter;
import net.mossworks.buoyancy.application.CategorizationUseCase;
import net.mossworks.buoyancy.application.RuleBasedTransactionClassifier;
import net.mossworks.buoyancy.infrastructure.persistence.YamlFileClassificationRuleRepository;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Composition root for the Buoyancy personal finance application.
 * Wires all components and delegates to the appropriate input adapter.
 */
public class Buoyancy {

    public static void main(String[] args) {
	// this is hard-coded for now, but depends on the classifier,
	// and should be configurable at runtime
        Path rulesPath = Paths.get(System.getProperty("user.home"), ".buoyancy", "rules.yaml");

        YamlFileClassificationRuleRepository repo =
            new YamlFileClassificationRuleRepository(rulesPath);

        RuleBasedTransactionClassifier classifier =
            new RuleBasedTransactionClassifier(repo);
        classifier.loadRules();

        CategorizationUseCase useCase = new CategorizationUseCase(classifier);

        CommandLineInputAdapter adapter =
            new CommandLineInputAdapter(useCase, args, System.out);
        adapter.run();
    }
}
