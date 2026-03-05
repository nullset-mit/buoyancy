package net.mossworks.buoyancy.application.repository;

import net.mossworks.buoyancy.domain.Category;

public interface CategoryRepository {
    void writeCategory(Category category);
}
