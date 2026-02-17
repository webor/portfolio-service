package com.thanos.portfolio.converter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;

public abstract class JsonConverter<T> implements AttributeConverter<T, String> {
    protected static final ObjectMapper OM = new ObjectMapper();

    protected abstract Class<T> clazz();

    @Override
    public String convertToDatabaseColumn(T attribute) {
        try {
            return attribute == null ? null : OM.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize JSON", e);
        }
    }

    @Override
    public T convertToEntityAttribute(String dbData) {
        try {
            return dbData == null ? null : OM.readValue(dbData, clazz());
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize JSON", e);
        }
    }
}