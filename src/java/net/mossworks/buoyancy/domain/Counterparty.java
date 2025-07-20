package net.mossworks.buoyancy.domain;

import lombok.Getter;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a counterparty in a financial transaction.
 * A counterparty is the other party involved in a transaction (e.g., a merchant, service provider).
 */
@Getter
public class Counterparty {
    private final UUID id;
    private final String name;
    private final Category defaultCategory;

    /**
     * Constructs a new Counterparty with a generated UUID.
     *
     * @param name The name of the counterparty
     * @throws IllegalArgumentException if name is null or empty
     */
    public Counterparty(String name, Category defaultCategory) {
        this(UUID.randomUUID(), name, defaultCategory);
    }

    /**
     * Constructs a new Counterparty with the specified ID and name.
     *
     * @param id The UUID of the counterparty
     * @param name The name of the counterparty
     * @throws IllegalArgumentException if id is null or name is null or empty
     */
    public Counterparty(UUID id, String name, Category defaultCategory) {
        if (id == null) {
            throw new IllegalArgumentException("Counterparty ID cannot be null");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Counterparty name cannot be null or empty");
        }
	if (defaultCategory == null) {
	    throw new IllegalArgumentException("Default category cannot be null");
	}
        this.id = id;
        this.name = name;
	this.defaultCategory = defaultCategory;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Counterparty that = (Counterparty) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Counterparty{id=%s, name='%s', defaultCategory=%s}", id, name, defaultCategory);
    }
}
