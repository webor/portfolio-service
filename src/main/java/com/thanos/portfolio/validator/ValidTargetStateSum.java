package com.thanos.portfolio.validator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = TargetStateSumValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidTargetStateSum {

    String message() default "targetState must sum to 100";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
