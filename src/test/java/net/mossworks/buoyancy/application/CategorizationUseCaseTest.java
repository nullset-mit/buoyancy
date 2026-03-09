package net.mossworks.buoyancy.application;

import net.mossworks.buoyancy.application.dto.UnclassifiedTransaction;
import net.mossworks.buoyancy.application.TransactionClassifier;
import net.mossworks.buoyancy.domain.Category;
import net.mossworks.buoyancy.domain.Counterparty;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class CategorizationUseCaseTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 3, 2);

    private UnclassifiedTransaction transaction(String memo, double amount) {
        return new UnclassifiedTransaction(memo, BigDecimal.valueOf(amount), TODAY);
    }

    @Test
    public void classify_delegatesToClassifier_andReturnsCounterparty() {
        Counterparty expected = new Counterparty("Grocery Store", new Category("Groceries"));
        TransactionClassifier stubClassifier = new TransactionClassifier() {
            @Override public Counterparty classify(UnclassifiedTransaction tx) { return expected; }
        };
        CategorizationUseCase useCase = new CategorizationUseCase(stubClassifier);

        Counterparty result = useCase.classify(transaction("GROCERY MART", -57.32));

        assertSame(expected, result);
    }

    @Test
    public void classify_returnsNull_whenClassifierFindsNoMatch() {
        TransactionClassifier stubClassifier = new TransactionClassifier() {
            @Override public Counterparty classify(UnclassifiedTransaction tx) { return null; }
        };
        CategorizationUseCase useCase = new CategorizationUseCase(stubClassifier);

        Counterparty result = useCase.classify(transaction("UNKNOWN VENDOR", -12.00));

        assertNull(result);
    }

    @Test
    public void classify_throwsException_whenTransactionIsNull() {
        TransactionClassifier stubClassifier = new TransactionClassifier() {
            @Override public Counterparty classify(UnclassifiedTransaction tx) { return null; }
        };
        CategorizationUseCase useCase = new CategorizationUseCase(stubClassifier);

        assertThrows(IllegalArgumentException.class, () -> useCase.classify(null));
    }

    @Test
    public void constructor_throwsException_whenClassifierIsNull() {
        assertThrows(IllegalArgumentException.class, () -> new CategorizationUseCase(null));
    }
}
