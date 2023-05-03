package ru.deelter.yookassa.api.data.requests.payments;

import org.jetbrains.annotations.NotNull;
import ru.deelter.yookassa.api.data.requests.YooRequest;
import ru.deelter.yookassa.utils.YooUrls;

import java.util.UUID;

public class PaymentGetRequest extends YooRequest {

	public PaymentGetRequest(@NotNull UUID paymentId) {
		super(String.format(YooUrls.PAYMENT_GET, paymentId), RequestMethod.GET);
	}
}
