package net.mossworks.buoyancy;

import net.mossworks.buoyancy.adapter.cli.AppShell;
import net.mossworks.buoyancy.adapter.cli.MainMenuController;
import net.mossworks.buoyancy.adapter.cli.classify.ClassifyController;
import net.mossworks.buoyancy.application.CategorizationUseCase;
import net.mossworks.buoyancy.application.RuleBasedTransactionClassifier;
import net.mossworks.buoyancy.application.RuleCreationUseCase;
import net.mossworks.buoyancy.infrastructure.persistence.SQLiteClassificationRuleRepository;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Buoyancy {

    public static void main(String[] args) throws IOException {
        Path dbPath = Paths.get(System.getProperty("user.home"), ".buoyancy", "buoyancy.db");

        SQLiteClassificationRuleRepository repo = new SQLiteClassificationRuleRepository(dbPath);

        RuleBasedTransactionClassifier classifier = new RuleBasedTransactionClassifier(repo);
        classifier.loadRules();

        CategorizationUseCase categorizationUseCase = new CategorizationUseCase(classifier);
        RuleCreationUseCase ruleCreationUseCase = new RuleCreationUseCase(repo, repo);

        Terminal terminal = TerminalBuilder.builder().system(true).build();

        ClassifyController classifyController =
            new ClassifyController(terminal, categorizationUseCase, ruleCreationUseCase);
        MainMenuController mainMenuController =
            new MainMenuController(terminal, classifyController);
        AppShell shell = new AppShell(terminal, mainMenuController);

        shell.run();
    }
}
