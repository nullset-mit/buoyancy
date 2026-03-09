package net.mossworks.buoyancy.adapter.cli.widget;

/**
 * The outcome of the widget's save callback: either the entity was persisted
 * successfully (Ok) or the name clashed with an existing entry (Duplicate).
 */
public sealed interface SaveResult<T> {
    record Ok<T>(T item) implements SaveResult<T> {}
    record Duplicate<T>(String message) implements SaveResult<T> {}
}
