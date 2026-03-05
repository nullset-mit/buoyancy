package net.mossworks.buoyancy.adapter.cli.classify;

import net.mossworks.buoyancy.application.dto.UnclassifiedTransaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class TransactionParser {

    private static final DateTimeFormatter DATE_FORMAT =
        DateTimeFormatter.ofPattern("MM/dd/yyyy");

    private TransactionParser() {}

    public static UnclassifiedTransaction parse(String input) {
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
