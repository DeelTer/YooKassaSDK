package ru.deelter.yookassa.api.data;

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
