package com.thanos.portfolio.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.thanos.portfolio.model.Category;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.math.BigDecimal;
import java.util.Map;

@Converter
public class TargetStateConverter implements AttributeConverter<Map<Category, BigDecimal>, String> {

    private static final TypeReference<Map<Category, BigDecimal>> TYPE = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(Map<Category, BigDecimal> attribute) {
        try {
            return attribute == null ? null : JsonConverter.OM.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize target_state JSON", e);
        }
    }

    @Override
    public Map<Category, BigDecimal> convertToEntityAttribute(String dbData) {
        try {
            return dbData == null ? null : JsonConverter.OM.readValue(dbData, TYPE);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize target_state JSON", e);
        }
    }
}