package ru.deelter.yookassa.exceptions;

public class ReceiptItemLimitException extends RuntimeException {

	public ReceiptItemLimitException() {
		super("Receipt can contain maximum 6 items");
	}
}
