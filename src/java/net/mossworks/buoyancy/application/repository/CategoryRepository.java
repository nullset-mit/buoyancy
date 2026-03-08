package net.mossworks.buoyancy.application.repository;

import net.mossworks.buoyancy.domain.Category;

import java.util.List;

public interface CategoryRepository {
    void writeCategory(Category category);
    List<Category> listCategories();
}
