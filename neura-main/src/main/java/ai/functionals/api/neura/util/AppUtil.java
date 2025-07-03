package ai.functionals.api.neura.util;

import ai.functionals.api.neura.model.enums.UserRoles;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static functionals.designer.v1.SoftwareDesigner.ResponseType.*;

@Slf4j
public class AppUtil {
    private AppUtil() {
    }

    public static final String DELIMITER = ",";
    public static final String LLM_RESPONSE_TOPIC = "llm-response";

    public static final String SNAPSHOT_PREFIX = "snapshot:";
    public static final Long DEFAULT_CREDITS = 5000L;

    private static final ObjectMapper mapper = new ObjectMapper()
            .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
            .configure(JsonParser.Feature.ALLOW_COMMENTS, true)
            .configure(JsonParser.Feature.ALLOW_TRAILING_COMMA, true)
            .configure(JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS, true)
            .configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true)
            .configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true)
            .configure(JsonParser.Feature.ALLOW_MISSING_VALUES, true)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final Map<String, String> keysToParse = Map.of(
            DOCUMENT.name(), "design_ready_prd",
            QUESTIONS.name(), "clarification_questions",
            FEEDBACK.name(), "design_ready_prd"
    );

    public static String genSlug() {
        return UUID.randomUUID().toString().replaceAll("-", "" );
    }

    public static String getS3ObjectKey(String tenantId, String sessionId, Integer version) {
        return version == null ?
                String.format("%s/%s", tenantId, sessionId) :
                String.format("%s/%s/%s.json", tenantId, sessionId, version);
    }

    public static Set<UserRoles> rolesFromString(String roles) {
        if (StringUtils.isBlank(roles)) {
            return Set.of();
        }
        return Stream.of(roles.split(DELIMITER))
                .map(String::trim)
                .map(UserRoles::valueOf)
                .collect(Collectors.toSet());
    }


    public static String getSchemaFromResources(String version, boolean llmIgnore) {
        if (StringUtils.isBlank(version) || StringUtils.equalsIgnoreCase(version, "latest")) version = "";
        String resourcePath = "schema/software-designer" + version + ".json";
        try (InputStream is = new ClassPathResource(resourcePath).getInputStream()) {
            JsonNode root = mapper.readTree(is);
            if (llmIgnore) removeLlmIgnoreFields(root);
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read or process schema at " + resourcePath, e);
        }
    }

    public static String getClarificationSchemaFromResources(String version) {
        try {
            if (StringUtils.isBlank(version) || StringUtils.equalsIgnoreCase(version, "latest")) version = "";
            return new String(new ClassPathResource("schema/clarification" + version + ".json" ).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read schema", e);
        }
    }

    private static void removeLlmIgnoreFields(JsonNode node) {
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            obj.remove("llmIgnore" );        // remove any top-level "llmIgnore" flags
            JsonNode props = obj.get("properties" );      // scrub if this object has a "properties" block
            if (props instanceof ObjectNode properties) {
                List<String> toRemove = new ArrayList<>();
                properties.fieldNames().forEachRemaining(fieldName -> {
                    JsonNode propDef = properties.get(fieldName);
                    if (propDef.path("llmIgnore" ).asBoolean(false)) {
                        toRemove.add(fieldName);
                    }
                });
                toRemove.forEach(properties::remove);
            }
            obj.fields().forEachRemaining(entry -> removeLlmIgnoreFields(entry.getValue()));
        } else if (node.isArray()) {
            for (JsonNode elt : node) {
                removeLlmIgnoreFields(elt);
            }
        }
    }

    public static String parseResponseAndExtractValue(String jsonResponse, String responseType) {
        try {
            JsonNode rootNode = mapper.readTree(jsonResponse);
            JsonNode valuesNode = rootNode.get("values" );
            if (keysToParse.containsKey(responseType)) {
                String key = keysToParse.get(responseType);
                if (key.isEmpty()) {
                    // For DOCUMENT type, store initial_prd
                    return valuesNode.get("initial_prd" ).asText();
                } else {
                    if (valuesNode.get(key) != null) {
                        // For other types, store the value specified by key
                        return valuesNode.get(key).toString();
                    }
                }
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to parse response JSON", e);
            throw new RuntimeException("Failed to parse response JSON", e);
        }
    }
}
