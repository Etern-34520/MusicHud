package indi.etern.musichud.throwable;

import java.net.ConnectException;

public class ApiException extends RuntimeException {
    public ApiException(ConnectException e) {
        super(e);
    }

    public ApiException() {
        super();
    }

    @Override
    public String getMessage() {
        return "无法连接到 API 服务器";
    }
}
