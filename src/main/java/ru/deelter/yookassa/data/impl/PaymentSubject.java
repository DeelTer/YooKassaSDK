package ru.deelter.yookassa.data.impl;

public enum PaymentSubject {
    COMMODITY("Товар"),
    EXCISE("Подакцизный товар"),
    JOB("Работа"),
    SERVICE("Услуга"),
    PAYMENT("Платеж"),
    CASINO("Платеж казино"),
    GAMBLING_BET("Ставка в азартной игре"),
    GAMBLING_PRIZE("Выигрыш азартной игры"),
    LOTTERY("Лотерейный билет"),
    LOTTERY_PRIZE("Выигрыш в лотерею"),
    INTELLECTUAL_ACTIVITY("Результаты интеллектуальной деятельности"),
    AGENT_COMMISSION("Агентское вознаграждение"),
    PROPERTY_RIGHT("Имущественное право"),
    NON_OPERATING_GAIN("Внереализационный доход"),
    INSURANCE_PREMIUM("Страховой сбор"),
    SALES_TAX("Торговый сбор"),
    RESORT_FEE("Курортный сбор"),
    COMPOSITE("Несколько вариантов"),
    ANOTHER("Другое"),
    MARKED("Товар с маркировкой (ТМ)"),
    NON_MARKED("Товар без маркировки (ТНМ)"),
    MARKED_EXCISE("Подакцизный товар с маркировкой (АТМ)"),
    NON_MARKED_EXCISE("Подакцизный товар без маркировки (АТНМ)"),
    FINE("Выплата"),
    TAX("Страховые взносы"),
    LIEN("Залог"),
    COST("Расход"),
    AGENT_WITHDRAWALS("Выдача денежных средств"),
    PENSION_INSURANCE_WITHOUT_PAYOUTS("Взносы на обязательное пенсионное страхование ИП"),
    PENSION_INSURANCE_WITH_PAYOUTS("Взносы на обязательное пенсионное страхование"),
    HEALTH_INSURANCE_WITHOUT_PAYOUTS("Взносы на обязательное медицинское страхование ИП"),
    HEALTH_INSURANCE_WITH_PAYOUTS("Взносы на обязательное медицинское страхование"),
    HEALTH_INSURANCE("Взносы на обязательное социальное страхование");

    private final String description;

    PaymentSubject(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}