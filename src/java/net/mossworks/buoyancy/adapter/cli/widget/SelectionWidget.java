package net.mossworks.buoyancy.adapter.cli.widget;

import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal;
import org.jline.utils.ClosedException;
import org.jline.utils.NonBlockingReader;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class SelectionWidget<T> {

    private static final int CONTENT_ROWS = 5;
    // title + top + content + bottom + status
    private static final int WIDGET_HEIGHT = 1 + 1 + CONTENT_ROWS + 1 + 1;

    private static final String ORANGE  = "\033[33m";
    private static final String RED     = "\033[31m";
    private static final String RESET   = "\033[0m";
    private static final String BOLD    = "\033[1m";
    private static final String INVERSE = "\033[7m";
    private static final String BLINK   = "\033[5m";

    private enum Pane { INPUT, LIST, CREATE }

    private final Terminal terminal;
    // The full list of items to choose from — e.g. all Categories or all Counterparties.
    // The generic type T is determined by the caller (Category, Counterparty, etc.).
    private final List<T> items;
    // Carries the type's display name (e.g. "Category") and a function that
    // converts each item of type T to a display string (e.g. Category::getName).
    private final SelectableType<T> type;
    // Invoked when the user confirms entity creation in create mode.
    // Returns SaveResult.Ok(T) on success or SaveResult.Duplicate(message) on name clash.
    private final Function<String, SaveResult<T>> saveFn;

    private String inputText = "";
    private List<T> filteredItems;
    private Pane focusedPane = Pane.INPUT;
    private int highlightIndex = 0;
    private int scrollOffset = 0;
    private boolean firstRender = true;
    private boolean createMode = false;
    private boolean duplicateError = false;
    private String duplicateMessage = "";

    public SelectionWidget(Terminal terminal, List<T> items, SelectableType<T> type,
                           Function<String, SaveResult<T>> saveFn) {
        this.terminal = terminal;
        this.items = new ArrayList<>(items);
        this.type = type;
        this.saveFn = saveFn;
        this.filteredItems = new ArrayList<>(items);
    }

    public SelectionResult<T> run() {
        PrintWriter w = terminal.writer();
        Attributes saved = null;

        w.print("\033[?25l"); // hide cursor
        w.flush();

        try {
            saved = terminal.enterRawMode();
            render(w);

            NonBlockingReader reader = terminal.reader();
            while (true) {
                int c;
                try {
                    c = reader.read();
                }
                catch (ClosedException e) {
                    break;
                }
                if (c == -1) break;

                if (c == 27) {
                    SelectionResult<T> result = handleEscape(reader);
                    if (result != null) {
                        clearWidget(w);
                        return result;
                    }
                }
                else if (c == 13 || c == 10) {
                    SelectionResult<T> result = handleEnter();
                    if (result != null) {
                        clearWidget(w);
                        return result;
                    }
                }
                else if (c == 127 || c == 8) {
                    if (focusedPane == Pane.INPUT || createMode) {
                        if (!inputText.isEmpty()) {
                            inputText = inputText.substring(0, inputText.length() - 1);
                            if (!createMode) updateFilter();
                            else clearDuplicateError();
                        }
                    }
                }
                else if (c >= 32 && c < 127) {
                    if (focusedPane == Pane.INPUT || createMode) {
                        inputText += (char) c;
                        if (!createMode) updateFilter();
                        else clearDuplicateError();
                    }
                }

                render(w);
            }
        }
        catch (IOException e) {
            throw new RuntimeException("Terminal read error", e);
        }
        finally {
            if (saved != null) terminal.setAttributes(saved);
            w.print("\033[?25h"); // show cursor
            w.flush();
        }

        // EOF — return whatever the input text is as a create (degenerate path)
        return new SelectionResult.Created<>(null);
    }

    // Returns non-null SelectionResult if ESC exits create mode and returns to normal,
    // or null if it was a navigation escape sequence that was handled internally.
    private SelectionResult<T> handleEscape(NonBlockingReader reader) throws IOException {
        int c2 = reader.read(100L);
        if (c2 < 0) {
            // Bare ESC — if in create mode, return to normal mode
            if (createMode) {
                createMode = false;
                duplicateError = false;
                duplicateMessage = "";
                focusedPane = Pane.CREATE;
            }
            return null;
        }
        if (c2 != '[') {
            // Non-CSI escape — ignore (c2 byte consumed but unrecognised)
            return null;
        }

        StringBuilder seq = new StringBuilder();
        int c;
        while ((c = reader.read(100L)) >= 0) {
            seq.append((char) c);
            if (c >= 64 && c <= 126) break; // final byte
        }

        String s = seq.toString();
        switch (s) {
            case "1;2C" -> { // Shift+Right
                if (!createMode) focusedPane = nextPane(focusedPane);
            }
            case "1;2D" -> { // Shift+Left
                if (!createMode) focusedPane = prevPane(focusedPane);
            }
            case "A" -> {   // Up arrow
                if (focusedPane == Pane.LIST && !createMode && highlightIndex > 0) {
                    highlightIndex--;
                    if (highlightIndex < scrollOffset) scrollOffset = highlightIndex;
                }
            }
            case "B" -> {   // Down arrow
                if (focusedPane == Pane.LIST && !createMode && highlightIndex < filteredItems.size() - 1) {
                    highlightIndex++;
                    if (highlightIndex >= scrollOffset + CONTENT_ROWS)
                        scrollOffset = highlightIndex - CONTENT_ROWS + 1;
                }
            }
        }
        return null;
    }

    // Returns a SelectionResult whose generic type matches this widget's T.
    // In create mode: calls saveFn; on Ok returns Created(item); on Duplicate shows error.
    // In normal mode: Selected wraps the chosen item (a T).
    private SelectionResult<T> handleEnter() {
        if (createMode) {
            if (duplicateError) return null; // blocked until name changes
            SaveResult<T> saveResult = saveFn.apply(inputText);
            return switch (saveResult) {
                case SaveResult.Ok<T> ok -> new SelectionResult.Created<>(ok.item());
                case SaveResult.Duplicate<T> dup -> {
                    duplicateError = true;
                    duplicateMessage = dup.message();
                    yield null;
                }
            };
        }

        if (focusedPane == Pane.CREATE) {
            // Enter create mode
            createMode = true;
            focusedPane = Pane.INPUT;
            return null;
        }

        if (filteredItems.isEmpty()) {
            // Enter create mode from empty list
            createMode = true;
            focusedPane = Pane.INPUT;
            return null;
        }

        return switch (focusedPane) {
            case INPUT -> new SelectionResult.Selected<>(
                filteredItems.get(Math.min(highlightIndex, filteredItems.size() - 1)));
            case LIST  -> new SelectionResult.Selected<>(filteredItems.get(highlightIndex));
            case CREATE -> null; // handled above
        };
    }

    private void clearDuplicateError() {
        if (duplicateError) {
            duplicateError = false;
            duplicateMessage = "";
        }
    }

    // Rebuilds filteredItems from the full list, keeping only items whose display
    // string contains every word the user has typed. type.displayFn().apply(item)
    // calls the display function (e.g. Category::getName) on each item of type T
    // to get the string we filter against.
    private void updateFilter() {
        if (inputText.isEmpty()) {
            filteredItems = new ArrayList<>(items);
        }
        else {
            String[] words = inputText.toLowerCase().split("\\s+");
            filteredItems = items.stream()
                .filter(item -> {
                    String display = type.displayFn().apply(item).toLowerCase();
                    for (String word : words) {
                        if (!display.contains(word)) return false;
                    }
                    return true;
                })
                .toList();
        }
        highlightIndex = Math.min(highlightIndex, Math.max(0, filteredItems.size() - 1));
        scrollOffset = Math.min(scrollOffset, Math.max(0, filteredItems.size() - CONTENT_ROWS));
    }

    private void render(PrintWriter w) {
        int width = Math.max(terminal.getWidth(), 80);
        if (width > 220) width = 220;

        int innerTotal = width - 4; // 2 outer borders + 2 inner dividers
        int p1 = innerTotal / 3;
        int p2 = innerTotal / 3;
        int p3 = innerTotal - p1 - p2;

        if (!firstRender) {
            w.print("\033[" + WIDGET_HEIGHT + "A\r");
        }

        // Title
        line(w, "Select a " + type.typeName() + ":");

        // Top border
        clearLine(w);
        w.print(inputBc()         + "┌" + "─".repeat(p1) + "┬" + RESET);
        w.print(bc(Pane.LIST)     + "─".repeat(p2) + "┬" + RESET);
        w.print(bc(Pane.CREATE)   + "─".repeat(p3) + "┐" + RESET);
        w.print("\n");

        // Content rows
        for (int row = 0; row < CONTENT_ROWS; row++) {
            clearLine(w);
            w.print(inputBc()       + "│" + RESET + inputContent(row, p1));
            w.print(bc(Pane.LIST)   + "│" + RESET + listContent(row, p2));
            w.print(bc(Pane.CREATE) + "│" + RESET + createContent(row, p3));
            w.print(bc(Pane.CREATE) + "│" + RESET);
            w.print("\n");
        }

        // Bottom border
        clearLine(w);
        w.print(inputBc()         + "└" + "─".repeat(p1) + "┴" + RESET);
        w.print(bc(Pane.LIST)     + "─".repeat(p2) + "┴" + RESET);
        w.print(bc(Pane.CREATE)   + "─".repeat(p3) + "┘" + RESET);
        w.print("\n");

        // Status
        line(w, "[Type to filter | Shift+Right/Left: move focus | Enter: confirm]");

        w.flush();
        firstRender = false;
    }

    private String inputContent(int row, int width) {
        if (row == 0) {
            String text = "> " + inputText;
            String cursor = (focusedPane == Pane.INPUT) ? BLINK + "_" + RESET : "";
            return visiblePad(text, text.length(), width) + cursor
                + " ".repeat(Math.max(0, width - text.length() - 1));
        }
        return " ".repeat(width);
    }

    private String listContent(int row, int width) {
        if (createMode) {
            // In create mode the list pane shows the duplicate error (if any)
            if (duplicateError && row == 0) {
                String msg = " " + truncate(duplicateMessage, width - 2);
                return msg + " ".repeat(Math.max(0, width - msg.length()));
            }
            return " ".repeat(width);
        }

        int itemIndex = scrollOffset + row;
        if (itemIndex < filteredItems.size()) {
            // Get the display string for this item (e.g. "Groceries" for a Category)
            String name = type.displayFn().apply(filteredItems.get(itemIndex));
            String visible = " " + truncate(name, width - 2);
            boolean highlighted = focusedPane == Pane.LIST && itemIndex == highlightIndex;
            String display = highlighted ? INVERSE + BOLD + visible + RESET : visible;
            return display + " ".repeat(Math.max(0, width - visible.length()));
        }
        return " ".repeat(width);
    }

    private String createContent(int row, int width) {
        if (createMode) {
            // Right pane shows save/cancel instructions
            // "Press Enter\nto Save.\n\nEsc to Cancel."
            String[] lines = {"Press Enter", "to Save.", "", "Esc to Cancel."};
            if (row < lines.length) {
                String text = " " + truncate(lines[row], width - 2);
                String display = INVERSE + BOLD + text + RESET;
                return display + " ".repeat(Math.max(0, width - text.length()));
            }
            return " ".repeat(width);
        }

        if (row == 0) {
            // Uses the type name (e.g. "Category") to label the create action
            String label = "<Create New " + type.typeName() + ">";
            String visible = " " + truncate(label, width - 2);
            boolean highlighted = focusedPane == Pane.CREATE;
            String display = highlighted ? INVERSE + BOLD + visible + RESET : visible;
            return display + " ".repeat(Math.max(0, width - visible.length()));
        }
        return " ".repeat(width);
    }

    private void clearWidget(PrintWriter w) {
        if (!firstRender) {
            w.print("\033[" + WIDGET_HEIGHT + "A\r");
            for (int i = 0; i < WIDGET_HEIGHT; i++) {
                w.print("\033[2K\n");
            }
            w.print("\033[" + WIDGET_HEIGHT + "A\r");
            w.flush();
        }
    }

    // Border color for the INPUT pane: red when duplicate error, orange when focused, reset otherwise.
    private String inputBc() {
        if (createMode && duplicateError) return RED;
        if (focusedPane == Pane.INPUT || createMode) return ORANGE;
        return RESET;
    }

    private String bc(Pane pane) {
        return focusedPane == pane ? ORANGE : RESET;
    }

    private static void line(PrintWriter w, String text) {
        clearLine(w);
        w.print(text + "\n");
    }

    private static void clearLine(PrintWriter w) {
        w.print("\r\033[2K");
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    private static String visiblePad(String visible, int visibleLen, int width) {
        // Returns the visible string padded — caller adds display decorations separately
        return visible + " ".repeat(Math.max(0, width - visibleLen));
    }

    private static Pane nextPane(Pane p) {
        return switch (p) {
            case INPUT -> Pane.LIST;
            case LIST  -> Pane.CREATE;
            case CREATE -> Pane.INPUT;
        };
    }

    private static Pane prevPane(Pane p) {
        return switch (p) {
            case INPUT  -> Pane.CREATE;
            case LIST   -> Pane.INPUT;
            case CREATE -> Pane.LIST;
        };
    }
}
