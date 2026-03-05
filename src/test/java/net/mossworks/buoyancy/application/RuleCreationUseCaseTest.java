package net.mossworks.buoyancy.application;

import net.mossworks.buoyancy.application.repository.CategoryRepository;
import net.mossworks.buoyancy.domain.Category;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RuleCreationUseCaseTest {

    private RuleCreationUseCase buildUseCase(List<Category> captured) {
        CategoryRepository repo = category -> captured.add(category);
        return new RuleCreationUseCase(repo);
    }

    @Test
    void createCategory_returnsCategory_withGivenName() {
        Category result = buildUseCase(new ArrayList<>()).createCategory("Shopping");
        assertEquals("Shopping", result.getName());
    }

    @Test
    void createCategory_persistsCategory() {
        List<Category> captured = new ArrayList<>();
        Category result = buildUseCase(captured).createCategory("Shopping");
        assertEquals(1, captured.size());
        assertEquals(result, captured.get(0));
    }

    @Test
    void createCategory_throwsException_whenNameIsBlank() {
        assertThrows(IllegalArgumentException.class,
            () -> buildUseCase(new ArrayList<>()).createCategory(""));
    }
}
