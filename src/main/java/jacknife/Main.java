package jacknife;

import io.undertow.Undertow;

import static io.undertow.Handlers.path;
import static io.undertow.Handlers.websocket;

// Leave these in to enhance the development of client-side stuff
import org.jamon.compiler.RecompilingTemplateManager;
import org.jamon.TemplateManagerSource;

public class Main {
	public static void main(String[] args) {
		String host = "localhost";
		if (args.length > 1) {
			host = args[1];
		}
        System.out.println("Binding to " + host);

		SwordHandler handler = new SwordHandler();
		
		/*  Un-comment these lines if you want your life to be a whole bunch easier while hacking on the Jamon template
		 // as this will allow the template to be dynamically loaded with just an F5 refresh. Leave commented out when
		 // pushing data to production.
		RecompilingTemplateManager.Data data = new RecompilingTemplateManager.Data();
		data.setSourceDir("src/main/jamon");
		data.setWorkDir("src/main/jamon-gen");
		TemplateManagerSource.setTemplateManager(new RecompilingTemplateManager(data));
		//*/
		
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
