package net.mossworks.buoyancy.adapter.cli.classify;

import net.mossworks.buoyancy.application.dto.UnclassifiedTransaction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class TransactionParserTest {

    @Test
    public void parse_extractsDateMemoAndAmount() {
        UnclassifiedTransaction tx = TransactionParser.parse("03/12/2026 320*WHLFDS Grocery 57.32");

        assertEquals(LocalDate.of(2026, 3, 12), tx.getDate());
        assertEquals("320*WHLFDS Grocery", tx.getMemo());
        assertEquals(new BigDecimal("57.32"), tx.getAmount());
    }

    @Test
    public void parse_handlesMultiWordMemo() {
        UnclassifiedTransaction tx = TransactionParser.parse("01/01/2026 SOME STORE WITH LONG NAME -99.99");

        assertEquals("SOME STORE WITH LONG NAME", tx.getMemo());
        assertEquals(new BigDecimal("-99.99"), tx.getAmount());
    }

    @Test
    public void parse_throwsException_forInvalidDate() {
        assertThrows(IllegalArgumentException.class,
            () -> TransactionParser.parse("2026-03-12 MEMO 50.00"));
    }

    @Test
    public void parse_throwsException_forInvalidAmount() {
        assertThrows(IllegalArgumentException.class,
            () -> TransactionParser.parse("03/12/2026 MEMO notanumber"));
    }

    @Test
    public void parse_throwsException_forTooFewTokens() {
        assertThrows(IllegalArgumentException.class,
            () -> TransactionParser.parse("03/12/2026 57.32"));
    }

    @Test
    public void parse_throwsException_forBlankInput() {
        assertThrows(IllegalArgumentException.class,
            () -> TransactionParser.parse("   "));
    }
}
