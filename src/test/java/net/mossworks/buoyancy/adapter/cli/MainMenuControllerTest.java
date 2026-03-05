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

public class MainMenuControllerTest {

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
        new MainMenuController(out, in, classifyController).run();
        return buf.toString();
    }

    @Test
    void run_exitsOnExitChoice() {
        String output = captureOutput("2\n");
        assertTrue(output.contains("What would you like to do?"));
    }

    @Test
    void run_dispatchesToClassify_onClassifyChoice() {
        // "1" → classify, then provide a transaction, then "2" → exit
        String output = captureOutput("1\n03/12/2026 UNKNOWN 99.99\nShopping\n2\n");
        assertTrue(output.contains("Enter transaction"));
    }

    @Test
    void run_printsUnknownOption_onInvalidChoice() {
        String output = captureOutput("9\n2\n");
        assertTrue(output.contains("Unknown option"));
    }

    @Test
    void run_loopsBackToMenu_afterControllerReturns() {
        // classify once, then exit — menu should appear twice
        String output = captureOutput("1\n03/12/2026 UNKNOWN 99.99\nShopping\n2\n");
        assertEquals(2, countOccurrences(output, "What would you like to do?"));
    }

    private int countOccurrences(String text, String pattern) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(pattern, index)) != -1) {
            count++;
            index += pattern.length();
        }
        return count;
    }
}
