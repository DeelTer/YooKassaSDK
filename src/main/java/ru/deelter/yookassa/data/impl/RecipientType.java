package ru.deelter.yookassa.data.impl;

import com.google.gson.annotations.SerializedName;

public class RecipientType {

	@SerializedName("account_id")
	private final String accountId;

	@SerializedName("gateway_id")
	private final String gatewayId;

	public RecipientType(String accountId, String gatewayId) {
		this.accountId = accountId;
		this.gatewayId = gatewayId;
	}

	public String getAccountId() {
		return accountId;
	}

	public String getGatewayId() {
		return gatewayId;
	}

	@Override
	public String toString() {
		return "RecipientType{" +
				"accountId='" + accountId + '\'' +
				", gatewayId='" + gatewayId + '\'' +
				'}';
	}
}
