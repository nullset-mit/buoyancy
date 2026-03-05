package net.mossworks.buoyancy.adapter.cli.classify;

import net.mossworks.buoyancy.application.RuleCreationUseCase;
import net.mossworks.buoyancy.application.repository.CategoryRepository;
import net.mossworks.buoyancy.domain.Category;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

public class CategoryCreationWorkflowTest {

    private CategoryCreationWorkflow buildWorkflow(String input, ByteArrayOutputStream buf,
                                                    List<Category> captured) {
        PrintStream out = new PrintStream(buf);
        Scanner in = new Scanner(new ByteArrayInputStream(input.getBytes()));
        CategoryRepository repo = category -> captured.add(category);
        RuleCreationUseCase useCase = new RuleCreationUseCase(repo);
        return new CategoryCreationWorkflow(out, in, useCase);
    }

    @Test
    void run_promptsForCategoryName() {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        buildWorkflow("Shopping\n", buf, new ArrayList<>()).run();
        assertTrue(buf.toString().contains("Enter category name:"));
    }

    @Test
    void run_printsConfirmation() {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        buildWorkflow("Shopping\n", buf, new ArrayList<>()).run();
        assertTrue(buf.toString().contains("Category 'Shopping' created."));
    }

    @Test
    void run_returnsCreatedCategory() {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        Category result = buildWorkflow("Shopping\n", buf, new ArrayList<>()).run();
        assertEquals("Shopping", result.getName());
    }

    @Test
    void run_persistsCategory() {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        List<Category> captured = new ArrayList<>();
        Category result = buildWorkflow("Shopping\n", buf, captured).run();
        assertEquals(1, captured.size());
        assertEquals(result, captured.get(0));
    }
}
