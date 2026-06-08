package com.securegateway.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class SafeStringValidator implements ConstraintValidator<SafeString, String> {

    private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>", Pattern.CASE_INSENSITIVE);
    private static final Pattern SQL_INJECTION = Pattern.compile(
            "(?i)(union\\s+select|drop\\s+table|insert\\s+into|delete\\s+from|exec\\s*\\(|xp_|'\\s*(or|and)\\s|--|;\\s*drop|;\\s*delete)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern JAVASCRIPT_PROTO = Pattern.compile("javascript\\s*:", Pattern.CASE_INSENSITIVE);
    private static final Pattern XSS_EVENT = Pattern.compile("on\\w+\\s*=", Pattern.CASE_INSENSITIVE);

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true;
        return !HTML_TAG.matcher(value).find()
                && !SQL_INJECTION.matcher(value).find()
                && !JAVASCRIPT_PROTO.matcher(value).find()
                && !XSS_EVENT.matcher(value).find();
    }
}
