package ru.deelter.yookassa.requests.payments;

import org.jetbrains.annotations.NotNull;
import ru.deelter.yookassa.data.impl.requests.EmptyData;
import ru.deelter.yookassa.data.impl.requests.YooRequest;
import ru.deelter.yookassa.utils.YooRequestUrls;

import java.util.UUID;

public class PaymentCancelRequest extends YooRequest {

    public PaymentCancelRequest(@NotNull UUID paymentId) {
        super(String.format(YooRequestUrls.PAYMENT_CANCEL, paymentId), RequestMethod.POST, new EmptyData());
    }
}
