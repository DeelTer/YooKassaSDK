package ru.deelter.yookassa.data.impl;

import com.google.gson.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.math.BigDecimal;

/*
  "amount": {
    "value": "100.00",
    "currency": "RUB"
  },
 */
public class Amount implements JsonSerializer<Amount>, JsonDeserializer<Amount> {

	private final BigDecimal value;
	private final Currency currency;

	public Amount(@NotNull BigDecimal value, @NotNull Currency currency) {
		this.value = value;
		this.currency = currency;
	}

	@Contract("_, _ -> new")
	public static @NotNull Amount from(double value, @NotNull Currency currency) {
		return new Amount(BigDecimal.valueOf(value), currency);
	}

	@Contract("_, _ -> new")
	public static @NotNull Amount from(long value, @NotNull Currency currency) {
		return new Amount(BigDecimal.valueOf(value), currency);
	}

	public BigDecimal getValue() {
		return value;
	}

	public Currency getCurrency() {
		return currency;
	}

	@Override
	public Amount deserialize(@NotNull JsonElement json, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
		JsonObject data = json.getAsJsonObject();
		return new Amount(
				data.get("value").getAsBigDecimal(),
				Currency.getByCode(data.get("currency").getAsString())
		);
	}

	@Override
	public JsonElement serialize(@NotNull Amount amount, Type type, JsonSerializationContext jsonSerializationContext) {
		JsonObject data = new JsonObject();
		data.addProperty("value", amount.getValue());
		data.addProperty("currency", currency.name());
		return data;
	}

	@Override
	public String toString() {
		return "Amount{" +
				"value=" + value +
				", currency=" + currency +
				'}';
	}
}
