package ai.functionals.api.neura.model.enums;

import jakarta.persistence.AttributeConverter;

public enum UserWaitlistStatus {
    PENDING, APPROVED, REJECTED;

    public static class Converter implements AttributeConverter<UserWaitlistStatus, String> {
        @Override
        public String convertToDatabaseColumn(UserWaitlistStatus attribute) {
            if (attribute == null) {
                return null;
            }
            return attribute.name();
        }

        @Override
        public UserWaitlistStatus convertToEntityAttribute(String dbData) {
            if (dbData == null || dbData.trim().isEmpty()) {
                return null;
            }
            return UserWaitlistStatus.valueOf(dbData);
        }
    }
}
