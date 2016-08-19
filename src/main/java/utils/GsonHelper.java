package utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class GsonHelper
{
    private GsonHelper() {}

    public static Long getLongOrNull(JsonElement jsonElement)
    {
        return getLongOrDefaultValue(jsonElement,null);
    }

    public static JsonArray getJsonArrayOrNull(JsonElement jsonElement)
    {
        if (jsonElement != null && !jsonElement.isJsonNull()) {
            return jsonElement.getAsJsonArray();
        }
        return null;
    }

    public static JsonObject getJsonObjectOrNull(JsonElement jsonElement)
    {
        if (jsonElement != null && !jsonElement.isJsonNull()) {
            return jsonElement.getAsJsonObject();
        }
        return null;
    }

    public static String getStringOrNull(JsonElement jsonElement)
    {
        return getStringOrDefaultValue(jsonElement,null);
    }

    public static Boolean getBooleanOrDefaultValue(JsonElement jsonElement, Boolean defaultValue)
    {
        return jsonElement != null ? jsonElement.getAsBoolean() : defaultValue;
    }

    public static Long getLongOrDefaultValue(JsonElement jsonElement, Long defaultValue)
    {
        if (jsonElement != null && !jsonElement.isJsonNull()) {
            return jsonElement.getAsLong();
        }
        return defaultValue;
    }

    public static String getStringOrDefaultValue(JsonElement jsonElement, String defaultValue)
    {
        if (jsonElement != null && !jsonElement.isJsonNull()) {
            return jsonElement.getAsString();
        }
        return defaultValue;
    }

    public static Boolean ifNullFalse(JsonElement jsonElement)
    {
        return getBooleanOrDefaultValue(jsonElement,false);
    }
}
