package ru.deelter.yookassa.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import ru.deelter.yookassa.model.JsonModel;

import java.lang.reflect.Type;
import java.util.Objects;

/**
 * Shared thread-safe JSON codec configured for YooKassa models.
 */
public final class JsonUtil {

	private static final Gson GSON = new GsonBuilder()
		.registerTypeHierarchyAdapter(JsonModel.class, new JsonModel.Adapter())
		.disableHtmlEscaping()
		.create();

	private JsonUtil() {
	}

	/**
	 * The configured codec. Reuse it instead of creating another Gson instance.
	 */
	public static Gson gson() {
		return GSON;
	}

	public static String toJson(Object object) {
		return GSON.toJson(object);
	}

	/**
	 * Converts an object to a JSON tree without an intermediate string.
	 */
	public static JsonElement toJsonTree(Object object) {
		return GSON.toJsonTree(object);
	}

	/**
	 * Parses JSON text, for example a stored API response, into a model or another type.
	 */
	public static <T> T fromJson(String json, Class<T> type) {
		return GSON.fromJson(Objects.requireNonNull(json, "json"), type);
	}

	public static <T> T fromJson(String json, Type type) {
		return GSON.fromJson(Objects.requireNonNull(json, "json"), type);
	}

	/**
	 * Reads a JSON tree directly, avoiding serialization to a string and parsing it again.
	 */
	public static <T> T fromJson(JsonElement json, Type type) {
		return GSON.fromJson(Objects.requireNonNull(json, "json"), type);
	}
}
