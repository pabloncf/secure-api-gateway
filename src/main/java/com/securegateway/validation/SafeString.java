package com.securegateway.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SafeStringValidator.class)
@Documented
public @interface SafeString {
    String message() default "Input contains potentially unsafe content";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
