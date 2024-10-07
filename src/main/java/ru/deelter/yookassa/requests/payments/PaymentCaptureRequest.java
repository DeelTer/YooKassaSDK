package ru.deelter.yookassa.requests.payments;

import org.jetbrains.annotations.NotNull;
import ru.deelter.yookassa.data.impl.requests.PaymentCaptureData;
import ru.deelter.yookassa.data.impl.requests.YooRequest;
import ru.deelter.yookassa.utils.YooRequestUrls;

import java.util.UUID;

public class PaymentCaptureRequest extends YooRequest {

    public PaymentCaptureRequest(@NotNull UUID paymentId, @NotNull PaymentCaptureData data) {
        super(String.format(YooRequestUrls.PAYMENT_CAPTURE, paymentId), RequestMethod.POST, data);
    }
}
