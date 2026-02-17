package com.thanos.portfolio.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.thanos.portfolio.model.Category;
import com.thanos.portfolio.model.StockPosition;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;
import java.util.Map;

@Converter
public class PortfolioDetailsConverter
        implements AttributeConverter<Map<Category, List<StockPosition>>, String> {

    private static final TypeReference<Map<Category, List<StockPosition>>> TYPE =
            new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(Map<Category, List<StockPosition>> attribute) {
        try {
            return attribute == null ? null : JsonConverter.OM.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize portfolio JSON", e);
        }
    }

    @Override
    public Map<Category, List<StockPosition>> convertToEntityAttribute(String dbData) {
        try {
            return dbData == null ? null : JsonConverter.OM.readValue(dbData, TYPE);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize portfolio JSON", e);
        }
    }
}