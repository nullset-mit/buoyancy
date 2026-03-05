package net.mossworks.buoyancy.adapter.cli;

import java.io.PrintStream;

public class AppShell {

    private final PrintStream out;
    private final MainMenuController mainMenuController;

    public AppShell(PrintStream out, MainMenuController mainMenuController) {
        this.out = out;
        this.mainMenuController = mainMenuController;
    }

    public void run() {
        out.println("Welcome to Buoyancy.");
        mainMenuController.run();
        out.println("Goodbye.");
    }
}
