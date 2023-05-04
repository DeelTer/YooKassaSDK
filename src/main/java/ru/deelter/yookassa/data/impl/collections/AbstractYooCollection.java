package ru.deelter.yookassa.data.impl.collections;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;
import ru.deelter.yookassa.data.IYooObject;

import java.util.Collection;
import java.util.UUID;

public abstract class AbstractYooCollection<T extends IYooObject> implements IYooObject {

	private final String type;
	private final Collection<T> items;
	@SerializedName("next_cursor")
	private final UUID nextCursor;


	protected AbstractYooCollection(@NotNull String type, @NotNull Collection<T> items, @NotNull UUID nextCursor) {
		this.type = type;
		this.items = items;
		this.nextCursor = nextCursor;
	}

	public String getType() {
		return type;
	}

	public Collection<T> getItems() {
		return items;
	}

	public UUID getNextCursor() {
		return nextCursor;
	}
}
