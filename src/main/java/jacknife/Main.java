package jacknife;

import io.undertow.Undertow;

public class Main {
	public static void main(String[] args) {
		Undertow server = Undertow.builder()
				.addListener(10001, "localhost")
				.setHandler(new SwordHandler())
				.build();
		
		server.start();
	}
}
