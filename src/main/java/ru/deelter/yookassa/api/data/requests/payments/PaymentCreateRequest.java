package ru.deelter.yookassa.api.data.requests.payments;

import org.jetbrains.annotations.NotNull;
import ru.deelter.yookassa.api.data.requests.PaymentCreateData;
import ru.deelter.yookassa.api.data.requests.YooRequest;
import ru.deelter.yookassa.utils.YooUrls;

public class PaymentCreateRequest extends YooRequest {

	public PaymentCreateRequest(@NotNull PaymentCreateData data) {
		super(YooUrls.PAYMENT_CREATE, RequestMethod.POST, data);
	}
}
