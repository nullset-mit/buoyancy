package net.mossworks.buoyancy.adapter.cli.classify;

import net.mossworks.buoyancy.TestCategoryRepository;
import net.mossworks.buoyancy.TestCounterpartyRepository;
import net.mossworks.buoyancy.application.CategorizationUseCase;
import net.mossworks.buoyancy.application.RuleCreationUseCase;
import net.mossworks.buoyancy.application.TransactionClassifier;
import net.mossworks.buoyancy.application.dto.UnclassifiedTransaction;
import net.mossworks.buoyancy.domain.Category;
import net.mossworks.buoyancy.domain.Counterparty;
import org.jline.terminal.Terminal;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class ClassifyControllerTest {

    private static final byte[] SHIFT_RIGHT = {27, '[', '1', ';', '2', 'C'};
    private static final byte   ENTER       = 13;

    private static byte[] concat(byte[]... parts) {
        int len = 0;
        for (byte[] p : parts) len += p.length;
        byte[] result = new byte[len];
        int pos = 0;
        for (byte[] p : parts) { System.arraycopy(p, 0, result, pos, p.length); pos += p.length; }
        return result;
    }

    private String captureOutput(byte[] rawInput, Counterparty matchResult) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        Terminal terminal = new org.jline.terminal.impl.DumbTerminal(
            "test", org.jline.terminal.Terminal.TYPE_DUMB,
            new ByteArrayInputStream(rawInput), buf,
            java.nio.charset.StandardCharsets.UTF_8);
        CategorizationUseCase categorizationUseCase = new CategorizationUseCase(
            new TransactionClassifier() {
                @Override public Counterparty classify(UnclassifiedTransaction tx) { return matchResult; }
            });
        RuleCreationUseCase ruleCreationUseCase = new RuleCreationUseCase(
            new TestCategoryRepository(), new TestCounterpartyRepository());
        new ClassifyController(terminal, categorizationUseCase, ruleCreationUseCase).run();
        return buf.toString();
    }

    @Test
    void run_printsCounterpartyAndCategory_whenRuleMatches() throws IOException {
        Category category = new Category("Groceries");
        Counterparty counterparty = new Counterparty("Walmart", category);
        byte[] input = "03/12/2026 WALMART 57.32\n".getBytes();
        String output = captureOutput(input, counterparty);
        assertTrue(output.contains("Walmart"));
        assertTrue(output.contains("Groceries"));
    }

    @Test
    void run_showsCounterpartyWidget_afterCategorySelected() throws IOException {
        // No-match path: category widget appears (empty list → create mode on Enter),
        // type "Shopping", Enter saves category; counterparty widget appears,
        // type "Target", Enter saves counterparty → confirmation printed.
        byte[] txLine = "03/12/2026 UNKNOWN 99.99\n".getBytes();
        byte[] catName = {'S', 'h', 'o', 'p', 'p', 'i', 'n', 'g'};
        byte[] cpName  = {'T', 'a', 'r', 'g', 'e', 't'};

        // Category widget: Enter → create mode; type name; Enter → save
        // Counterparty widget: Enter → create mode; type name; Enter → save
        byte[] input = concat(txLine,
            new byte[]{ENTER}, catName, new byte[]{ENTER},
            new byte[]{ENTER}, cpName,  new byte[]{ENTER});
        String output = captureOutput(input, null);
        assertTrue(output.contains("No matching rule found"));
        assertTrue(output.contains("Counterparty: Target"));
        assertTrue(output.contains("Category: Shopping"));
    }

    @Test
    void run_printsError_whenTransactionFormatInvalid() throws IOException {
        byte[] input = "not a valid transaction\n".getBytes();
        String output = captureOutput(input, null);
        assertTrue(output.contains("Error:"));
    }
}
