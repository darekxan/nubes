package integration.injection;

import static io.vertx.core.http.HttpVersion.HTTP_1_1;
import integration.VertxNubesTestBase;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;

import org.junit.Test;

public class TestMethodParamInjection extends VertxNubesTestBase {

	@Test
	public void testInjectSocketAddress(TestContext context) {
		Async async = context.async();
		getJSON("/params/injection/socketAddress", response -> {
			context.assertEquals(200, response.statusCode());
			response.bodyHandler(buff -> {
				JsonObject json = new JsonObject(buff.toString("UTF-8"));
				context.assertNotNull(json.getString("host")); // normally assertEquals("127.0.0.1") should do it, but on every machine ?
				async.complete();
			});
		});
	}

	@Test
	public void testInjectHttpVersion(TestContext context) {
		Async async = context.async();
		getJSON("/params/injection/httpVersion", response -> {
			context.assertEquals(200, response.statusCode());
			response.bodyHandler(buff -> {
				JsonObject json = new JsonObject(buff.toString("UTF-8"));
				context.assertEquals(HTTP_1_1.toString(), json.getString("version"));
				async.complete();
			});
		});
	}

	@Test
	public void testInjectHeaders(TestContext context) {
		Async async = context.async();
		String headerName = "someTestHeader";
		String headerValue = "theHeaderValue";
		client().get("/params/injection/headers?headerName=" + headerName, response -> {
			context.assertEquals(200, response.statusCode());
			response.bodyHandler(buff -> {
				context.assertEquals(headerValue, buff.toString("UTF-8"));
				async.complete();
			});
		}).putHeader(HttpHeaders.ACCEPT, "text/plain").putHeader(headerName, headerValue).end();
	}

	@Test
	public void testRequest(TestContext context) {
		Async async = context.async();
		String path = "/params/injection/request";
		client().get(path, response -> {
			context.assertEquals(200, response.statusCode());
			response.bodyHandler(buff -> {
				context.assertEquals(path, buff.toString("UTF-8"));
				async.complete();
			});
		}).putHeader(HttpHeaders.ACCEPT, "text/plain").end();
	}

	@Test
	public void testEventBus(TestContext context) {
		Async async = context.async();
		String path = "/params/injection/eventBus";
		client().get(path, response -> {
			context.assertEquals(200, response.statusCode());
			response.bodyHandler(buff -> {
				context.assertNotNull(buff.toString("UTF-8"));
				async.complete();
			});
		}).putHeader(HttpHeaders.ACCEPT, "text/plain").end();
	}

}
