package ru.deelter.yookassa.requests.payments;

import org.jetbrains.annotations.NotNull;
import ru.deelter.yookassa.data.impl.requests.YooRequest;
import ru.deelter.yookassa.utils.YooRequestUrls;

import java.util.UUID;

public class PaymentGetRequest extends YooRequest {

	public PaymentGetRequest(@NotNull UUID paymentId) {
		super(String.format(YooRequestUrls.PAYMENT_GET, paymentId), RequestMethod.GET);
	}
}
