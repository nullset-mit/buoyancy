package net.mossworks.buoyancy.infrastructure.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import net.mossworks.buoyancy.application.repository.ClassificationRuleRepository;
import net.mossworks.buoyancy.domain.Category;
import net.mossworks.buoyancy.domain.ClassificationRule;
import net.mossworks.buoyancy.domain.Counterparty;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class YamlFileClassificationRuleRepository implements ClassificationRuleRepository {

    private final Path filePath;
    private final ObjectMapper mapper;

    public YamlFileClassificationRuleRepository(Path filePath) {
        if (filePath == null) {
            throw new IllegalArgumentException("File path cannot be null");
        }
        this.filePath = filePath;
        this.mapper = new ObjectMapper(new YAMLFactory());
    }

    @Override
    public List<ClassificationRule> loadRules() {
        if (!filePath.toFile().exists()) {
            return new ArrayList<>();
        }
        try {
            JsonNode root = mapper.readTree(filePath.toFile());
            JsonNode rulesNode = root.get("rules");
            if (rulesNode == null || !rulesNode.isArray()) {
                return new ArrayList<>();
            }
            List<ClassificationRule> rules = new ArrayList<>();
            for (int i = 0; i < rulesNode.size(); i++) {
                JsonNode ruleNode = rulesNode.get(i);
                validateRuleNode(ruleNode, i);
                rules.add(toDomain(ruleNode));
            }
            return rules;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load rules from " + filePath, e);
        }
    }

    @Override
    public void writeRule(ClassificationRule rule) {
        List<ClassificationRule> existing = loadRules();
        for (ClassificationRule r : existing) {
            if (r.getId().equals(rule.getId())) {
                throw new IllegalArgumentException(
                    "A rule with ID " + rule.getId() + " already exists");
            }
        }
        existing.add(rule);
        List<Map<String, Object>> ruleList = new ArrayList<>();
        for (ClassificationRule r : existing) {
            ruleList.add(toMap(r));
        }
        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("rules", ruleList);
        try {
            mapper.writeValue(filePath.toFile(), wrapper);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write rule to " + filePath, e);
        }
    }

    private void validateRuleNode(JsonNode node, int index) {
        String ctx = "Rule at index " + index;
        requireTextField(node, "id", ctx);
        requireTextField(node, "memoPattern", ctx);
        requireNumberField(node, "priority", ctx);
        requireTextField(node, "amountType", ctx);
        requireObjectField(node, "counterparty", ctx);

        JsonNode cpNode = node.get("counterparty");
        String cpCtx = ctx + " > counterparty";
        requireTextField(cpNode, "id", cpCtx);
        requireTextField(cpNode, "name", cpCtx);
        requireObjectField(cpNode, "defaultCategory", cpCtx);

        JsonNode catNode = cpNode.get("defaultCategory");
        String catCtx = cpCtx + " > defaultCategory";
        requireTextField(catNode, "id", catCtx);
        requireTextField(catNode, "name", catCtx);
        // subCategory is optional — no validation required
    }

    private void requireTextField(JsonNode parent, String field, String context) {
        if (!parent.has(field) || parent.get(field).isNull()) {
            throw new IllegalArgumentException(context + ": missing required field '" + field + "'");
        }
        if (!parent.get(field).isTextual()) {
            throw new IllegalArgumentException(context + ": field '" + field + "' must be text");
        }
    }

    private void requireNumberField(JsonNode parent, String field, String context) {
        if (!parent.has(field) || parent.get(field).isNull()) {
            throw new IllegalArgumentException(context + ": missing required field '" + field + "'");
        }
        if (!parent.get(field).isNumber()) {
            throw new IllegalArgumentException(context + ": field '" + field + "' must be a number");
        }
    }

    private void requireObjectField(JsonNode parent, String field, String context) {
        if (!parent.has(field) || parent.get(field).isNull()) {
            throw new IllegalArgumentException(context + ": missing required field '" + field + "'");
        }
        if (!parent.get(field).isObject()) {
            throw new IllegalArgumentException(context + ": field '" + field + "' must be an object");
        }
    }

    private ClassificationRule toDomain(JsonNode node) {
        UUID id = UUID.fromString(node.get("id").asText());
        String memoPattern = node.get("memoPattern").asText();
        int priority = node.get("priority").asInt();
        ClassificationRule.AmountType amountType =
            ClassificationRule.AmountType.valueOf(node.get("amountType").asText());

        JsonNode cpNode = node.get("counterparty");
        UUID cpId = UUID.fromString(cpNode.get("id").asText());
        String cpName = cpNode.get("name").asText();

        JsonNode catNode = cpNode.get("defaultCategory");
        UUID catId = UUID.fromString(catNode.get("id").asText());
        String catName = catNode.get("name").asText();
        String subCategory = catNode.has("subCategory") && !catNode.get("subCategory").isNull()
            ? catNode.get("subCategory").asText()
            : null;

        Category category = new Category(catId, catName, subCategory);
        Counterparty counterparty = new Counterparty(cpId, cpName, category);
        return new ClassificationRule(id, memoPattern, priority, counterparty, amountType);
    }

    private Map<String, Object> toMap(ClassificationRule rule) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", rule.getId().toString());
        map.put("memoPattern", rule.getMemoPattern());
        map.put("priority", rule.getPriority());
        map.put("amountType", rule.getAmountType().name());

        Map<String, Object> cpMap = new LinkedHashMap<>();
        cpMap.put("id", rule.getCounterparty().getId().toString());
        cpMap.put("name", rule.getCounterparty().getName());

        Map<String, Object> catMap = new LinkedHashMap<>();
        catMap.put("id", rule.getCounterparty().getDefaultCategory().getId().toString());
        catMap.put("name", rule.getCounterparty().getDefaultCategory().getName());
        catMap.put("subCategory", rule.getCounterparty().getDefaultCategory().getSubCategory());

        cpMap.put("defaultCategory", catMap);
        map.put("counterparty", cpMap);
        return map;
    }
}
