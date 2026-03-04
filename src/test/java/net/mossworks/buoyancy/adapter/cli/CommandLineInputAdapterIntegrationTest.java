package net.mossworks.buoyancy.adapter.cli;

import net.mossworks.buoyancy.application.CategorizationUseCase;
import net.mossworks.buoyancy.application.RuleBasedTransactionClassifier;
import net.mossworks.buoyancy.infrastructure.persistence.YamlFileClassificationRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for the full classify flow:
 * CLI args → adapter → use case → classifier → YAML-backed repository.
 */
public class CommandLineInputAdapterIntegrationTest {

    @TempDir
    Path tempDir;

    private Path rulesFile;

    @BeforeEach
    public void setUp() throws IOException {
        rulesFile = tempDir.resolve("rules.yaml");
        Files.writeString(rulesFile, String.join("\n",
            "rules:",
            "  - id: \"550e8400-e29b-41d4-a716-446655440000\"",
            "    memoPattern: \"320*WHLFDS%\"",
            "    priority: 0",
            "    amountType: DEBIT",
            "    counterparty:",
            "      id: \"550e8400-e29b-41d4-a716-446655440001\"",
            "      name: \"Walmart\"",
            "      defaultCategory:",
            "        id: \"550e8400-e29b-41d4-a716-446655440002\"",
            "        name: \"Groceries\"",
            "        subCategory: null",
            "  - id: \"660e8400-e29b-41d4-a716-446655440000\"",
            "    memoPattern: \"PAYROLL%\"",
            "    priority: 0",
            "    amountType: CREDIT",
            "    counterparty:",
            "      id: \"660e8400-e29b-41d4-a716-446655440001\"",
            "      name: \"Employer\"",
            "      defaultCategory:",
            "        id: \"660e8400-e29b-41d4-a716-446655440002\"",
            "        name: \"Income\"",
            "        subCategory: null"
        ));
    }

    private CommandLineInputAdapter buildAdapter(String... args) {
        YamlFileClassificationRuleRepository repo =
            new YamlFileClassificationRuleRepository(rulesFile);
        RuleBasedTransactionClassifier classifier =
            new RuleBasedTransactionClassifier(repo);
        classifier.loadRules();
        CategorizationUseCase useCase = new CategorizationUseCase(classifier);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        return new CommandLineInputAdapter(useCase, args, new PrintStream(buf)) {
            // expose output for assertions via captureOutput below
        };
    }

    private String captureOutput(String... args) {
        YamlFileClassificationRuleRepository repo =
            new YamlFileClassificationRuleRepository(rulesFile);
        RuleBasedTransactionClassifier classifier =
            new RuleBasedTransactionClassifier(repo);
        classifier.loadRules();
        CategorizationUseCase useCase = new CategorizationUseCase(classifier);

        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buf);
        new CommandLineInputAdapter(useCase, args, out).run();
        return buf.toString();
    }

    @Test
    public void classify_matchesDebitRule_andPrintsCounterparty() {
        String output = captureOutput("classify", "-i", "03/12/2026 320*WHLFDS Grocery -57.32");

        assertTrue(output.contains("Walmart"), "Expected counterparty name in output");
        assertTrue(output.contains("Groceries"), "Expected category name in output");
    }

    @Test
    public void classify_matchesCreditRule_andPrintsCounterparty() {
        String output = captureOutput("classify", "-i", "03/12/2026 PAYROLL DIRECT DEPOSIT 2500.00");

        assertTrue(output.contains("Employer"), "Expected counterparty name in output");
        assertTrue(output.contains("Income"), "Expected category name in output");
    }

    @Test
    public void classify_printsNoMatch_whenNoRuleMatches() {
        String output = captureOutput("classify", "-i", "03/12/2026 UNKNOWN VENDOR XYZ 99.99");

        assertTrue(output.contains("no match"), "Expected 'no match' in output");
    }

    @Test
    public void classify_printsError_whenInputFlagMissing() {
        String output = captureOutput("classify");

        assertTrue(output.contains("Error"), "Expected error message when -i flag is missing");
    }

    @Test
    public void classify_printsUsage_whenSubcommandMissing() {
        String output = captureOutput();

        assertTrue(output.contains("Usage"), "Expected usage message when no subcommand given");
    }

    @Test
    public void classify_printsError_whenDateFormatIsInvalid() {
        String output = captureOutput("classify", "-i", "2026-03-12 GROCERY STORE 57.32");

        assertTrue(output.contains("Error"), "Expected error for invalid date format");
    }
}
