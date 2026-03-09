package net.mossworks.buoyancy.application.repository;

import net.mossworks.buoyancy.domain.Category;
import net.mossworks.buoyancy.domain.Counterparty;

import java.util.List;

public interface CounterpartyRepository {
    void writeCounterparty(Counterparty counterparty);
    List<Counterparty> listCounterpartiesByCategory(Category category);
}
