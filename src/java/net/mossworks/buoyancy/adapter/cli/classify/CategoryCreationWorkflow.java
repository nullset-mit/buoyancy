package net.mossworks.buoyancy.adapter.cli.classify;

import net.mossworks.buoyancy.application.RuleCreationUseCase;
import net.mossworks.buoyancy.domain.Category;

import java.io.PrintStream;
import java.util.Scanner;

public class CategoryCreationWorkflow {

    private final PrintStream out;
    private final Scanner in;
    private final RuleCreationUseCase useCase;

    public CategoryCreationWorkflow(PrintStream out, Scanner in, RuleCreationUseCase useCase) {
        this.out = out;
        this.in = in;
        this.useCase = useCase;
    }

    public Category run() {
        out.print("Enter category name: ");
        String name = in.nextLine();
        Category category = useCase.createCategory(name);
        out.println("Category '" + name + "' created.");
        return category;
    }
}
