package net.mossworks.buoyancy.adapter.cli;

import net.mossworks.buoyancy.adapter.cli.classify.ClassifyController;

import java.io.PrintStream;
import java.util.Scanner;

public class MainMenuController {

    private final PrintStream out;
    private final Scanner in;
    private final ClassifyController classifyController;

    public MainMenuController(PrintStream out, Scanner in, ClassifyController classifyController) {
        this.out = out;
        this.in = in;
        this.classifyController = classifyController;
    }

    public void run() {
        while (true) {
            out.println();
            out.println("What would you like to do?");
            out.println("  1. Classify a transaction");
            out.println("  2. Exit");
            out.print("> ");

            String choice = in.nextLine().trim();
            switch (choice) {
                case "1" -> classifyController.run();
                case "2" -> { return; }
                default  -> out.println("Unknown option: " + choice);
            }
        }
    }
}
