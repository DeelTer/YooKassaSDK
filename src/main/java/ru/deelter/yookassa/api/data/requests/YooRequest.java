package ru.deelter.yookassa.api.data.requests;

import org.jetbrains.annotations.NotNull;

public class YooRequest {

	private final String url;
	private final RequestType type;

	protected YooRequest(@NotNull String url, @NotNull RequestType type) {
		this.url = url;
		this.type = type;
	}

	public String getUrl() {
		return url;
	}

	public RequestType getType() {
		return type;
	}

	public enum RequestMethod {

		GET,
		POST,
		DELETE
	}
}
