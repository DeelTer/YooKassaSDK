package ru.deelter.yookassa.api.data.requests.refunds;

import org.jetbrains.annotations.NotNull;
import ru.deelter.yookassa.api.data.requests.RefundCreateData;
import ru.deelter.yookassa.api.data.requests.YooRequest;
import ru.deelter.yookassa.utils.YooUrls;

public class RefundCreateRequest extends YooRequest {

	public RefundCreateRequest(@NotNull RefundCreateData data) {
		super(YooUrls.REFUND_CREATE, RequestMethod.POST, data);
	}
}
