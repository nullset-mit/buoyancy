package net.mossworks.buoyancy.adapter.cli;

import org.jline.terminal.Terminal;
import org.jline.utils.ClosedException;

import java.io.IOException;

public abstract class AbstractController {

    protected final Terminal terminal;

    protected AbstractController(Terminal terminal) {
        this.terminal = terminal;
    }

    protected String readLine() {
        StringBuilder sb = new StringBuilder();
        try {
            int c;
            while ((c = terminal.reader().read()) != -1) {
                if (c == '\n') break;
                if (c == '\r') continue;
                sb.append((char) c);
            }
        }
        catch (ClosedException e) {
            if (sb.isEmpty()) return null;
        }
        catch (IOException e) {
            if (sb.isEmpty()) return null;
        }
        return sb.toString();
    }
}
