package net.mossworks.buoyancy;

import net.mossworks.buoyancy.adapter.cli.AppShell;
import net.mossworks.buoyancy.adapter.cli.MainMenuController;
import net.mossworks.buoyancy.adapter.cli.classify.ClassifyController;
import net.mossworks.buoyancy.application.CategorizationUseCase;
import net.mossworks.buoyancy.application.RuleBasedTransactionClassifier;
import net.mossworks.buoyancy.application.RuleCreationUseCase;
import net.mossworks.buoyancy.infrastructure.persistence.SQLiteClassificationRuleRepository;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class Buoyancy {

    public static void main(String[] args) {
        Path dbPath = Paths.get(System.getProperty("user.home"), ".buoyancy", "buoyancy.db");

        SQLiteClassificationRuleRepository repo = new SQLiteClassificationRuleRepository(dbPath);

        RuleBasedTransactionClassifier classifier = new RuleBasedTransactionClassifier(repo);
        classifier.loadRules();

        CategorizationUseCase categorizationUseCase = new CategorizationUseCase(classifier);
        RuleCreationUseCase ruleCreationUseCase = new RuleCreationUseCase(repo);

        Scanner scanner = new Scanner(System.in);

        ClassifyController classifyController =
            new ClassifyController(System.out, scanner, categorizationUseCase, ruleCreationUseCase);
        MainMenuController mainMenuController =
            new MainMenuController(System.out, scanner, classifyController);
        AppShell shell = new AppShell(System.out, mainMenuController);

        shell.run();
    }
}
