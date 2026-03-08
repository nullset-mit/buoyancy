package net.mossworks.buoyancy.adapter.cli.widget;

import net.mossworks.buoyancy.domain.Category;
import org.jline.terminal.Terminal;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class SelectionWidgetTest {

    private static final SelectableType<Category> CATEGORY_TYPE =
        new SelectableType<>("Category", Category::getName);

    private static final byte[] SHIFT_RIGHT = {27, '[', '1', ';', '2', 'C'};
    private static final byte[] SHIFT_LEFT  = {27, '[', '1', ';', '2', 'D'};
    private static final byte[] ARROW_DOWN  = {27, '[', 'B'};
    private static final byte   ENTER       = 13;
    private static final byte   ESC         = 27;

    // saveFn that always succeeds, creating a Category with the given name
    private static final Function<String, SaveResult<Category>> SAVE_OK =
        name -> new SaveResult.Ok<>(new Category(name));

    // saveFn that always returns Duplicate
    private static final Function<String, SaveResult<Category>> SAVE_DUPLICATE =
        name -> new SaveResult.Duplicate<>("Category '" + name + "' already exists");

    private SelectionResult<Category> runWidget(List<Category> items, byte[] inputBytes)
            throws IOException {
        return runWidget(items, inputBytes, SAVE_OK);
    }

    private SelectionResult<Category> runWidget(List<Category> items, byte[] inputBytes,
                                                Function<String, SaveResult<Category>> saveFn)
            throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        Terminal terminal = new org.jline.terminal.impl.DumbTerminal(
            "test", org.jline.terminal.Terminal.TYPE_DUMB,
            new ByteArrayInputStream(inputBytes), buf,
            java.nio.charset.StandardCharsets.UTF_8);
        return new SelectionWidget<>(terminal, items, CATEGORY_TYPE, saveFn).run();
    }

    private static byte[] concat(byte[]... parts) {
        int len = 0;
        for (byte[] p : parts) len += p.length;
        byte[] result = new byte[len];
        int pos = 0;
        for (byte[] p : parts) { System.arraycopy(p, 0, result, pos, p.length); pos += p.length; }
        return result;
    }

    @Test
    void run_entersCreateMode_whenCreatePaneSelectedAndEnterPressed() throws IOException {
        // After entering create mode, pressing Enter calls saveFn → Created
        List<Category> items = List.of(new Category("Groceries"));
        // Type "New", Shift+Right twice → CREATE pane, Enter → enter create mode,
        // then Enter again → saveFn returns Ok → Created
        byte[] typed = {'N', 'e', 'w'};
        SelectionResult<Category> result = runWidget(items,
            concat(typed, SHIFT_RIGHT, SHIFT_RIGHT, new byte[]{ENTER}, new byte[]{ENTER}));
        assertInstanceOf(SelectionResult.Created.class, result);
        assertEquals("New", ((SelectionResult.Created<Category>) result).item().getName());
    }

    @Test
    void run_returnsCreated_whenSaveSucceeds() throws IOException {
        List<Category> items = List.of();
        // Empty list: Enter immediately enters create mode; type name; Enter saves
        byte[] typed = {'F', 'o', 'o', 'd'};
        SelectionResult<Category> result = runWidget(items,
            concat(new byte[]{ENTER}, typed, new byte[]{ENTER}));
        assertInstanceOf(SelectionResult.Created.class, result);
        assertEquals("Food", ((SelectionResult.Created<Category>) result).item().getName());
    }

    @Test
    void run_showsDuplicateError_whenSaveReturnsDuplicate() throws IOException {
        // First Enter → create mode; type "Shopping"; Enter → Duplicate; type "x"; Enter → Ok
        List<Category> items = List.of();
        byte[] typed = {'S', 'h', 'o', 'p', 'p', 'i', 'n', 'g'};
        byte[] typed2 = {'x'};
        // SAVE_DUPLICATE on first attempt, then we append 'x' which clears error,
        // then second Enter → SAVE_DUPLICATE again (still same fn). Use a counter fn:
        int[] calls = {0};
        Function<String, SaveResult<Category>> fn = name -> {
            calls[0]++;
            if (calls[0] == 1) return new SaveResult.Duplicate<>("Category '" + name + "' already exists");
            return new SaveResult.Ok<>(new Category(name));
        };
        SelectionResult<Category> result = runWidget(items,
            concat(new byte[]{ENTER}, typed, new byte[]{ENTER}, typed2, new byte[]{ENTER}), fn);
        assertInstanceOf(SelectionResult.Created.class, result);
        assertEquals(2, calls[0]);
    }

    @Test
    void run_blocksEnter_whileDuplicateErrorShowing() throws IOException {
        // Enter → create mode; type name; Enter → Duplicate (blocks second Enter);
        // another Enter → still blocked; type 'x'; Enter → Ok
        List<Category> items = List.of();
        byte[] typed = {'A'};
        byte[] extra  = {'B'};
        int[] calls = {0};
        Function<String, SaveResult<Category>> fn = name -> {
            calls[0]++;
            if (calls[0] == 1) return new SaveResult.Duplicate<>("already exists");
            return new SaveResult.Ok<>(new Category(name));
        };
        // First Enter → create mode; type 'A'; Enter → Duplicate;
        // Enter → blocked (no save call); type 'B'; Enter → Ok
        SelectionResult<Category> result = runWidget(items,
            concat(new byte[]{ENTER}, typed, new byte[]{ENTER},
                   new byte[]{ENTER}, extra, new byte[]{ENTER}), fn);
        assertInstanceOf(SelectionResult.Created.class, result);
        assertEquals(2, calls[0]); // only 2 real save attempts, not 3
    }

    @Test
    void run_returnsToNormalMode_onEscInCreateMode() throws IOException {
        // Enter create mode, type a name, press ESC — saveFn should NOT be called.
        // ESC is the last byte so read(100L) returns -1 (timeout), triggering create-mode cancel.
        Category groceries = new Category("Groceries");
        List<Category> items = List.of(groceries);
        int[] saveCalls = {0};
        Function<String, SaveResult<Category>> fn = name -> {
            saveCalls[0]++;
            return new SaveResult.Ok<>(new Category(name));
        };
        // Navigate to CREATE pane (SHIFT_RIGHT×2), ENTER → create mode, type "New", ESC → cancel
        byte[] typed = {'N', 'e', 'w'};
        runWidget(items, concat(SHIFT_RIGHT, SHIFT_RIGHT, new byte[]{ENTER}, typed, new byte[]{ESC}), fn);
        assertEquals(0, saveCalls[0]); // ESC cancelled create mode — saveFn never invoked
    }

    @Test
    void run_returnsSelected_whenItemPickedFromList() throws IOException {
        Category groceries = new Category("Groceries");
        Category shopping  = new Category("Shopping");
        List<Category> items = List.of(groceries, shopping);
        // Shift+Right: INPUT → LIST, Enter on first item (Groceries)
        SelectionResult<Category> result = runWidget(items,
            concat(SHIFT_RIGHT, new byte[]{ENTER}));
        assertInstanceOf(SelectionResult.Selected.class, result);
        assertEquals("Groceries", ((SelectionResult.Selected<Category>) result).item().getName());
    }

    @Test
    void run_returnsSelected_secondItemAfterArrowDown() throws IOException {
        Category groceries = new Category("Groceries");
        Category shopping  = new Category("Shopping");
        List<Category> items = List.of(groceries, shopping);
        // Shift+Right → LIST, Arrow Down, Enter
        SelectionResult<Category> result = runWidget(items,
            concat(SHIFT_RIGHT, ARROW_DOWN, new byte[]{ENTER}));
        assertInstanceOf(SelectionResult.Selected.class, result);
        assertEquals("Shopping", ((SelectionResult.Selected<Category>) result).item().getName());
    }

    @Test
    void run_filtersItems_wordLevelAnyOrder() throws IOException {
        // Type "gro" → only Groceries should match; Shift+Right to LIST; Enter
        Category groceries = new Category("Groceries");
        Category household = new Category("Household");
        List<Category> items = List.of(groceries, household);
        byte[] typed = {'g', 'r', 'o'};
        SelectionResult<Category> result = runWidget(items,
            concat(typed, SHIFT_RIGHT, new byte[]{ENTER}));
        assertInstanceOf(SelectionResult.Selected.class, result);
        assertEquals("Groceries", ((SelectionResult.Selected<Category>) result).item().getName());
    }
}
