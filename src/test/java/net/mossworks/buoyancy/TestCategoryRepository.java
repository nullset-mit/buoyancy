package net.mossworks.buoyancy;

import net.mossworks.buoyancy.application.repository.CategoryRepository;
import net.mossworks.buoyancy.domain.Category;

import java.util.ArrayList;
import java.util.List;

public class TestCategoryRepository implements CategoryRepository {

    private final List<Category> written = new ArrayList<>();
    private final List<Category> stored;

    public TestCategoryRepository() {
        this.stored = new ArrayList<>();
    }

    public TestCategoryRepository(List<Category> existingCategories) {
        this.stored = new ArrayList<>(existingCategories);
    }

    @Override
    public void writeCategory(Category category) {
        written.add(category);
        stored.add(category);
    }

    @Override
    public List<Category> listCategories() {
        return List.copyOf(stored);
    }

    public List<Category> getWritten() {
        return written;
    }
}
