package util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import model.LocalDateTimeAdapter;

import java.time.LocalDateTime;

public class JsonUtil {

    private static final Gson gson =
            new GsonBuilder()
                    .registerTypeAdapter(LocalDateTime.class,
                            new LocalDateTimeAdapter())
                    .create();

    public static String toJson(Object o) {
        return gson.toJson(o);
    }
}
