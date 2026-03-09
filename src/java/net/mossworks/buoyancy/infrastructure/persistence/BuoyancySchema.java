package net.mossworks.buoyancy.infrastructure.persistence;

import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

/**
 * jOOQ table and field constants for the Buoyancy SQLite schema.
 * Used in standalone mode (no code generation).
 */
final class BuoyancySchema {

    private BuoyancySchema() {}

    static final Table<Record> CATEGORY = DSL.table("category");
    static final Field<String> CATEGORY_ID = DSL.field(DSL.name("category", "id"), SQLDataType.VARCHAR);
    static final Field<String> CATEGORY_NAME = DSL.field(DSL.name("category", "name"), SQLDataType.VARCHAR);
    static final Field<String> CATEGORY_SUB_CATEGORY = DSL.field(DSL.name("category", "sub_category"), SQLDataType.VARCHAR);

    static final Table<Record> COUNTERPARTY = DSL.table("counterparty");
    static final Field<String> COUNTERPARTY_ID = DSL.field(DSL.name("counterparty", "id"), SQLDataType.VARCHAR);
    static final Field<String> COUNTERPARTY_NAME = DSL.field(DSL.name("counterparty", "name"), SQLDataType.VARCHAR);
    static final Field<String> COUNTERPARTY_DEFAULT_CATEGORY_ID = DSL.field(DSL.name("counterparty", "default_category_id"), SQLDataType.VARCHAR);

    static final Table<Record> CLASSIFICATION_RULE = DSL.table("classification_rule");
    static final Field<String> RULE_ID = DSL.field(DSL.name("classification_rule", "id"), SQLDataType.VARCHAR);
    static final Field<String> RULE_MEMO_PATTERN = DSL.field(DSL.name("classification_rule", "memo_pattern"), SQLDataType.VARCHAR);
    static final Field<Integer> RULE_PRIORITY = DSL.field(DSL.name("classification_rule", "priority"), SQLDataType.INTEGER);
    static final Field<String> RULE_COUNTERPARTY_ID = DSL.field(DSL.name("classification_rule", "counterparty_id"), SQLDataType.VARCHAR);
    static final Field<String> RULE_AMOUNT_TYPE = DSL.field(DSL.name("classification_rule", "amount_type"), SQLDataType.VARCHAR);
}
