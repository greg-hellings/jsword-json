package jacknife;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.crosswire.jsword.book.Book;
import org.crosswire.jsword.book.BookData;
import org.crosswire.jsword.book.BookException;
import org.crosswire.jsword.book.Books;
import org.crosswire.jsword.book.OSISUtil;
import org.crosswire.jsword.passage.Key;
import org.crosswire.jsword.passage.NoSuchKeyException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import io.undertow.websockets.spi.WebSocketHttpExchange;

public class SwordHandler implements HttpHandler, WebSocketConnectionCallback {
	
	private HttpServerExchange exchange;
	private static Map<Book, List<String>> sections = new HashMap<Book, List<String>>();
	private static Map<Book, Map<Key, String>> entries = new HashMap<Book, Map<Key,String>>();
	
	/*
	 * HELPERS
	 */
	private void handleError(String message) {
		JSONObject response = new JSONObject();
		response.put("success", false);
		response.put("message", message);
		this.exchange.setResponseCode(500);
		this.exchange.getResponseSender().send(response.toString());
	}

	private Book getBook(String name) {
		Books books = Books.installed();
		return books.getBook(name);
	}

	/*
	 * Raw HTTP handler
	 * 
	 * @see io.undertow.server.HttpHandler#handleRequest(io.undertow.server.HttpServerExchange)
	 */
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		this.exchange = exchange;
		String path = exchange.getRequestPath();
		String response;
		
		exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
		if (path.equals("/")) {
			response = this.listModules();
		} else {
			String[] parts = path.split("/");
			try {
				if(parts.length == 2) {
					response = this.listSections(parts[1]);
				} else {
					response = this.listEntries(parts[1], parts[2]);
				}
			} catch(NoSuchKeyException ex) {
				this.handleError("It looks like you requested an invalid module key.");
				ex.printStackTrace();
				return;
			} catch(BookException ex) {
				this.handleError("It looks like you requested a module that is not installed.");
				return;
			}
		}
		
		exchange.getResponseSender().send(response);
	}

	/*
	 * WebSocket handler
	 * 
	 * @see io.undertow.websockets.WebSocketConnectionCallback#onConnect(io.undertow.websockets.spi.WebSocketHttpExchange, io.undertow.websockets.core.WebSocketChannel)
	 */
	public void onConnect(WebSocketHttpExchange exchange,
			WebSocketChannel channel) {
		channel.getReceiveSetter().set(new AbstractReceiveListener() {
			@Override
			public void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) {
				JSONObject request = new JSONObject(message.getData());
				
				try {
					if (!request.has("module")) {
						WebSockets.sendText(listModules(), channel, null);
					} else if(!request.has("key")) {
						WebSockets.sendText(listSections(request.getString("module")), channel, null);
					} else {
						WebSockets.sendText(listEntries(request.getString("module"), request.getString("key")), channel, null);
					}
				} catch(BookException e) {
					JSONObject response = new JSONObject();
					response.put("success", false);
					response.put("message", "It appears you have selected an invalid module.");
					WebSockets.sendText(response.toString(), channel, null);
				} catch (NoSuchKeyException e) {
					JSONObject response = new JSONObject();
					response.put("success", false);
					response.put("message", "You have requested an invalid key.");
					WebSockets.sendText(response.toString(), channel, null);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		channel.resumeReceives();
	}

	private String listEntries(String module, String reference) throws NoSuchKeyException, BookException {
		Book book = this.getBook(module);
		JSONArray verses = new JSONArray();
		Key baseKey = book.getKey(reference);
		
		for (Key key : baseKey) {
			BookData data = new BookData(book, key);
			JSONObject entry = new JSONObject();
			
			entry.put("reference", key.getOsisID());
			entry.put("text", OSISUtil.getCanonicalText(data.getOsisFragment()));
			verses.put(entry);
		}
		
		return verses.toString();
	}
	
	private String listSections(String module) throws BookException, NoSuchKeyException {
		Book   book   = this.getBook(module);
		JSONObject response = new JSONObject();
		List<String> refList = null;
		JSONArray refArray;
		
		if (SwordHandler.sections.containsKey(book)) {
			refList = SwordHandler.sections.get(book);
			refArray = new JSONArray(refList);
		} else if (book != null) {
			refList = new ArrayList<String>();
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
			SwordHandler.sections.put(book, refList);
		} else {
			this.handleError("No such book found.");
			return "";
		}
		
		// Insert results to the response object
		if (refList.size() != 0) {
			refArray = new JSONArray(refList);
			response.put("success", true);
			response.put("references", refArray);
			response.put("message", "Append one of these references to your URL path to retrive its contents.");
		} else {
			this.handleError("No top-level references found.");
		}
		
		return response.toString();
	}

	private String listModules() {
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
		
		return object.toString();
	}

}
