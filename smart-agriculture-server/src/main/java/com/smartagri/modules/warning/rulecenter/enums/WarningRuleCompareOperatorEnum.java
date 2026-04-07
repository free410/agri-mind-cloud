package com.smartagri.modules.warning.rulecenter.enums;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.Arrays;

@Getter
public enum WarningRuleCompareOperatorEnum {

    GT(">", "大于"),
    LT("<", "小于"),
    GTE(">=", "大于等于"),
    LTE("<=", "小于等于"),
    EQ("==", "等于");

    private final String code;
    private final String description;

    WarningRuleCompareOperatorEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static boolean contains(String code) {
        return Arrays.stream(values()).anyMatch(item -> item.code.equals(code));
    }

    public static boolean evaluate(String operator, BigDecimal actual, BigDecimal threshold) {
        if (actual == null || threshold == null || !contains(operator)) {
            return false;
        }
        return switch (operator) {
            case ">" -> actual.compareTo(threshold) > 0;
            case "<" -> actual.compareTo(threshold) < 0;
            case ">=" -> actual.compareTo(threshold) >= 0;
            case "<=" -> actual.compareTo(threshold) <= 0;
            case "==" -> actual.compareTo(threshold) == 0;
            default -> false;
        };
    }
}

