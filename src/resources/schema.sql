CREATE TABLE IF NOT EXISTS category (
    id           TEXT PRIMARY KEY,
    name         TEXT NOT NULL,
    sub_category TEXT
);

CREATE TABLE IF NOT EXISTS counterparty (
    id                  TEXT PRIMARY KEY,
    name                TEXT NOT NULL,
    default_category_id TEXT NOT NULL,
    FOREIGN KEY (default_category_id) REFERENCES category(id)
);

CREATE TABLE IF NOT EXISTS classification_rule (
    id               TEXT PRIMARY KEY,
    memo_pattern     TEXT    NOT NULL,
    priority         INTEGER NOT NULL DEFAULT 0,
    counterparty_id  TEXT    NOT NULL,
    amount_type      TEXT    NOT NULL,
    FOREIGN KEY (counterparty_id) REFERENCES counterparty(id)
);
