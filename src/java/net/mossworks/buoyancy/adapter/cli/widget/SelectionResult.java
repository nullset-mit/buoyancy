package net.mossworks.buoyancy.adapter.cli.widget;

/**
 * The outcome of a SelectionWidget interaction: either the user picked
 * an existing item (Selected) or the widget created a new one and persisted
 * it (Created). Both variants carry the domain object of type T.
 */
public sealed interface SelectionResult<T> {
    record Selected<T>(T item) implements SelectionResult<T> {}
    record Created<T>(T item) implements SelectionResult<T> {}
}
