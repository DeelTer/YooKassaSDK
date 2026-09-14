package ru.deelter.yookassa;

import com.google.gson.*;
import okhttp3.*;
import okio.Buffer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.deelter.yookassa.model.JsonModel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

/** Exercises every facade operation against an in-memory transport and the pinned contract. */
@RunWith(Parameterized.class)
public class OperationsTest {
    private final JsonObject operation;
    public OperationsTest(String name, JsonObject operation) { this.operation = operation; }

    @Parameterized.Parameters(name="{0}")
    public static Collection<Object[]> operations() throws Exception {
        JsonObject coverage = read("openapi/coverage.json");
        JsonObject spec = read("openapi/yookassa.json");
        Set<String> declared = new HashSet<>();
        for (Map.Entry<String, JsonElement> path : spec.getAsJsonObject("paths").entrySet()) {
            for (String verb : Arrays.asList("get", "post", "delete", "put", "patch")) {
                if (path.getValue().getAsJsonObject().has(verb)) declared.add(verb.toUpperCase(Locale.ROOT)+" "+path.getKey());
            }
        }
        List<Object[]> result = new ArrayList<>();
        Set<String> implemented = new HashSet<>();
        for (JsonElement item : coverage.getAsJsonArray("operations")) {
            JsonObject operation = item.getAsJsonObject();
            String route = operation.get("http_method").getAsString()+" "+operation.get("path").getAsString();
            assertTrue("Duplicate route", implemented.add(route));
            result.add(new Object[] {route, operation});
        }
        assertEquals("Every spec operation must have a facade method", declared, implemented);
        return result;
    }

    static JsonObject read(String file) throws Exception {
        return JsonParser.parseString(new String(Files.readAllBytes(Paths.get(file)), StandardCharsets.UTF_8)).getAsJsonObject();
    }

    @Test public void routeHeadersBodyAndResponseMatchContract() throws Exception {
        AtomicReference<Request> sent = new AtomicReference<>();
        String response = "{\"id\":\"pd-reference\",\"future_field\":{\"value\":123.4567890123456789}}";
        OkHttpClient http = new OkHttpClient.Builder().addInterceptor(chain -> {
            sent.set(chain.request());
            return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200)
                    .message("OK").body(ResponseBody.create(response, YooKassa.MEDIA_TYPE_JSON)).build();
        }).build();
        YooKassa api = YooKassa.builder().oauth("test-oauth").httpClient(http).build();
        List<Class<?>> types = new ArrayList<>();
        List<Object> arguments = new ArrayList<>();
        JsonObject expectedBody = null;
        for (JsonElement item : operation.getAsJsonArray("arguments")) {
            String type = item.getAsJsonArray().get(0).getAsString();
            String name = item.getAsJsonArray().get(1).getAsString();
            if (type.equals("String")) {
                types.add(String.class);
                arguments.add(name.equals("idempotenceKey") ? "persisted-operation-1" : "id/with?special#%+chars");
            } else {
                Class<?> clazz = Class.forName(type.equals("JsonModel") ? "ru.deelter.yookassa.model.JsonModel" : "ru.deelter.yookassa.model."+type);
                JsonModel model = (JsonModel)clazz.getConstructor().newInstance();
                if (name.equals("body")) {
                    model.setField("metadata", JsonParser.parseString("{\"order\":\"123\"}"));
                    expectedBody = model.toJsonObject();
                }
                types.add(clazz);
                arguments.add(model);
            }
        }
        Method method = YooKassa.class.getMethod(operation.get("method").getAsString(), types.toArray(new Class<?>[0]));
        Object result = method.invoke(api, arguments.toArray());
        Request request = sent.get();
        assertNotNull(request);
        assertEquals(operation.get("http_method").getAsString(), request.method());
        assertEquals("https", request.url().scheme());
        assertEquals("api.yookassa.ru", request.url().host());
        String expectedPath = "/v3"+operation.get("path").getAsString().replaceAll("\\{[^}]+\\}", "id%2Fwith%3Fspecial%23%25+chars");
        assertEquals(expectedPath, request.url().encodedPath());
        assertEquals("Bearer test-oauth", request.header("Authorization"));
        assertEquals(request.method().equals("GET") ? null : "persisted-operation-1", request.header("Idempotence-Key"));
        if (request.method().equals("POST")) {
            Buffer buffer = new Buffer();
            request.body().writeTo(buffer);
            assertEquals(expectedBody == null ? new JsonObject() : expectedBody, JsonParser.parseString(buffer.readUtf8()));
        } else assertNull(request.body());
        if (operation.get("response").getAsString().equals("void")) assertNull(result);
        else assertEquals(JsonParser.parseString(response), ((JsonModel)result).toJsonObject());
    }
}
