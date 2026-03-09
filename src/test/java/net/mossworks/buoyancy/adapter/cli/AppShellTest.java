package net.mossworks.buoyancy.adapter.cli;

import net.mossworks.buoyancy.TestCategoryRepository;
import net.mossworks.buoyancy.TestCounterpartyRepository;
import net.mossworks.buoyancy.adapter.cli.classify.ClassifyController;
import net.mossworks.buoyancy.application.CategorizationUseCase;
import net.mossworks.buoyancy.application.RuleCreationUseCase;
import org.jline.terminal.Terminal;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class AppShellTest {

    private String captureOutput(String userInput) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        Terminal terminal = new org.jline.terminal.impl.DumbTerminal(
            "test", org.jline.terminal.Terminal.TYPE_DUMB,
            new ByteArrayInputStream(userInput.getBytes()), buf,
            java.nio.charset.StandardCharsets.UTF_8);
        CategorizationUseCase categorizationUseCase = new CategorizationUseCase(
            new net.mossworks.buoyancy.application.TransactionClassifier() {
                @Override public net.mossworks.buoyancy.domain.Counterparty classify(
                    net.mossworks.buoyancy.application.dto.UnclassifiedTransaction tx) { return null; }
            });
        RuleCreationUseCase ruleCreationUseCase = new RuleCreationUseCase(
            new TestCategoryRepository(), new TestCounterpartyRepository());
        ClassifyController classifyController =
            new ClassifyController(terminal, categorizationUseCase, ruleCreationUseCase);
        MainMenuController mainMenuController =
            new MainMenuController(terminal, classifyController);
        new AppShell(terminal, mainMenuController).run();
        return buf.toString();
    }

    @Test
    void run_printsWelcomeAndGoodbye() throws IOException {
        String output = captureOutput("2\n");
        assertTrue(output.contains("Welcome to Buoyancy."));
        assertTrue(output.contains("Goodbye."));
    }
}
