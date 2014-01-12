package jacknife;

import io.undertow.Undertow;

import static io.undertow.Handlers.path;
import static io.undertow.Handlers.websocket;

public class Main {
	public static void main(String[] args) {
		String host = "localhost";
		if (args.length > 1) {
			host = args[1];
		}
        System.out.println("Binding to " + host);

		SwordHandler handler = new SwordHandler();
		
		Undertow server = Undertow.builder()
				.addListener(10001, host)
				.setHandler(handler)
				.build();
		
		Undertow socketServer = Undertow.builder()
				.addListener(10002, host)
				.setHandler(path().addPrefixPath("/", websocket(handler)))
				.build();
		
		server.start();
		socketServer.start();
	}
}
