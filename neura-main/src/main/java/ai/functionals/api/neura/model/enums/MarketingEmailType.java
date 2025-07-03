package ai.functionals.api.neura.model.enums;

import jakarta.persistence.AttributeConverter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static ai.functionals.api.neura.util.AppUtil.DELIMITER;

public enum MarketingEmailType {
    NONE,
    DEFAULT,
    MARKETING,
    PRODUCT_UPDATES,
    NEWSLETTER,
    PROMOTIONS,
    SURVEYS,
    CASE_STUDIES,
    WEBINARS,
    EVENTS,
    ALL;

    public static class Converter implements AttributeConverter<List<MarketingEmailType>, String> {
        @Override
        public String convertToDatabaseColumn(List<MarketingEmailType> attribute) {
            if (attribute == null) {
                return null;
            }
            return attribute.stream()
                    .map(Enum::name)
                    .collect(Collectors.joining(DELIMITER));
        }

        @Override
        public List<MarketingEmailType> convertToEntityAttribute(String dbData) {
            if (dbData == null || dbData.trim().isEmpty()) {
                return null;
            }
            return Arrays.stream(dbData.split(DELIMITER))
                    .map(MarketingEmailType::valueOf)
                    .collect(Collectors.toList());
        }
    }
}
