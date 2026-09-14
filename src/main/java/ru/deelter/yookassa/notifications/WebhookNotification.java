package ru.deelter.yookassa.notifications;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import ru.deelter.yookassa.model.JsonModel;
import ru.deelter.yookassa.model.Payment;
import ru.deelter.yookassa.model.Refund;

import java.util.Objects;

/**
 * Incoming HTTP notification body: {@code {"type":"notification","event":"payment.succeeded","object":{...}}}.
 *
 * <p>A notification is a hint, not proof. Check the sender address with {@link YooKassaNetworks},
 * then load the object through the API (for example {@code api.getPayment(id)}) and act on that state.
 */
public final class WebhookNotification {

	private final String type;
	private final String event;
	private final JsonModel object;

	private WebhookNotification(String type, String event, JsonModel object) {
		this.type = type;
		this.event = event;
		this.object = object;
	}

	/**
	 * Parses a notification body; throws {@link JsonParseException} for malformed input.
	 */
	public static WebhookNotification parse(String body) {
		Objects.requireNonNull(body, "body");
		JsonElement root;
		try {
			root = JsonParser.parseString(body);
		} catch (RuntimeException e) {
			throw new JsonParseException("Notification body is not JSON", e);
		}
		if (!root.isJsonObject()) throw new JsonParseException("Notification body must be a JSON object");
		JsonObject json = root.getAsJsonObject();
		String event = string(json, "event");
		JsonElement object = json.get("object");
		if (event == null || object == null || !object.isJsonObject())
			throw new JsonParseException("Notification must contain event and object");
		return new WebhookNotification(string(json, "type"), event, new JsonModel(object.getAsJsonObject()));
	}

	private static String string(JsonObject json, String name) {
		JsonElement value = json.get(name);
		return value != null && value.isJsonPrimitive() ? value.getAsString() : null;
	}

	public String getType() {
		return type;
	}

	/**
	 * Event name, for example {@code payment.succeeded} or {@code refund.succeeded}.
	 */
	public String getEvent() {
		return event;
	}

	/**
	 * Object type prefix of the event: {@code payment}, {@code refund}, {@code payout}, {@code deal}.
	 */
	public String getObjectType() {
		int dot = event.indexOf('.');
		return dot < 0 ? event : event.substring(0, dot);
	}

	public String getObjectId() {
		JsonElement id = object.getField("id");
		return id != null && id.isJsonPrimitive() ? id.getAsString() : null;
	}

	/**
	 * The raw event object, including fields unknown to this SDK version.
	 */
	public JsonModel getObject() {
		return object.as(JsonModel.class);
	}

	public <T extends JsonModel> T getObject(Class<T> type) {
		return object.as(type);
	}

	public boolean isPaymentEvent() {
		return "payment".equals(getObjectType());
	}

	public boolean isRefundEvent() {
		return "refund".equals(getObjectType());
	}

	/**
	 * Typed payment view; call only when {@link #isPaymentEvent()} is true.
	 */
	public Payment getPayment() {
		return object.as(Payment.class);
	}

	/**
	 * Typed refund view; call only when {@link #isRefundEvent()} is true.
	 */
	public Refund getRefund() {
		return object.as(Refund.class);
	}
}
