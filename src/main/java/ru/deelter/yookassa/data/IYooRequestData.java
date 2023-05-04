package ru.deelter.yookassa.data;

import ru.deelter.yookassa.utils.JsonUtil;

public interface IYooRequestData {

	default String toJson() {
		return JsonUtil.toJson(this);
	}
}
