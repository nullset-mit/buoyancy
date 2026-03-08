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

public class MainMenuControllerTest {

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
        new MainMenuController(terminal, classifyController).run();
        return buf.toString();
    }

    @Test
    void run_exitsOnExitChoice() throws IOException {
        String output = captureOutput("2\n");
        assertTrue(output.contains("What would you like to do?"));
    }

    @Test
    void run_dispatchesToClassify_onClassifyChoice() throws IOException {
        // '\n' before category/counterparty names enters create mode in the widget
        String output = captureOutput("1\n03/12/2026 UNKNOWN 99.99\n\nShopping\n\nTarget\n2\n");
        assertTrue(output.contains("Enter transaction"));
    }

    @Test
    void run_printsUnknownOption_onInvalidChoice() throws IOException {
        String output = captureOutput("9\n2\n");
        assertTrue(output.contains("Unknown option"));
    }

    @Test
    void run_loopsBackToMenu_afterControllerReturns() throws IOException {
        // Full no-match flow: transaction, then category widget (Enter→create mode, name, Enter),
        // then counterparty widget (Enter→create mode, name, Enter), then exit.
        String output = captureOutput("1\n03/12/2026 UNKNOWN 99.99\n\nShopping\n\nTarget\n2\n");
        assertEquals(2, countOccurrences(output, "What would you like to do?"));
    }

    private int countOccurrences(String text, String pattern) {
        int count = 0, index = 0;
        while ((index = text.indexOf(pattern, index)) != -1) { count++; index += pattern.length(); }
        return count;
    }
}
