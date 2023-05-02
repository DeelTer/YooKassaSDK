package ru.deelter.yookassa;

import ru.deelter.yookassa.api.YooKassa;
import ru.deelter.yookassa.api.data.Amount;
import ru.deelter.yookassa.api.data.Currency;
import ru.deelter.yookassa.api.data.Payment;
import ru.deelter.yookassa.api.data.requests.PaymentRequest;

import java.math.BigDecimal;

public class Main {

	private static final YooKassa YOO_KASSA = YooKassa.create(
			951045,
			"live_juOwTWL0lMLlRaTsQZrweUIP3CsNq3bSdTd1YJPtVSo"
	);

	public static void main(String[] args) throws Exception {
		Payment payment = YOO_KASSA.createPayment(
				PaymentRequest.builder()
						.amount(new Amount(BigDecimal.valueOf(1), Currency.RUB))
						.description("Test")
						.redirect("https://github.com/deelter")
						.capture(true)
						.build()
		);
		System.out.println("Payment url:" + payment.getConfirmation().getUrl());

		while (true) {
			Payment payment2 = YOO_KASSA.getPayment(payment.getId());
			System.out.printf("\nStatus: %s", payment2.getStatus());
			Thread.sleep(3000L);
		}
	}

	//20:07 start
}
