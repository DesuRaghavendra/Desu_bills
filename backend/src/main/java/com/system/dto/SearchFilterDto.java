package com.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class SearchFilterDto {
    @NotBlank
    private String column;

    @NotBlank
    private String operator; // e.g., Contains, StartsWith, Equals, GreaterThan, LessThan, Between

    @NotNull
    private Object value; // String for text, Number for numeric

    // For Between operator
    private Object maxValue;

    // Getters and Setters
    public String getColumn() { return column; }
    public void setColumn(String column) { this.column = column; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public Object getValue() { return value; }
    public void setValue(Object value) { this.value = value; }
    public Object getMaxValue() { return maxValue; }
    public void setMaxValue(Object maxValue) { this.maxValue = maxValue; }
}
