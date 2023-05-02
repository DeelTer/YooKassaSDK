package ru.deelter.yookassa.api.data;

import com.google.gson.*;
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
