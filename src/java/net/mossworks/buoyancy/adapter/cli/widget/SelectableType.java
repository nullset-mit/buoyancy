package net.mossworks.buoyancy.adapter.cli.widget;

import java.util.function.Function;

/**
 * Display metadata for the SelectionWidget: the human-readable type name
 * (e.g. "Category") and a function that extracts a display string from each item.
 */
public record SelectableType<T>(String typeName, Function<T, String> displayFn) {}
