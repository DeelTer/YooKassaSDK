package ru.deelter.yookassa.api.data;

import java.util.UUID;

public class Webhook implements IYooObject {

	private final UUID id;
	private final String event;
	private final String url;

	public Webhook(UUID id, String event, String url) {
		this.id = id;
		this.event = event;
		this.url = url;
	}

	public UUID getId() {
		return id;
	}

	public String getEvent() {
		return event;
	}

	public String getUrl() {
		return url;
	}
}
