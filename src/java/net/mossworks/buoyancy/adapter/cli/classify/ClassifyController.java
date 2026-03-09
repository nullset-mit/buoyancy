package net.mossworks.buoyancy.adapter.cli.classify;

import net.mossworks.buoyancy.adapter.cli.AbstractController;
import net.mossworks.buoyancy.adapter.cli.widget.SaveResult;
import net.mossworks.buoyancy.adapter.cli.widget.SelectableType;
import net.mossworks.buoyancy.adapter.cli.widget.SelectionResult;
import net.mossworks.buoyancy.adapter.cli.widget.SelectionWidget;
import net.mossworks.buoyancy.application.CategorizationUseCase;
import net.mossworks.buoyancy.application.DuplicateCategoryException;
import net.mossworks.buoyancy.application.DuplicateCounterpartyException;
import net.mossworks.buoyancy.application.RuleCreationUseCase;
import net.mossworks.buoyancy.application.dto.UnclassifiedTransaction;
import net.mossworks.buoyancy.domain.Category;
import net.mossworks.buoyancy.domain.Counterparty;
import net.mossworks.buoyancy.util.TransactionParser;
import org.jline.terminal.Terminal;

import java.util.List;

public class ClassifyController extends AbstractController {

    private final CategorizationUseCase categorizationUseCase;
    private final RuleCreationUseCase ruleCreationUseCase;

    public ClassifyController(Terminal terminal,
                              CategorizationUseCase categorizationUseCase,
                              RuleCreationUseCase ruleCreationUseCase) {
        super(terminal);
        this.categorizationUseCase = categorizationUseCase;
        this.ruleCreationUseCase = ruleCreationUseCase;
    }

    public void run() {
        terminal.writer().print("Enter transaction (mm/dd/yyyy memo amount): ");
        terminal.writer().flush();

        String input = readLine();
        if (input == null) return;

        UnclassifiedTransaction transaction;
        try {
            transaction = TransactionParser.parse(input);
        }
        catch (IllegalArgumentException e) {
            terminal.writer().println("Error: " + e.getMessage());
            terminal.writer().flush();
            return;
        }

        Counterparty counterparty = categorizationUseCase.classify(transaction);

        if (counterparty != null) {
            String subCategory = counterparty.getDefaultCategory().hasSubCategory()
                ? " / " + counterparty.getDefaultCategory().getSubCategory()
                : "";
            terminal.writer().printf("Counterparty : %s%n", counterparty.getName());
            terminal.writer().printf("Category     : %s%s%n",
                counterparty.getDefaultCategory().getName(), subCategory);
            terminal.writer().flush();
        }
        else {
            terminal.writer().println("No matching rule found. Let's create a classification rule.");
            terminal.writer().flush();
            Category category = selectOrCreateCategory();
            if (category == null) return;
            Counterparty cp = selectOrCreateCounterparty(category);
            if (cp == null) return;
            terminal.writer().println("Counterparty: " + cp.getName());
            terminal.writer().println("Category: " + category.getName());
            terminal.writer().flush();
        }
    }

    private Category selectOrCreateCategory() {
        List<Category> categories = ruleCreationUseCase.listCategories();
        SelectableType<Category> type = new SelectableType<>("Category", Category::getName);
        SelectionWidget<Category> widget = new SelectionWidget<>(terminal, categories, type,
            name -> {
                try {
                    Category created = ruleCreationUseCase.createCategory(name);
                    return new SaveResult.Ok<>(created);
                }
                catch (DuplicateCategoryException e) {
                    return new SaveResult.Duplicate<>(e.getMessage());
                }
            });
        SelectionResult<Category> result = widget.run();
        return switch (result) {
            case SelectionResult.Selected<Category> s -> s.item();
            case SelectionResult.Created<Category> c -> c.item();
        };
    }

    private Counterparty selectOrCreateCounterparty(Category category) {
        List<Counterparty> counterparties = ruleCreationUseCase.listCounterpartiesByCategory(category);
        SelectableType<Counterparty> type = new SelectableType<>("Counterparty", Counterparty::getName);
        SelectionWidget<Counterparty> widget = new SelectionWidget<>(terminal, counterparties, type,
            name -> {
                try {
                    Counterparty created = ruleCreationUseCase.createCounterparty(name, category);
                    return new SaveResult.Ok<>(created);
                }
                catch (DuplicateCounterpartyException e) {
                    return new SaveResult.Duplicate<>(e.getMessage());
                }
            });
        SelectionResult<Counterparty> result = widget.run();
        return switch (result) {
            case SelectionResult.Selected<Counterparty> s -> s.item();
            case SelectionResult.Created<Counterparty> c -> c.item();
        };
    }
}
