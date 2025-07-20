package net.mossworks.buoyancy.domain;

import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a financial transaction.
 * Transactions are the core domain objects that record financial activities.
 */
@Getter
public class Transaction {
    private final UUID id;
    private final LocalDate date;
    private final String memo;
    private final Counterparty counterparty;
    private final BigDecimal amount;
    private final Category category;

    /**
     * Constructs a new Transaction with a generated UUID.
     *
     * @param date The date of the transaction
     * @param memo The transaction description or memo
     * @param counterparty The counterparty involved in the transaction
     * @param amount The transaction amount
     * @param category The transaction category
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public Transaction(LocalDate date, String memo, Counterparty counterparty, 
                       BigDecimal amount, Category category) {
        this(UUID.randomUUID(), date, memo, counterparty, amount, category);
    }

    /**
     * Constructs a new Transaction with the specified ID.
     *
     * @param id The UUID of the transaction
     * @param date The date of the transaction
     * @param memo The transaction description or memo
     * @param counterparty The counterparty involved in the transaction
     * @param amount The transaction amount
     * @param category The transaction category
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public Transaction(UUID id, LocalDate date, String memo, Counterparty counterparty, 
                       BigDecimal amount, Category category) {
        if (id == null) {
            throw new IllegalArgumentException("Transaction ID cannot be null");
        }
        if (date == null) {
            throw new IllegalArgumentException("Transaction date cannot be null");
        }
        if (memo == null) {
            throw new IllegalArgumentException("Transaction memo cannot be null");
        }
        if (counterparty == null) {
            throw new IllegalArgumentException("Transaction counterparty cannot be null");
        }
        if (amount == null) {
            throw new IllegalArgumentException("Transaction amount cannot be null");
        }
        if (category == null) {
            throw new IllegalArgumentException("Transaction category cannot be null");
        }
        
        // Validate amount precision (as per requirement - not more than 2 decimal places)
        if (amount.scale() > 2) {
            throw new IllegalArgumentException("Transaction amount cannot have more than 2 decimal places");
        }
        
        this.id = id;
        this.date = date;
        this.memo = memo;
        this.counterparty = counterparty;
        // Ensure amount is stored with exactly 2 decimal places
        this.amount = amount.setScale(2, RoundingMode.HALF_EVEN);
        this.category = category;
    }

    /**
     * @return The sub-category of this transaction, or null if none exists
     */
    public String getSubCategory() {
        return category.getSubCategory();
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Transaction{id=%s, date=%s, memo='%s', counterparty=%s, amount=%s, category=%s}",
                id, date, memo, counterparty, amount, category);
    }
}
