package net.mossworks.buoyancy.adapter.cli.classify;

import net.mossworks.buoyancy.application.CategorizationUseCase;
import net.mossworks.buoyancy.application.RuleCreationUseCase;
import net.mossworks.buoyancy.application.TransactionClassifier;
import net.mossworks.buoyancy.application.dto.UnclassifiedTransaction;
import net.mossworks.buoyancy.domain.Category;
import net.mossworks.buoyancy.domain.Counterparty;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

public class ClassifyControllerTest {

    private String captureOutput(String userInput, Counterparty matchResult) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buf);
        Scanner in = new Scanner(new ByteArrayInputStream(userInput.getBytes()));
        CategorizationUseCase categorizationUseCase = new CategorizationUseCase(
            new TransactionClassifier() {
                @Override public Counterparty classify(UnclassifiedTransaction tx) { return matchResult; }
            });
        RuleCreationUseCase ruleCreationUseCase = new RuleCreationUseCase(cat -> {});
        new ClassifyController(out, in, categorizationUseCase, ruleCreationUseCase).run();
        return buf.toString();
    }

    @Test
    void run_printsCounterpartyAndCategory_whenRuleMatches() {
        Category category = new Category("Groceries");
        Counterparty counterparty = new Counterparty("Walmart", category);

        String output = captureOutput("03/12/2026 WALMART 57.32\n", counterparty);

        assertTrue(output.contains("Walmart"));
        assertTrue(output.contains("Groceries"));
    }

    @Test
    void run_startsRuleCreationWorkflow_whenNoMatch() {
        String output = captureOutput("03/12/2026 UNKNOWN 99.99\nShopping\n", null);

        assertTrue(output.contains("No matching rule found"));
        assertTrue(output.contains("Enter category name:"));
        assertTrue(output.contains("created."));
    }

    @Test
    void run_printsError_whenTransactionFormatInvalid() {
        String output = captureOutput("not a valid transaction\n", null);

        assertTrue(output.contains("Error:"));
    }
}
