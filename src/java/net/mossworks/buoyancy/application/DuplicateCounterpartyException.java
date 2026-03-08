package net.mossworks.buoyancy.application;

public class DuplicateCounterpartyException extends RuntimeException {
    public DuplicateCounterpartyException(String name) {
        super("A counterparty with name '" + name + "' already exists");
    }
}
