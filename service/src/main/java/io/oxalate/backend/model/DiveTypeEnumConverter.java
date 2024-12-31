package io.oxalate.backend.model;

import io.oxalate.backend.api.DiveTypeEnum;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class DiveTypeEnumConverter implements AttributeConverter<DiveTypeEnum, String> {

    @Override
    public String convertToDatabaseColumn(DiveTypeEnum attribute) {
        return attribute != null ? attribute.typeName : null;
    }

    @Override
    public DiveTypeEnum convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        for (DiveTypeEnum diveType : DiveTypeEnum.values()) {
            if (diveType.typeName.equals(dbData)) {
                return diveType;
            }
        }
        throw new IllegalArgumentException("Unknown database value: " + dbData);
    }
}
