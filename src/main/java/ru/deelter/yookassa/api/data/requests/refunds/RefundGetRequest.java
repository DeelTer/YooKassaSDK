package ru.deelter.yookassa.api.data.requests.refunds;

import org.jetbrains.annotations.NotNull;
import ru.deelter.yookassa.api.data.requests.YooRequest;
import ru.deelter.yookassa.utils.YooUrls;

import java.util.UUID;

public class RefundGetRequest extends YooRequest {

	public RefundGetRequest(@NotNull UUID paymentId) {
		super(String.format(YooUrls.REFUND_GET, paymentId), RequestMethod.GET);
	}
}
