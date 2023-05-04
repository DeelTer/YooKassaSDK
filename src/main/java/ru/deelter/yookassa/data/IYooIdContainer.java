package ru.deelter.yookassa.data;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public interface IYooIdContainer extends IYooObject {

	@NotNull UUID getId();

}
