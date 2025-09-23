package client; 
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class JsonUtil {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    public static String toJson(Object o) { return GSON.toJson(o); }
    public static <T> T fromJson(String s, Class<T> t) { return GSON.fromJson(s, t); }
}
