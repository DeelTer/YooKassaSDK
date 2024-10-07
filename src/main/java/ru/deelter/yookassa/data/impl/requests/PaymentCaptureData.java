package ru.deelter.yookassa.data.impl.requests;

import org.jetbrains.annotations.Nullable;
import ru.deelter.yookassa.data.IYooRequestData;
import ru.deelter.yookassa.data.impl.Amount;

public class PaymentCaptureData implements IYooRequestData {

    private final Amount amount;

    public PaymentCaptureData(@Nullable Amount amount) {
        this.amount = amount;
    }

    public Amount getAmount() {
        return amount;
    }
}