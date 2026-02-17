package com.thanos.portfolio.validator;

import com.thanos.portfolio.model.Category;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;
import java.util.Map;

public class TargetStateSumValidator implements ConstraintValidator<ValidTargetStateSum, Map<Category, BigDecimal>> {

    @Override
    public boolean isValid(Map<Category, BigDecimal> categoryBigDecimalMap,
                           ConstraintValidatorContext context) {

        if (categoryBigDecimalMap == null || categoryBigDecimalMap.isEmpty()) return false;

        BigDecimal sum = categoryBigDecimalMap.values().stream()
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return sum.subtract(BigDecimal.valueOf(100))
                .abs()
                .compareTo(new BigDecimal("0.01")) <= 0;
    }
}
