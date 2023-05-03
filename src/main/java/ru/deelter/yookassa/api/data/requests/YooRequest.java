package ru.deelter.yookassa.api.data.requests;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class YooRequest {

	private final String url;
	private final RequestMethod method;
	private final IYooRequestData data;

	protected YooRequest(@NotNull String url, @NotNull RequestMethod method, @Nullable IYooRequestData data) {
		this.url = url;
		this.method = method;
		this.data = data;
	}

	protected YooRequest(@NotNull String url, @NotNull RequestMethod method) {
		this(url, method, null);
	}

	@NotNull
	public String getUrl() {
		return url + getUrlParameters();
	}

	@NotNull
	public RequestMethod getMethod() {
		return method;
	}

	@Nullable
	public IYooRequestData getData() {
		return data;
	}

	public String getUrlParameters() {
		return "";
	}

	public enum RequestMethod {

		GET,
		POST,
		DELETE
	}
}
