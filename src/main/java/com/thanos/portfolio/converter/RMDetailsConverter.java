package com.thanos.portfolio.converter;

import com.thanos.portfolio.model.RMDetails;
import jakarta.persistence.Converter;

@Converter
public class RMDetailsConverter extends JsonConverter<RMDetails> {
    @Override protected Class<RMDetails> clazz() { return RMDetails.class; }
}