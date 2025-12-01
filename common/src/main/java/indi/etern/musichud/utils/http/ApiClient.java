package indi.etern.musichud.utils.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import indi.etern.musichud.MusicHud;
import indi.etern.musichud.interfaces.PostProcessable;
import indi.etern.musichud.server.api.ServerApiMeta;
import indi.etern.musichud.throwable.ApiException;
import indi.etern.musichud.utils.JsonUtil;
import lombok.SneakyThrows;

import java.net.ConnectException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.Executors;

public class ApiClient {
    public static final HttpClient CLIENT;

    static {
        CLIENT = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(3))
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .build();
    }

    @SneakyThrows
    public static <T> T post(ServerApiMeta.UrlMeta<T> urlMeta, Object requestBody, String formattedUserCookie) {
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(urlMeta.toURI())
                    .setHeader("Content-Type", "application/json");
            if (requestBody != null) {
                JsonNode payload = requestBody instanceof JsonNode element ? element : JsonUtil.objectMapper.valueToTree(requestBody);
                if (payload instanceof ObjectNode objectNode) {
                    if (formattedUserCookie != null && !formattedUserCookie.isEmpty()) {
                        for (String cookieItem : formattedUserCookie.split(";;")) {
                            requestBuilder.header("Cookie", cookieItem);
                        }
                    } else {
                        objectNode.put("noCookie", true);
                    }
                    requestBuilder.POST(HttpRequest.BodyPublishers.ofString(
                                    payload.toString(),
                                    StandardCharsets.UTF_8
                            )
                    );
                } else {
                    throw new IllegalStateException();
                }
            } else {
                if (formattedUserCookie != null && !formattedUserCookie.isEmpty()) {
                    for (String cookieItem : formattedUserCookie.split(";;")) {
                        requestBuilder.header("Cookie", cookieItem);
                    }
                }
                requestBuilder.POST(HttpRequest.BodyPublishers.noBody());
            }
            HttpRequest request = requestBuilder
                    .build();
            try {
                HttpResponse<?> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                T t = JsonUtil.objectMapper.readValue(response.body().toString(), urlMeta.responseType());
                if (t instanceof PostProcessable postProcessable) {
                    postProcessable.postProcess();
                }
                return t;
            } catch (ConnectException e) {
                MusicHud.getLogger(ApiClient.class).error("请检查 API 服务器状态");
                throw e;
            }
        } catch (ConnectException e) {
            throw new ApiException(e);
        }
    }

    @SneakyThrows
    public static <T> T get(ServerApiMeta.UrlMeta<T> urlMeta, String formattedUserCookie) {
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(urlMeta.toURI());
            if (formattedUserCookie != null && !formattedUserCookie.isEmpty()) {
                for (String cookieItem : formattedUserCookie.split(";;")) {
                    requestBuilder.header("Cookie", cookieItem);
                }
            }
            HttpRequest request = requestBuilder
                    .GET()
                    .build();
            try {
                HttpResponse<?> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                T t = JsonUtil.objectMapper.readValue(response.body().toString(), urlMeta.responseType());
                if (t instanceof PostProcessable postProcessable) {
                    postProcessable.postProcess();
                }
                return t;
            } catch (ConnectException e) {
                MusicHud.getLogger(ApiClient.class).error("请检查 API 服务器状态");
                throw e;
            }
        } catch (ConnectException e) {
            throw new ApiException(e);
        }
    }
}
