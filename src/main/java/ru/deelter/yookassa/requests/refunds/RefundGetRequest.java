package ru.deelter.yookassa.requests.refunds;

import org.jetbrains.annotations.NotNull;
import ru.deelter.yookassa.data.impl.requests.YooRequest;
import ru.deelter.yookassa.utils.YooRequestUrls;

import java.util.UUID;

public class RefundGetRequest extends YooRequest {

	public RefundGetRequest(@NotNull UUID paymentId) {
		super(String.format(YooRequestUrls.REFUND_GET, paymentId), RequestMethod.GET);
	}
}
