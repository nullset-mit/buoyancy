package net.mossworks.buoyancy.application;

import net.mossworks.buoyancy.application.repository.CategoryRepository;
import net.mossworks.buoyancy.application.repository.CounterpartyRepository;
import net.mossworks.buoyancy.domain.Category;
import net.mossworks.buoyancy.domain.Counterparty;

import java.util.List;

public class RuleCreationUseCase {

    private final CategoryRepository categoryRepo;
    private final CounterpartyRepository counterpartyRepo;

    public RuleCreationUseCase(CategoryRepository categoryRepo,
                               CounterpartyRepository counterpartyRepo) {
        if (categoryRepo == null) throw new IllegalArgumentException("Category repository cannot be null");
        if (counterpartyRepo == null) throw new IllegalArgumentException("Counterparty repository cannot be null");
        this.categoryRepo = categoryRepo;
        this.counterpartyRepo = counterpartyRepo;
    }

    public Category createCategory(String name) {
        Category category = new Category(name);
        categoryRepo.writeCategory(category);
        return category;
    }

    public List<Category> listCategories() {
        return categoryRepo.listCategories();
    }

    public Counterparty createCounterparty(String name, Category category) {
        Counterparty cp = new Counterparty(name, category);
        counterpartyRepo.writeCounterparty(cp);
        return cp;
    }

    public List<Counterparty> listCounterpartiesByCategory(Category category) {
        return counterpartyRepo.listCounterpartiesByCategory(category);
    }
}
