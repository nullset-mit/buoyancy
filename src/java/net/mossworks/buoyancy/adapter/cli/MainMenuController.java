package net.mossworks.buoyancy.adapter.cli;

import net.mossworks.buoyancy.adapter.cli.classify.ClassifyController;
import org.jline.terminal.Terminal;

public class MainMenuController extends AbstractController {

    private final ClassifyController classifyController;

    public MainMenuController(Terminal terminal, ClassifyController classifyController) {
        super(terminal);
        this.classifyController = classifyController;
    }

    public void run() {
        while (true) {
            terminal.writer().println();
            terminal.writer().println("What would you like to do?");
            terminal.writer().println("  1. Classify a transaction");
            terminal.writer().println("  2. Exit");
            terminal.writer().print("> ");
            terminal.writer().flush();

            String choice = readLine();
            if (choice == null) return; // EOF
            switch (choice.trim()) {
                case "1" -> classifyController.run();
                case "2" -> { return; }
                default  -> terminal.writer().println("Unknown option: " + choice.trim());
            }
        }
    }
}
