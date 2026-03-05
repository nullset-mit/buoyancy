package net.mossworks.buoyancy.adapter.cli.classify;

import net.mossworks.buoyancy.application.CategorizationUseCase;
import net.mossworks.buoyancy.application.RuleCreationUseCase;
import net.mossworks.buoyancy.application.dto.UnclassifiedTransaction;
import net.mossworks.buoyancy.domain.Counterparty;

import java.io.PrintStream;
import java.util.Scanner;

public class ClassifyController {

    private final PrintStream out;
    private final Scanner in;
    private final CategorizationUseCase categorizationUseCase;
    private final RuleCreationUseCase ruleCreationUseCase;

    public ClassifyController(PrintStream out, Scanner in,
                              CategorizationUseCase categorizationUseCase,
                              RuleCreationUseCase ruleCreationUseCase) {
        this.out = out;
        this.in = in;
        this.categorizationUseCase = categorizationUseCase;
        this.ruleCreationUseCase = ruleCreationUseCase;
    }

    public void run() {
        out.print("Enter transaction (mm/dd/yyyy memo amount): ");
        String input = in.nextLine();

        UnclassifiedTransaction transaction;
        try {
            transaction = TransactionParser.parse(input);
        } catch (IllegalArgumentException e) {
            out.println("Error: " + e.getMessage());
            return;
        }

        Counterparty counterparty = categorizationUseCase.classify(transaction);

        if (counterparty != null) {
            String subCategory = counterparty.getDefaultCategory().hasSubCategory()
                ? " / " + counterparty.getDefaultCategory().getSubCategory()
                : "";
            out.printf("Counterparty : %s%n", counterparty.getName());
            out.printf("Category     : %s%s%n",
                counterparty.getDefaultCategory().getName(), subCategory);
        } else {
            out.println("No matching rule found. Let's create a classification rule.");
            new CategoryCreationWorkflow(out, in, ruleCreationUseCase).run();
        }
    }
}
