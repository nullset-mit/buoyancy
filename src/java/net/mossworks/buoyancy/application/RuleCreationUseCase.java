package net.mossworks.buoyancy.application;

import net.mossworks.buoyancy.application.repository.CategoryRepository;
import net.mossworks.buoyancy.domain.Category;

public class RuleCreationUseCase {

    private final CategoryRepository categoryRepo;

    public RuleCreationUseCase(CategoryRepository categoryRepo) {
        if (categoryRepo == null) throw new IllegalArgumentException("Category repository cannot be null");
        this.categoryRepo = categoryRepo;
    }

    public Category createCategory(String name) {
        Category category = new Category(name);
        categoryRepo.writeCategory(category);
        return category;
    }
}
