package net.mossworks.buoyancy.infrastructure.persistence;

import net.mossworks.buoyancy.application.repository.CategoryRepository;
import net.mossworks.buoyancy.application.repository.ClassificationRuleRepository;
import net.mossworks.buoyancy.domain.Category;
import net.mossworks.buoyancy.domain.ClassificationRule;
import net.mossworks.buoyancy.domain.Counterparty;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import static net.mossworks.buoyancy.infrastructure.persistence.BuoyancySchema.*;

public class SQLiteClassificationRuleRepository implements ClassificationRuleRepository, CategoryRepository {

    private final Connection connection;
    private final DSLContext dsl;

    public SQLiteClassificationRuleRepository(Path dbPath) {
        if (dbPath == null) {
            throw new IllegalArgumentException("Database path cannot be null");
        }
        try {
            dbPath.getParent().toFile().mkdirs();
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath.toString());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to open SQLite database at " + dbPath, e);
        }
        this.dsl = createDslContext(this.connection);
        initializeSchema();
    }

    SQLiteClassificationRuleRepository(Connection connection) {
        this.connection = connection;
        this.dsl = createDslContext(connection);
        initializeSchema();
    }

    private DSLContext createDslContext(Connection connection) {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to enable foreign key enforcement", e);
        }
        return DSL.using(connection, SQLDialect.SQLITE);
    }

    private void initializeSchema() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("schema.sql")) {
            if (is == null) {
                throw new RuntimeException("schema.sql not found on classpath");
            }
            String sql = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            for (String statement : sql.split(";")) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty()) {
                    dsl.execute(trimmed);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read schema.sql", e);
        }
    }

    @Override
    public List<ClassificationRule> loadRules() {
        return dsl
            .select()
            .from(CLASSIFICATION_RULE)
            .join(COUNTERPARTY).on(RULE_COUNTERPARTY_ID.eq(COUNTERPARTY_ID))
            .join(CATEGORY).on(COUNTERPARTY_DEFAULT_CATEGORY_ID.eq(CATEGORY_ID))
            .fetch()
            .stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public void writeRule(ClassificationRule rule) {
        boolean exists = dsl.fetchExists(
            dsl.selectFrom(CLASSIFICATION_RULE).where(RULE_ID.eq(rule.getId().toString()))
        );
        if (exists) {
            throw new IllegalArgumentException("A rule with ID " + rule.getId() + " already exists");
        }
        dsl.transaction(txConfig -> {
            DSLContext tx = DSL.using(txConfig);
            ensureCategoryExists(tx, rule.getCategory());
            ensureCounterpartyExists(tx, rule.getCounterparty());
            tx.insertInto(CLASSIFICATION_RULE)
                .set(RULE_ID, rule.getId().toString())
                .set(RULE_MEMO_PATTERN, rule.getMemoPattern())
                .set(RULE_PRIORITY, rule.getPriority())
                .set(RULE_COUNTERPARTY_ID, rule.getCounterparty().getId().toString())
                .set(RULE_AMOUNT_TYPE, rule.getAmountType().name())
                .execute();
        });
    }

    @Override
    public void writeCategory(Category category) {
        boolean exists = dsl.fetchExists(
            dsl.selectFrom(CATEGORY).where(CATEGORY_ID.eq(category.getId().toString()))
        );
        if (exists) {
            throw new IllegalArgumentException("Category with ID " + category.getId() + " already exists");
        }
        dsl.insertInto(CATEGORY)
            .set(CATEGORY_ID, category.getId().toString())
            .set(CATEGORY_NAME, category.getName())
            .set(CATEGORY_SUB_CATEGORY, category.getSubCategory())
            .execute();
    }

    private void ensureCategoryExists(DSLContext tx, Category category) {
        tx.insertInto(CATEGORY)
            .set(CATEGORY_ID, category.getId().toString())
            .set(CATEGORY_NAME, category.getName())
            .set(CATEGORY_SUB_CATEGORY, category.getSubCategory())
            .onDuplicateKeyIgnore()
            .execute();
    }

    private void ensureCounterpartyExists(DSLContext tx, Counterparty counterparty) {
        tx.insertInto(COUNTERPARTY)
            .set(COUNTERPARTY_ID, counterparty.getId().toString())
            .set(COUNTERPARTY_NAME, counterparty.getName())
            .set(COUNTERPARTY_DEFAULT_CATEGORY_ID, counterparty.getDefaultCategory().getId().toString())
            .onDuplicateKeyIgnore()
            .execute();
    }

    private ClassificationRule toDomain(Record record) {
        UUID catId = UUID.fromString(record.get(CATEGORY_ID));
        String catName = record.get(CATEGORY_NAME);
        String subCategory = record.get(CATEGORY_SUB_CATEGORY);
        Category category = new Category(catId, catName, subCategory);

        UUID cpId = UUID.fromString(record.get(COUNTERPARTY_ID));
        String cpName = record.get(COUNTERPARTY_NAME);
        Counterparty counterparty = new Counterparty(cpId, cpName, category);

        UUID ruleId = UUID.fromString(record.get(RULE_ID));
        String memoPattern = record.get(RULE_MEMO_PATTERN);
        int priority = record.get(RULE_PRIORITY);
        ClassificationRule.AmountType amountType =
            ClassificationRule.AmountType.valueOf(record.get(RULE_AMOUNT_TYPE));

        return new ClassificationRule(ruleId, memoPattern, priority, counterparty, amountType);
    }
}
