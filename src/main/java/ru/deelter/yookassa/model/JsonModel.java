package ru.deelter.yookassa.model;

import com.google.gson.*;
import ru.deelter.yookassa.utils.JsonUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Lossless JSON model. Unknown fields and future enum values survive round trips.
 * Models are mutable builders and must not be modified concurrently with a request.
 * Constructors, setters and getters copy JSON data; modify nested objects then set them back.
 * Getters of nested models build a new object on every call, so store the result in a local variable.
 */
public class JsonModel {
	private static final ClassValue<Constructor<? extends JsonModel>> CONSTRUCTORS =
		new ClassValue<Constructor<? extends JsonModel>>() {
			@Override
			protected Constructor<? extends JsonModel> computeValue(Class<?> type) {
				try {
					return type.asSubclass(JsonModel.class).getConstructor(JsonObject.class);
				} catch (NoSuchMethodException | ClassCastException e) {
					throw new JsonParseException("Cannot construct API model " + type.getName(), e);
				}
			}
		};

	private final JsonObject json;

	public JsonModel() {
		this.json = new JsonObject();
	}

	public JsonModel(JsonObject json) {
		this.json = Objects.requireNonNull(json, "json").deepCopy();
	}

	/**
	 * Returns a detached snapshot, including unrecognized API fields.
	 */
	public final JsonObject toJsonObject() {
		return json.deepCopy();
	}

	/**
	 * Compact JSON text of this model, as sent to the API.
	 */
	public String toJson() {
		return json.toString();
	}

	/**
	 * Class name only: API objects may contain personal data, so field values are never logged implicitly.
	 */
	@Override
	public String toString() {
		return getClass().getSimpleName() + "{fields=" + json.keySet() + "}";
	}

	/**
	 * Equal when both models have the same class and the same JSON content.
	 */
	@Override
	public boolean equals(Object other) {
		return this == other || other != null && other.getClass() == getClass() && json.equals(((JsonModel) other).json);
	}

	@Override
	public int hashCode() {
		return json.hashCode();
	}

	/**
	 * Reads a detached field value. An absent field returns null.
	 */
	public final JsonElement getField(String name) {
		JsonElement value = json.get(name);
		return value == null ? null : value.deepCopy();
	}

	/**
	 * Sets an extension or variant field. Null removes it; JsonNull represents explicit JSON null.
	 */
	public final JsonModel setField(String name, Object value) {
		put(name, value);
		return this;
	}

	protected final void put(String name, Object value) {
		Objects.requireNonNull(name, "name");
		if (value == null) json.remove(name);
		else json.add(name, tree(value));
	}

	private static JsonElement tree(Object value) {
		if (value instanceof JsonModel) return ((JsonModel) value).json.deepCopy();
		if (value instanceof JsonElement) return ((JsonElement) value).deepCopy();
		if (value instanceof String) return new JsonPrimitive((String) value);
		if (value instanceof Boolean) return new JsonPrimitive((Boolean) value);
		if (value instanceof Number) return new JsonPrimitive((Number) value);
		return JsonUtil.toJsonTree(value);
	}

	protected final <T> T read(String name, Type type) {
		JsonElement value = json.get(name);
		if (value == null || value.isJsonNull()) return null;
		if (type == String.class && value.isJsonPrimitive()) return cast(value.getAsString());
		// Gson returns tree elements without copying them; keep getters detached from this model.
		if (type instanceof Class && JsonElement.class.isAssignableFrom((Class<?>) type)) {
			if (!((Class<?>) type).isInstance(value))
				throw new JsonParseException("Field " + name + " has unexpected JSON type");
			return cast(value.deepCopy());
		}
		if (type instanceof Class && JsonModel.class.isAssignableFrom((Class<?>) type)) {
			if (!value.isJsonObject()) throw new JsonParseException("Field " + name + " is not a JSON object");
			return cast(construct(((Class<?>) type).asSubclass(JsonModel.class), value.getAsJsonObject()));
		}
		return JsonUtil.fromJson(value.deepCopy(), type);
	}

	/**
	 * Parses an ISO-8601 date-time field; the raw string getter remains available.
	 */
	protected final OffsetDateTime readDateTime(String name) {
		String value = read(name, String.class);
		return value == null ? null : OffsetDateTime.parse(value);
	}

	protected final void putDateTime(String name, OffsetDateTime value) {
		put(name, value == null ? null : DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(value));
	}

	protected final LocalDate readDate(String name) {
		String value = read(name, String.class);
		return value == null ? null : LocalDate.parse(value);
	}

	protected final void putDate(String name, LocalDate value) {
		put(name, value == null ? null : value.toString());
	}

	/**
	 * Parses a decimal string such as an amount value.
	 */
	protected final BigDecimal readDecimal(String name) {
		String value = read(name, String.class);
		return value == null ? null : new BigDecimal(value);
	}

	protected final void putDecimal(String name, BigDecimal value) {
		put(name, value == null ? null : value.toPlainString());
	}

	@SuppressWarnings("unchecked")
	private static <T> T cast(Object value) {
		return (T) value;
	}

	/**
	 * Views a polymorphic object as its documented variant, preserving all fields.
	 */
	public final <T extends JsonModel> T as(Class<T> type) {
		return construct(type, json);
	}

	static <T extends JsonModel> T construct(Class<T> type, JsonObject json) {
		try {
			return type.cast(CONSTRUCTORS.get(type).newInstance(json));
		} catch (InvocationTargetException e) {
			throw new JsonParseException("Cannot construct API model " + type.getName(), e.getCause());
		} catch (ReflectiveOperationException e) {
			throw new JsonParseException("Cannot construct API model " + type.getName(), e);
		}
	}

	/**
	 * Adapter shared by all generated subclasses; never serializes the internal wrapper field.
	 */
	public static final class Adapter implements JsonSerializer<JsonModel>, JsonDeserializer<JsonModel> {
		@Override
		public JsonElement serialize(JsonModel value, Type type, JsonSerializationContext context) {
			return value.toJsonObject();
		}

		@Override
		public JsonModel deserialize(JsonElement value, Type type, JsonDeserializationContext context) {
			if (!value.isJsonObject()) throw new JsonParseException("Expected a JSON object");
			if (!(type instanceof Class)) throw new JsonParseException("Cannot construct API model " + type);
			return construct(((Class<?>) type).asSubclass(JsonModel.class), value.getAsJsonObject());
		}
	}
}
