package ai.functionals.api.neura.model.enums;

import jakarta.persistence.AttributeConverter;

public enum DocSessionStatus {
    STARTED,
    CLARIFICATION_GENERATED_BY_LLM,
    CLARIFICATION_SUBMITTED_BY_USER,
    INITIAL_DOC_GENERATED_BY_LLM,
    DETAILED_DOC_REQUESTED_BY_SYSTEM,
    DETAILED_DOC_GENERATED_BY_LLM,
    FEEDBACK_REQUESTED_BY_USER,
    FEEDBACK_GENERATED_BY_LLM,
    REVERTED_BY_USER;

    public static class Converter implements AttributeConverter<DocSessionStatus, String> {
        @Override
        public String convertToDatabaseColumn(DocSessionStatus attribute) {
            if (attribute == null) {
                return null;
            }
            return attribute.name();
        }

        @Override
        public DocSessionStatus convertToEntityAttribute(String dbData) {
            if (dbData == null || dbData.trim().isEmpty()) {
                return null;
            }
            return DocSessionStatus.valueOf(dbData);
        }
    }
}
