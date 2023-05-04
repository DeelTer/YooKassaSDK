package ru.deelter.yookassa.requests.payments;

import org.jetbrains.annotations.NotNull;
import ru.deelter.yookassa.data.impl.requests.PaymentCreateData;
import ru.deelter.yookassa.data.impl.requests.YooRequest;
import ru.deelter.yookassa.utils.YooRequestUrls;

public class PaymentCreateRequest extends YooRequest {

	public PaymentCreateRequest(@NotNull PaymentCreateData data) {
		super(YooRequestUrls.PAYMENT_CREATE, RequestMethod.POST, data);
	}
}
