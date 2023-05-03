package ru.deelter.yookassa.api.data.requests;

import com.google.gson.*;
import org.jetbrains.annotations.NotNull;
import ru.deelter.yookassa.api.events.YooKassaEvent;

import java.lang.reflect.Type;

public class WebhookCreateData implements IYooRequestData, JsonSerializer<WebhookCreateData>, JsonDeserializer<WebhookCreateData> {

	private final YooKassaEvent event;
	private final String url;

	public WebhookCreateData(YooKassaEvent event, String url) {
		this.event = event;
		this.url = url;
	}

	public YooKassaEvent getEvent() {
		return event;
	}

	public String getUrl() {
		return url;
	}

	@Override
	public WebhookCreateData deserialize(@NotNull JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
		JsonObject data = jsonElement.getAsJsonObject();
		return new WebhookCreateData(
				YooKassaEvent.getByName(data.get("event").getAsString()),
				data.get("url").getAsString()
		);
	}

	@Override
	public JsonElement serialize(WebhookCreateData webhookCreateData, Type type, JsonSerializationContext jsonSerializationContext) {
		JsonObject data = new JsonObject();
		data.addProperty("event", event.getName());
		data.addProperty("url", url);
		return data;
	}
}