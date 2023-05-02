package ru.deelter.yookassa.api.data;

import org.jetbrains.annotations.NotNull;

public enum Currency {

	BYN("byn"),
	EUR("€"),
	KZT("₸"),
	RUB("₽"),
	UAH("₴"),
	USD("$"),
	UNKNOWN("?");

	private final String symbol;

	Currency(@NotNull String symbol) {
		this.symbol = symbol;
	}

	@NotNull
	public static Currency getByCode(@NotNull String symbol) {
		for (Currency currency : values()) {
			if (currency.name().equalsIgnoreCase(symbol))
				return currency;
		}
		return UNKNOWN;
	}

	@NotNull
	public String getSymbol() {
		return symbol;
	}

}