package indi.etern.musichud.interfaces;

public interface IntegerCodeEnum {
//    @JsonValue
    int getCode();
/*
    class Serializer implements JsonSerializer<IntegerCodeEnum> {
        @Override
        public JsonElement serialize(IntegerCodeEnum src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.getCode());
        }
    }
*/
}
