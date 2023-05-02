package ru.deelter.yookassa.api.data.requests;

import com.google.gson.*;
import org.jetbrains.annotations.NotNull;
import ru.deelter.yookassa.api.events.YooKassaEvent;

import java.lang.reflect.Type;

public class WebhookRequest implements IYooRequestData, JsonSerializer<WebhookRequest>, JsonDeserializer<WebhookRequest> {

	private final YooKassaEvent event;
	private final String url;

	public WebhookRequest(YooKassaEvent event, String url) {
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
	public WebhookRequest deserialize(@NotNull JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
		JsonObject data = jsonElement.getAsJsonObject();
		return new WebhookRequest(
				YooKassaEvent.getByName(data.get("event").getAsString()),
				data.get("url").getAsString()
		);
	}

	@Override
	public JsonElement serialize(WebhookRequest webhookRequest, Type type, JsonSerializationContext jsonSerializationContext) {
		JsonObject data = new JsonObject();
		data.addProperty("event", event.getName());
		data.addProperty("url", url);
		return data;
	}
}