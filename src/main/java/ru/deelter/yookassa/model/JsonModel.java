package ru.deelter.yookassa.model;

import com.google.gson.*;
import ru.deelter.yookassa.exceptions.ModelValidationException;
import ru.deelter.yookassa.utils.JsonUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Lossless JSON model. Unknown fields and future enum values survive round trips.
 * Models are mutable builders and must not be modified concurrently with a request.
 * Constructors, setters and getters copy JSON data; modify nested objects then set them back.
 * Getters of nested models build a new object on every call, so store the result in a local variable.
 *
 * <p>Reading is tolerant: a field the specification calls required may be absent and its getter then
 * returns null, because an unexpected response must not make an object unreadable. Numeric fields with a
 * documented range are the exception: their getters and setters reject values outside it, so a zero
 * denominator can never reach caller arithmetic. Call {@link #validate()} to check a whole object,
 * including nested ones, against the specification.
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

	// ─── Validation against the specification ─────────────────────────────────────

	/**
	 * Problems of this object and its nested typed objects, one message per problem; empty when valid.
	 *
	 * <p>Covered: required fields, numeric ranges, string lengths and list sizes from the specification.
	 * Not covered: enum membership (new values appear without notice), patterns, polymorphic fields typed
	 * as {@link JsonModel}, and business rules only the API can check. Messages never contain values.
	 */
	public final List<String> violations() {
		List<String> out = new ArrayList<>();
		collectViolations("", out);
		return Collections.unmodifiableList(out);
	}

	/**
	 * Throws {@link ModelValidationException} listing every problem found by {@link #violations()}.
	 */
	public final void validate() {
		List<String> problems = violations();
		if (!problems.isEmpty()) throw new ModelValidationException(getClass().getSimpleName(), problems);
	}

	/**
	 * Overridden by generated models; {@code path} is empty or ends with a dot.
	 */
	protected void collectViolations(String path, List<String> out) {
	}

	/**
	 * Rejects a number outside its documented range. Null passes: absence is not a range error.
	 */
	protected static <N extends Number> N checkRange(String name, N value, Long min, Long max) {
		String problem = value == null ? null : rangeProblem(new BigDecimal(value.toString()), min, max);
		if (problem != null)
			throw new ModelValidationException(name, Collections.singletonList(name + " " + problem));
		return value;
	}

	private static String rangeProblem(BigDecimal value, Long min, Long max) {
		if (min != null && value.compareTo(BigDecimal.valueOf(min)) < 0) return "must be at least " + min;
		if (max != null && value.compareTo(BigDecimal.valueOf(max)) > 0) return "must be at most " + max;
		return null;
	}

	private JsonElement present(String name) {
		JsonElement value = json.get(name);
		return value == null || value.isJsonNull() ? null : value;
	}

	protected final void required(String path, String name, List<String> out) {
		if (present(name) == null) out.add(path + name + " is required");
	}

	protected final void range(String path, String name, Long min, Long max, List<String> out) {
		JsonElement value = present(name);
		if (value == null) return;
		if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
			out.add(path + name + " must be a number");
			return;
		}
		String problem;
		try {
			problem = rangeProblem(value.getAsBigDecimal(), min, max);
		} catch (NumberFormatException e) {
			problem = "must be a number";
		}
		if (problem != null) out.add(path + name + " " + problem);
	}

	protected final void length(String path, String name, Long min, Long max, List<String> out) {
		JsonElement value = present(name);
		if (value == null || !value.isJsonPrimitive()) return;
		String text = value.getAsString();
		// Code points, as the API counts characters; UTF-16 units would reject valid emoji-heavy text.
		int size = text.codePointCount(0, text.length());
		if (min != null && size < min) out.add(path + name + " must contain at least " + min + " character(s)");
		if (max != null && size > max) out.add(path + name + " must contain at most " + max + " character(s)");
	}

	protected final void items(String path, String name, Long min, Long max, List<String> out) {
		JsonElement value = present(name);
		if (value == null || !value.isJsonArray()) return;
		int size = value.getAsJsonArray().size();
		if (min != null && size < min) out.add(path + name + " must contain at least " + min + " item(s)");
		if (max != null && size > max) out.add(path + name + " must contain at most " + max + " item(s)");
	}

	protected final void nested(String path, String name, Class<? extends JsonModel> type, List<String> out) {
		JsonElement value = present(name);
		if (value == null) return;
		if (!value.isJsonObject()) {
			out.add(path + name + " must be an object");
			return;
		}
		view(type, value.getAsJsonObject()).collectViolations(path + name + ".", out);
	}

	protected final void nestedList(String path, String name, Class<? extends JsonModel> type, List<String> out) {
		JsonElement value = present(name);
		if (value == null) return;
		if (!value.isJsonArray()) {
			out.add(path + name + " must be a list");
			return;
		}
		JsonArray array = value.getAsJsonArray();
		for (int i = 0; i < array.size(); i++) {
			JsonElement item = array.get(i);
			String itemPath = path + name + "[" + i + "]";
			if (item.isJsonObject()) view(type, item.getAsJsonObject()).collectViolations(itemPath + ".", out);
			else out.add(itemPath + " must be an object");
		}
	}

	private static JsonModel view(Class<? extends JsonModel> type, JsonObject json) {
		return construct(type, json);
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
