package net.mossworks.buoyancy.adapter.cli;

import net.mossworks.buoyancy.adapter.cli.classify.ClassifyController;
import net.mossworks.buoyancy.application.CategorizationUseCase;
import net.mossworks.buoyancy.application.RuleCreationUseCase;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

public class AppShellTest {

    private String captureOutput(String userInput) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buf);
        Scanner in = new Scanner(new ByteArrayInputStream(userInput.getBytes()));
        CategorizationUseCase categorizationUseCase = new CategorizationUseCase(
            new net.mossworks.buoyancy.application.TransactionClassifier() {
                @Override public net.mossworks.buoyancy.domain.Counterparty classify(
                    net.mossworks.buoyancy.application.dto.UnclassifiedTransaction tx) { return null; }
            });
        RuleCreationUseCase ruleCreationUseCase = new RuleCreationUseCase(cat -> {});
        ClassifyController classifyController =
            new ClassifyController(out, in, categorizationUseCase, ruleCreationUseCase);
        MainMenuController mainMenuController =
            new MainMenuController(out, in, classifyController);
        new AppShell(out, mainMenuController).run();
        return buf.toString();
    }

    @Test
    void run_printsWelcomeAndGoodbye() {
        String output = captureOutput("2\n");
        assertTrue(output.contains("Welcome to Buoyancy."));
        assertTrue(output.contains("Goodbye."));
    }
}
