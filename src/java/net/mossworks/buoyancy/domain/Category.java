package net.mossworks.buoyancy.domain;

import lombok.Getter;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a category for financial transactions.
 * Categories help organize and group transactions for reporting and analysis.
 */
@Getter
public class Category {
    private final UUID id;
    private final String name;
    private final String subCategory;

    /**
     * Constructs a new Category with a generated UUID and no sub-category.
     *
     * @param name The name of the category
     * @throws IllegalArgumentException if name is null or empty
     */
    public Category(String name) {
        this(UUID.randomUUID(), name, null);
    }

    /**
     * Constructs a new Category with a generated UUID.
     *
     * @param name The name of the category
     * @param subCategory The sub-category (may be null)
     * @throws IllegalArgumentException if name is null or empty
     */
    public Category(String name, String subCategory) {
        this(UUID.randomUUID(), name, subCategory);
    }

    /**
     * Constructs a new Category with the specified ID.
     *
     * @param id The UUID of the category
     * @param name The name of the category
     * @param subCategory The sub-category (may be null)
     * @throws IllegalArgumentException if id is null or name is null or empty
     */
    public Category(UUID id, String name, String subCategory) {
        if (id == null) {
            throw new IllegalArgumentException("Category ID cannot be null");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Category name cannot be null or empty");
        }
        this.id = id;
        this.name = name;
        this.subCategory = subCategory;
    }

    /**
     * @return true if this category has a sub-category, false otherwise
     */
    public boolean hasSubCategory() {
        return subCategory != null && !subCategory.trim().isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;
        return id.equals(category.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Category{id=%s, name='%s', subCategory='%s'}",
			     id,
			     name,
			     subCategory != null ? subCategory : "");
    }
}
