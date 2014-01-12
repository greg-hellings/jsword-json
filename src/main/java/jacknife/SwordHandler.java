package jacknife;

import java.util.ArrayList;
import java.util.List;

import org.crosswire.jsword.book.Book;
import org.crosswire.jsword.book.BookCategory;
import org.crosswire.jsword.book.BookData;
import org.crosswire.jsword.book.BookException;
import org.crosswire.jsword.book.Books;
import org.crosswire.jsword.book.OSISUtil;
import org.crosswire.jsword.passage.Key;
import org.crosswire.jsword.passage.NoSuchKeyException;
import org.json.JSONArray;
import org.json.JSONObject;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

public class SwordHandler implements HttpHandler {
	
	private void handleError(String message, HttpServerExchange exchange) {
		JSONObject response = new JSONObject();
		response.put("success", false);
		response.put("message", message);
		exchange.setResponseCode(500);
		exchange.getResponseSender().send(response.toString());
	}

	public void handleRequest(HttpServerExchange exchange) throws Exception {
		String path = exchange.getRequestPath();
		
		exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
		if (path.equals("/")) {
			this.listModules(exchange);
		} else {
			String[] parts = path.split("/");
			try {
				if(parts.length == 2) {
					this.listSections(parts, exchange);
				} else {
					this.listEntries(parts, exchange);
				}
			} catch(NoSuchKeyException ex) {
				this.handleError("It looks like you requested an invalid module key.", exchange);
			} catch(BookException ex) {
				this.handleError("It looks like you requested a module that is not installed.", exchange);
			}
		}
	}

	private void listEntries(String[] parts, HttpServerExchange exchange) throws NoSuchKeyException, BookException {
		Book book = this.getBook(parts[1]);
		JSONArray verses = new JSONArray();
		
		for (Key key : book.getKey(parts[2])) {
			BookData data = new BookData(book, key);
			JSONObject entry = new JSONObject();
			
			entry.put("reference", key.getOsisID());
			entry.put("text", OSISUtil.getCanonicalText(data.getOsisFragment()));
			verses.put(entry);
		}
		
		exchange.getResponseSender().send(verses.toString());
	}

	private Book getBook(String name) {
		Books books = Books.installed();
		return books.getBook(name);
	}
	
	private void listSections(String[] parts, HttpServerExchange exchange) throws BookException, NoSuchKeyException {
		Book   book   = this.getBook(parts[1]);
		JSONObject response = new JSONObject();
		List<String> refList = new ArrayList<String>();
		JSONArray refArray;
		
		if (book != null) {
			Key allKeys = book.getGlobalKeyList();
			// Iterate all keys, pulling out unique top-level keys
			for (Key ref : allKeys) {
				String[] refParts = ref.getOsisID().split("\\.");
				if (refParts.length == 0) {
					System.out.println("Invalid key found: " + ref.getOsisID());
				} else if (!refList.contains(refParts[0])) {
					refList.add(refParts[0]);
				}
			}
			// Insert results to the response object
			if (refList.size() != 0) {
				refArray = new JSONArray(refList);
				response.put("success", true);
				response.put("references", refArray);
				response.put("message", "Append one of these references to your URL path to retrive its contents.");
			} else {
				this.handleError("No top-level references found.", exchange);
			}
		} else {
			this.handleError("No such book found.", exchange);
		}
		
		exchange.getResponseSender().send(response.toString());
	}

	private void listModules(HttpServerExchange exchange) {
		JSONObject object = new JSONObject();
		JSONArray  modules = new JSONArray();
		JSONObject module;
		
		Books books = Books.installed();
		for (Book book : books.getBooks()) {
			module = new JSONObject();
			module.put("name", book.getName());
			module.put("key", book.getInitials());
			module.put("category", book.getBookCategory());
			modules.put(module);
		}
		
		object.put("results", true);
		object.put("modules", modules);
		object.put("directions", "Select a module's key to get a listing of its core elements.");
		
		exchange.getResponseSender().send(object.toString());
	}

}
