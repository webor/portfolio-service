package com.thanos.portfolio.converter;
import com.thanos.portfolio.model.UserDetails;
import jakarta.persistence.Converter;

@Converter
public class UserDetailsConverter extends JsonConverter<UserDetails> {
    @Override protected Class<UserDetails> clazz() { return UserDetails.class; }
}
