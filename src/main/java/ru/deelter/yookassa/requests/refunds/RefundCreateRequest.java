package ru.deelter.yookassa.requests.refunds;

import org.jetbrains.annotations.NotNull;
import ru.deelter.yookassa.data.impl.requests.RefundCreateData;
import ru.deelter.yookassa.data.impl.requests.YooRequest;
import ru.deelter.yookassa.utils.YooRequestUrls;

public class RefundCreateRequest extends YooRequest {

	public RefundCreateRequest(@NotNull RefundCreateData data) {
		super(YooRequestUrls.REFUND_CREATE, RequestMethod.POST, data);
	}
}
