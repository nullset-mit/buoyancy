package net.mossworks.buoyancy.application.dto;

import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Data Transfer Object representing an unclassified transaction from an external source.
 * This class holds transaction data before it is categorized and converted to a domain entity.
 */
@Getter
public class UnclassifiedTransaction {
    private final String memo;
    private final BigDecimal amount;
    private final LocalDate date;

    /**
     * Constructs a new UnclassifiedTransaction with the specified details.
     *
     * @param memo The transaction description or memo
     * @param amount The transaction amount
     * @param date The transaction date
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public UnclassifiedTransaction(String memo, BigDecimal amount, LocalDate date) {
        if (memo == null) {
            throw new IllegalArgumentException("Transaction memo cannot be null");
        }
        if (amount == null) {
            throw new IllegalArgumentException("Transaction amount cannot be null");
        }
        if (date == null) {
            throw new IllegalArgumentException("Transaction date cannot be null");
        }
        
        // Validate amount precision (not more than 2 decimal places)
        if (amount.scale() > 2) {
            throw new IllegalArgumentException("Transaction amount cannot have more than 2 decimal places");
        }
        
        this.memo = memo;
        // Ensure amount is stored with exactly 2 decimal places
        this.amount = amount.setScale(2, RoundingMode.HALF_EVEN);
        this.date = date;
    }

    /**
     * @return true if this transaction has a positive amount (credit), false otherwise
     */
    public boolean isCredit() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * @return true if this transaction has a negative amount (debit), false otherwise
     */
    public boolean isDebit() {
        return amount.compareTo(BigDecimal.ZERO) < 0;
    }

    @Override
    public String toString() {
        return String.format("UnclassifiedTransaction{date=%s, memo='%s', amount=%s}", 
                date, memo, amount);
    }
}
