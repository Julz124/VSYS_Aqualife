package src.aqua.blatt1;

import java.io.Serializable;

@SuppressWarnings("serial")
public final class RegisterResponse implements Serializable {
	private final String id;

	public RegisterResponse(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

}
