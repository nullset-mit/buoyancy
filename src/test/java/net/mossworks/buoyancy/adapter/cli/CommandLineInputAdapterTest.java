package net.mossworks.buoyancy.adapter.cli;

import net.mossworks.buoyancy.application.CategorizationUseCase;
import net.mossworks.buoyancy.application.TransactionClassifier;
import net.mossworks.buoyancy.application.dto.UnclassifiedTransaction;
import net.mossworks.buoyancy.domain.Counterparty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class CommandLineInputAdapterTest {

    private CommandLineInputAdapter adapter;

    @BeforeEach
    public void setUp() {
        TransactionClassifier stub = new TransactionClassifier() {
            @Override public Counterparty classify(UnclassifiedTransaction tx) { return null; }
        };
        CategorizationUseCase useCase = new CategorizationUseCase(stub);
        adapter = new CommandLineInputAdapter(useCase, new String[]{}, System.out);
    }

    @Test
    public void parse_extractsDateMemoAndAmount() {
        UnclassifiedTransaction tx = adapter.parse("03/12/2026 320*WHLFDS Grocery 57.32");

        assertEquals(LocalDate.of(2026, 3, 12), tx.getDate());
        assertEquals("320*WHLFDS Grocery", tx.getMemo());
        assertEquals(new BigDecimal("57.32"), tx.getAmount());
    }

    @Test
    public void parse_handlesMultiWordMemo() {
        UnclassifiedTransaction tx = adapter.parse("01/01/2026 SOME STORE WITH LONG NAME -99.99");

        assertEquals("SOME STORE WITH LONG NAME", tx.getMemo());
        assertEquals(new BigDecimal("-99.99"), tx.getAmount());
    }

    @Test
    public void parse_throwsException_forInvalidDate() {
        assertThrows(IllegalArgumentException.class,
            () -> adapter.parse("2026-03-12 MEMO 50.00"));
    }

    @Test
    public void parse_throwsException_forInvalidAmount() {
        assertThrows(IllegalArgumentException.class,
            () -> adapter.parse("03/12/2026 MEMO notanumber"));
    }

    @Test
    public void parse_throwsException_forTooFewTokens() {
        assertThrows(IllegalArgumentException.class,
            () -> adapter.parse("03/12/2026 57.32"));
    }

    @Test
    public void parse_throwsException_forBlankInput() {
        assertThrows(IllegalArgumentException.class,
            () -> adapter.parse("   "));
    }
}
