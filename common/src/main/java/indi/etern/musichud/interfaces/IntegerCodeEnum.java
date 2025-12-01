package indi.etern.musichud.interfaces;

import com.fasterxml.jackson.annotation.JsonValue;

public interface IntegerCodeEnum {
    @JsonValue
    int getCode();
}
