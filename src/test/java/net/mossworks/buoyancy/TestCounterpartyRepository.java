package net.mossworks.buoyancy;

import net.mossworks.buoyancy.application.repository.CounterpartyRepository;
import net.mossworks.buoyancy.domain.Category;
import net.mossworks.buoyancy.domain.Counterparty;

import java.util.ArrayList;
import java.util.List;

public class TestCounterpartyRepository implements CounterpartyRepository {

    private final List<Counterparty> written = new ArrayList<>();
    private final List<Counterparty> stored;

    public TestCounterpartyRepository() {
        this.stored = new ArrayList<>();
    }

    public TestCounterpartyRepository(List<Counterparty> existingCounterparties) {
        this.stored = new ArrayList<>(existingCounterparties);
    }

    @Override
    public void writeCounterparty(Counterparty counterparty) {
        written.add(counterparty);
        stored.add(counterparty);
    }

    @Override
    public List<Counterparty> listCounterpartiesByCategory(Category category) {
        return stored.stream()
            .filter(cp -> cp.getDefaultCategory().getName()
                .equalsIgnoreCase(category.getName()))
            .toList();
    }

    public List<Counterparty> getWritten() {
        return written;
    }
}
