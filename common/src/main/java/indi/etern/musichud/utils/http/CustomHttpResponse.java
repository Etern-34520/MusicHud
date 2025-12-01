package indi.etern.musichud.utils.http;

import com.fasterxml.jackson.core.JacksonException;
import indi.etern.musichud.utils.JsonUtil;

import javax.net.ssl.SSLSession;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

public class CustomHttpResponse implements HttpResponse<String> {
    HttpResponse<String> response;

    public CustomHttpResponse(HttpResponse<String> response) {
        this.response = response;
    }

    protected static CustomHttpResponse from(HttpResponse<String> response) {
        return new CustomHttpResponse(response);
    }

    public <T> T bodyAs(Class<T> clazz) throws JacksonException {
        return JsonUtil.objectMapper.readValue(response.body(), clazz);
    }

    @Override
    public int statusCode() {
        return response.statusCode();
    }

    @Override
    public HttpRequest request() {
        return response.request();
    }

    @Override
    public Optional<HttpResponse<String>> previousResponse() {
        return response.previousResponse();
    }

    @Override
    public HttpHeaders headers() {
        return response.headers();
    }

    @Override
    public String body() {
        return response.body();
    }

    @Override
    public Optional<SSLSession> sslSession() {
        return response.sslSession();
    }

    @Override
    public URI uri() {
        return response.uri();
    }

    @Override
    public HttpClient.Version version() {
        return response.version();
    }
}
