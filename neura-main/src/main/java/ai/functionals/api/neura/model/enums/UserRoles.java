package ai.functionals.api.neura.model.enums;

import jakarta.persistence.AttributeConverter;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static ai.functionals.api.neura.util.AppUtil.DELIMITER;

public enum UserRoles {
    ROLE_FUN_ANONYMOUS,
    ROLE_FUN_SUBSCRIBED,
    ROLE_FUN_USER,
    ROLE_FUN_ADMIN,
    ROLE_FUN_ROOT;

    public static class Converter implements AttributeConverter<Set<UserRoles>, String> {
        @Override
        public String convertToDatabaseColumn(Set<UserRoles> attribute) {
            if (attribute == null) {
                return null;
            }
            return attribute.stream()
                    .map(Enum::name)
                    .collect(Collectors.joining(DELIMITER));
        }

        @Override
        public Set<UserRoles> convertToEntityAttribute(String dbData) {
            if (dbData == null || dbData.trim().isEmpty()) {
                return null;
            }
            return Arrays.stream(dbData.split(DELIMITER))
                    .map(UserRoles::valueOf)
                    .collect(Collectors.toSet());
        }
    }
}