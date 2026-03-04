package net.mossworks.buoyancy.adapter.cli;

import net.mossworks.buoyancy.application.CategorizationUseCase;
import net.mossworks.buoyancy.application.dto.UnclassifiedTransaction;
import net.mossworks.buoyancy.application.port.InputAdapter;
import net.mossworks.buoyancy.domain.Counterparty;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * CLI input adapter for the categorization workflow.
 *
 * Usage: buoyancy classify -i "mm/dd/yyyy memo amount"
 *
 * Parses the -i argument into an UnclassifiedTransaction, calls the use case,
 * and prints the result (or "no match") to the output stream.
 */
public class CommandLineInputAdapter implements InputAdapter {

    private static final DateTimeFormatter DATE_FORMAT =
        DateTimeFormatter.ofPattern("MM/dd/yyyy");

    private final CategorizationUseCase useCase;
    private final String[] args;
    private final PrintStream out;

    public CommandLineInputAdapter(CategorizationUseCase useCase, String[] args, PrintStream out) {
        if (useCase == null) throw new IllegalArgumentException("Use case cannot be null");
        if (args == null)    throw new IllegalArgumentException("Args cannot be null");
        if (out == null)     throw new IllegalArgumentException("Output stream cannot be null");
        this.useCase = useCase;
        this.args = args;
        this.out = out;
    }

    @Override
    public void run() {
        String subcommand = args.length > 0 ? args[0] : "";
        if (!"classify".equals(subcommand)) {
            out.println("Usage: buoyancy classify -i \"mm/dd/yyyy memo amount\"");
            return;
        }

        String input = extractInputFlag(args);
        if (input == null) {
            out.println("Error: -i flag is required. Usage: buoyancy classify -i \"mm/dd/yyyy memo amount\"");
            return;
        }

        UnclassifiedTransaction transaction;
        try {
            transaction = parse(input);
        } catch (IllegalArgumentException e) {
            out.println("Error: " + e.getMessage());
            return;
        }

        Counterparty counterparty = useCase.classify(transaction);

        if (counterparty != null) {
            String subCategory = counterparty.getDefaultCategory().hasSubCategory()
                ? " / " + counterparty.getDefaultCategory().getSubCategory()
                : "";
            out.printf("Counterparty : %s%n", counterparty.getName());
            out.printf("Category     : %s%s%n",
                counterparty.getDefaultCategory().getName(), subCategory);
        } else {
            out.println("no match");
        }
    }

    /**
     * Extracts the value following the -i flag from the argument array.
     */
    private String extractInputFlag(String[] args) {
        for (int i = 0; i < args.length - 1; i++) {
            if ("-i".equals(args[i])) {
                return args[i + 1];
            }
        }
        return null;
    }

    /**
     * Parses a transaction string of the form "mm/dd/yyyy memo amount".
     * The date is the first token, the amount is the last token, and everything
     * in between is the memo.
     */
    UnclassifiedTransaction parse(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Transaction input cannot be blank");
        }
        String[] tokens = input.trim().split("\\s+");
        if (tokens.length < 3) {
            throw new IllegalArgumentException(
                "Transaction must have the format: mm/dd/yyyy memo amount");
        }

        LocalDate date;
        try {
            date = LocalDate.parse(tokens[0], DATE_FORMAT);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                "Invalid date '" + tokens[0] + "': expected mm/dd/yyyy");
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(tokens[tokens.length - 1]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                "Invalid amount '" + tokens[tokens.length - 1] + "': must be a number");
        }

        String[] memoParts = new String[tokens.length - 2];
        System.arraycopy(tokens, 1, memoParts, 0, memoParts.length);
        String memo = String.join(" ", memoParts);

        return new UnclassifiedTransaction(memo, amount, date);
    }
}
