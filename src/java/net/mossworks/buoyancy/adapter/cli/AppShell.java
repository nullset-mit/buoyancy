package net.mossworks.buoyancy.adapter.cli;

import org.jline.terminal.Terminal;

/**
 * AppShell
 *
 * This class application lifecycle (startup, shutdown) and delegates further interaction to the top-level controller (currently MainMenuController)
 *
 **/
public class AppShell {

    private final Terminal terminal;
    private final MainMenuController mainMenuController;

    public AppShell(Terminal terminal, MainMenuController mainMenuController) {
        this.terminal = terminal;
        this.mainMenuController = mainMenuController;
    }

    public void run() {
        terminal.writer().println("Welcome to Buoyancy.");
        terminal.writer().flush();
        mainMenuController.run();
        terminal.writer().println("Goodbye.");
        terminal.writer().flush();
    }
}
