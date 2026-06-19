package paulevs.betterweather.config;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class represents config entry
 * @param <T> stored data type
 */
public class ConfigEntry <T> {
	private final List<String> comments = new ArrayList<>();
	private final String name;
	protected T value;

	protected ConfigEntry(String name, T value, List<String> comments) {
		this.comments.addAll(comments);
		this.value = value;
		this.name = name;
	}

	/** Replace the stored value at runtime (used by live config edits from the in-game GUI). */
	@SuppressWarnings("unchecked")
	void setValueUnchecked(Object newValue) {
		this.value = (T) newValue;
	}

	protected void append(FileWriter writer) throws IOException {
		for (String comment : comments) {
			writer.append("# ");
			writer.append(comment);
			writer.append('\n');
		}
		writer.append(name);
		writer.append(" = ");
		writer.append(value.toString());
		writer.append('\n');
	}
}
