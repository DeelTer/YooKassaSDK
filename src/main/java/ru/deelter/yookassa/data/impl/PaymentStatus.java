package ru.deelter.yookassa.data.impl;

public enum PaymentStatus {

	PENDING,
	SUCCEEDED,
	WAITING_FOR_CAPTURE,
	CANCELED;

	public boolean isSuccess() {
		return this == SUCCEEDED;
	}

	@Override
	public String toString() {
		return super.toString().toLowerCase();
	}
}
