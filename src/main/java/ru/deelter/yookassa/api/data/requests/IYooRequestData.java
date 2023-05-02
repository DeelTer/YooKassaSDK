package ru.deelter.yookassa.api.data.requests;

import ru.deelter.yookassa.utils.JsonUtil;

public interface IYooRequestData {

	default String toJson() {
		return JsonUtil.toJson(this);
	}
}
