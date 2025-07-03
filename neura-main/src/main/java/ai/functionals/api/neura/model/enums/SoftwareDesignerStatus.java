package ai.functionals.api.neura.model.enums;

import jakarta.persistence.AttributeConverter;

public enum SoftwareDesignerStatus {
    TO_DO,
    IN_PROGRESS,
    DONE,
    BLOCKED,
    REVIEW,
    APPROVED,
    REJECTED;

    public static class Converter implements AttributeConverter<SoftwareDesignerStatus, String> {
        @Override
        public String convertToDatabaseColumn(SoftwareDesignerStatus attribute) {
            if (attribute == null) {
                return null;
            }
            return attribute.name();
        }

        @Override
        public SoftwareDesignerStatus convertToEntityAttribute(String dbData) {
            if (dbData == null || dbData.trim().isEmpty()) {
                return null;
            }
            return SoftwareDesignerStatus.valueOf(dbData);
        }
    }
}
