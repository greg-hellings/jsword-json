package jacknife;

import io.undertow.Undertow;

import static io.undertow.Handlers.path;
import static io.undertow.Handlers.websocket;

public class Main {
	public static void main(String[] args) {
		SwordHandler handler = new SwordHandler();
		
		Undertow server = Undertow.builder()
				.addListener(10001, "localhost")
				.setHandler(handler)
				.build();
		
		Undertow socketServer = Undertow.builder()
				.addListener(10002, "localhost")
				.setHandler(path().addPrefixPath("/", websocket(handler)))
				.build();
		
		server.start();
		socketServer.start();
	}
}
