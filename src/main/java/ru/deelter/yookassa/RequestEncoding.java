package ru.deelter.yookassa;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import okhttp3.HttpUrl;
import ru.deelter.yookassa.model.JsonModel;

import java.util.Map;
import java.util.Objects;

/**
 * URL and header encoding for API operations.
 */
final class RequestEncoding {
	private RequestEncoding() {
	}

	/**
	 * Validates a caller-supplied object ID used as one path segment; OkHttp encodes it.
	 */
	static String id(String value) {
		Objects.requireNonNull(value, "id");
		if (value.trim().isEmpty() || value.equals(".") || value.equals(".."))
			throw new IllegalArgumentException("ID must be a nonempty path segment");
		return value;
	}

	static HttpUrl url(HttpUrl base, String[] segments, JsonModel query) {
		HttpUrl.Builder url = base.newBuilder();
		for (String segment : segments) url.addPathSegment(segment);
		if (query != null) {
			for (Map.Entry<String, JsonElement> entry : query.toJsonObject().entrySet()) {
				JsonElement value = entry.getValue();
				if (value.isJsonNull()) continue;
				if (value.isJsonObject()) {
					JsonObject object = value.getAsJsonObject();
					for (Map.Entry<String, JsonElement> nested : object.entrySet()) {
						url.addQueryParameter(entry.getKey() + "[" + nested.getKey() + "]", scalar(nested.getValue()));
					}
				} else {
					url.addQueryParameter(entry.getKey(), scalar(value));
				}
			}
		}
		return url.build();
	}

	private static String scalar(JsonElement value) {
		if (!value.isJsonPrimitive()) throw new IllegalArgumentException("Query values must be scalar");
		return value.getAsString();
	}

	static String key(String key) {
		Objects.requireNonNull(key, "idempotenceKey");
		if (key.isEmpty() || key.length() > 64)
			throw new IllegalArgumentException("Idempotence key must contain 1 to 64 visible ASCII characters");
		for (int i = 0; i < key.length(); i++) {
			if (key.charAt(i) < 33 || key.charAt(i) > 126)
				throw new IllegalArgumentException("Idempotence key must contain visible ASCII characters");
		}
		return key;
	}
}
