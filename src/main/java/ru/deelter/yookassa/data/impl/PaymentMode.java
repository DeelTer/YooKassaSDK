package ru.deelter.yookassa.data.impl;

public enum PaymentMode {
    FULL_PREPAYMENT("Полная предоплата"),
    PARTIAL_PREPAYMENT("Частичная предоплата"),
    ADVANCE("Аванс"),
    FULL_PAYMENT("Полный расчет"),
    PARTIAL_PAYMENT("Частичный расчет и кредит"),
    CREDIT("Кредит"),
    CREDIT_PAYMENT("Выплата по кредиту");

    private final String description;

    PaymentMode(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}