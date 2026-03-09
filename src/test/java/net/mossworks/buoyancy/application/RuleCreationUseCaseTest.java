package net.mossworks.buoyancy.application;

import net.mossworks.buoyancy.TestCategoryRepository;
import net.mossworks.buoyancy.TestCounterpartyRepository;
import net.mossworks.buoyancy.domain.Category;
import net.mossworks.buoyancy.domain.Counterparty;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RuleCreationUseCaseTest {

    private RuleCreationUseCase useCase() {
        return new RuleCreationUseCase(new TestCategoryRepository(), new TestCounterpartyRepository());
    }

    @Test
    void createCategory_returnsCategory_withGivenName() {
        assertEquals("Shopping", useCase().createCategory("Shopping").getName());
    }

    @Test
    void createCategory_persistsCategory() {
        TestCategoryRepository repo = new TestCategoryRepository();
        Category result = new RuleCreationUseCase(repo, new TestCounterpartyRepository()).createCategory("Shopping");
        assertEquals(1, repo.getWritten().size());
        assertEquals(result, repo.getWritten().get(0));
    }

    @Test
    void createCategory_throwsException_whenNameIsBlank() {
        assertThrows(IllegalArgumentException.class, () -> useCase().createCategory(""));
    }

    @Test
    void listCategories_delegatesToRepository() {
        TestCategoryRepository repo = new TestCategoryRepository(List.of(new Category("Food")));
        List<Category> result = new RuleCreationUseCase(repo, new TestCounterpartyRepository()).listCategories();
        assertEquals(1, result.size());
        assertEquals("Food", result.get(0).getName());
    }

    @Test
    void createCounterparty_persistsAndReturns() {
        TestCounterpartyRepository repo = new TestCounterpartyRepository();
        Category category = new Category("Groceries");
        RuleCreationUseCase uc = new RuleCreationUseCase(new TestCategoryRepository(), repo);
        Counterparty result = uc.createCounterparty("Walmart", category);
        assertEquals("Walmart", result.getName());
        assertEquals(1, repo.getWritten().size());
        assertEquals(result, repo.getWritten().get(0));
    }

    @Test
    void listCounterpartiesByCategory_delegatesToRepo() {
        Category category = new Category("Groceries");
        Counterparty cp = new Counterparty("Walmart", category);
        TestCounterpartyRepository repo = new TestCounterpartyRepository(List.of(cp));
        List<Counterparty> result =
            new RuleCreationUseCase(new TestCategoryRepository(), repo)
                .listCounterpartiesByCategory(category);
        assertEquals(1, result.size());
        assertEquals("Walmart", result.get(0).getName());
    }
}
