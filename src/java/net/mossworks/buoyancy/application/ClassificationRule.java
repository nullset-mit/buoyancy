package net.mossworks.buoyancy.application;

import lombok.Getter;

import java.util.Objects;
import java.util.UUID;

import net.mossworks.buoyancy.domain.Counterparty;
import net.mossworks.buoyancy.domain.Category;

/**
 * Represents a rule used to classify transactions based on their memo pattern and amount type.
 */
@Getter
public class ClassificationRule {
    
    /**
     * Enum representing the types of amounts a rule can match.
     */
    public enum AmountType {
        CREDIT, DEBIT, BOTH
    }
    
    private final UUID id;
    private final String memoPattern;
    private final int priority;
    private final Counterparty counterparty;
    private final AmountType amountType;

    /**
     * Constructs a new ClassificationRule with a generated ID and default priority.
     *
     * @param memoPattern The pattern to match against transaction memos
     * @param counterparty The counterparty to assign to matching transactions
     * @param amountType Whether this rule applies to credits, debits, or both
     */
    public ClassificationRule(String memoPattern, Counterparty counterparty,
			      AmountType amountType) {
        this(UUID.randomUUID(), memoPattern, 0, counterparty, amountType);
    }

    /**
     * Constructs a new ClassificationRule with a generated ID.
     *
     * @param memoPattern The pattern to match against transaction memos
     * @param priority The rule priority, higher numbers take precedence
     * @param counterparty The counterparty to assign to matching transactions
     * @param amountType Whether this rule applies to credits, debits, or both
     */
    public ClassificationRule(String memoPattern, int priority,
			      Counterparty counterparty, AmountType amountType) {
        this(UUID.randomUUID(), memoPattern, priority, counterparty, amountType);
    }

    /**
     * Constructs a new ClassificationRule.
     *
     * @param id The UUID of this rule
     * @param memoPattern The pattern to match against transaction memos
     * @param priority The rule priority, higher numbers take precedence
     * @param counterparty The counterparty to assign to matching transactions
     * @param amountType Whether this rule applies to credits, debits, or both
     * @throws IllegalArgumentException if any required parameter is invalid
     */
    public ClassificationRule(UUID id, String memoPattern, int priority,
                              Counterparty counterparty, AmountType amountType) {
        if (id == null) {
            throw new IllegalArgumentException("Rule ID cannot be null");
        }
        if (memoPattern == null || memoPattern.trim().isEmpty()) {
            throw new IllegalArgumentException("Memo pattern cannot be null or empty");
        }
        if (counterparty == null) {
            throw new IllegalArgumentException("Counterparty cannot be null");
        }
        if (amountType == null) {
            throw new IllegalArgumentException("Amount type cannot be null");
        }
        
        this.id = id;
        this.memoPattern = memoPattern;
        this.priority = priority;
        this.counterparty = counterparty;
        this.amountType = amountType;
    }

    /**
     * Gets the category for this rule, which is the default category of the counterparty.
     * 
     * @return The category for transactions matching this rule
     */
    public Category getCategory() {
        return counterparty.getDefaultCategory();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClassificationRule that = (ClassificationRule) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("ClassificationRule{id=%s, memoPattern='%s', priority=%d, amountType=%s, Counterparty=%s}",
			     id, memoPattern, priority, amountType, counterparty);
    }
}
