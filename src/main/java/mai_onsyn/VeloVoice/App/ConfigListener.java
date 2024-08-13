package mai_onsyn.VeloVoice.App;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ibm.icu.impl.locale.XCldrStub;
import javafx.scene.paint.Color;
import mai_onsyn.AnimeFX.Frame.Module.FXLogger;
import mai_onsyn.AnimeFX.Frame.Utils.Toolkit;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.*;

import static mai_onsyn.VeloVoice.App.Theme.*;
import static mai_onsyn.VeloVoice.App.AppConfig.*;

public class ConfigListener {
    public static Thread CONFIG_SAVE_THREAD;

    private static final String configPath = System.getProperty("user.dir") + "\\settings.json";
    private static JSONObject configJson;
    private static final DecimalFormat decimalFormat = new DecimalFormat("#.##");

    static boolean light_theme_cache = false;
    static Color theme_color_cache = Color.DARKCYAN;

    static {
        try {
            String jsonString = Files.readString(Path.of(configPath));
            configJson = JSONObject.parseObject(jsonString);

            switch (configJson.getString("Theme")) {
                case "Light" -> light_theme_cache = true;
                case "Dark" -> light_theme_cache = false;
            }
            theme_color_cache = Color.web(configJson.getString("ThemeColor"));

            //触发Theme类加载
            BACKGROUND_AMBIGUITY = configJson.getDouble("BackgroundAmbiguity");
            BACKGROUND_BRIGHTNESS = configJson.getDouble("BackgroundBrightness");
            BACKGROUND_IMAGE_URI = configJson.getString("BackgroundImageURI");

            maxConnectThread = configJson.getInteger("ConnectThreadCount");
            retryCount = configJson.getInteger("RetryCount");
            timeoutSeconds = configJson.getInteger("TimeoutSeconds");

            textPieceSize = configJson.getInteger("TextPieceLength");
            String symbolString = configJson.getString("TextSplitSymbols");
            if (symbolString != null && !symbolString.isEmpty()) {
                textSplitSymbols.clear();
                textSplitSymbols.addAll(symbolString.chars().mapToObj(c -> (char) c).toList());
            }
            isAppendVolumeName = configJson.getBoolean("WebChapterAppendVolumeName");
            isAppendOrdinal = configJson.getBoolean("SaveFileAppendOrdinal");

            logLevel = configJson.getInteger("LogLevel");
        }
        catch (Exception e) {
            System.out.println("The correctly formatted configuration file was not found!");
            System.out.println("Overwrite the profile with the default configuration");
            writeConfigurations();
        }

        VariableChangeListener.cacheCurrentValues();
        CONFIG_SAVE_THREAD = Thread.ofVirtual().name("Config-Save-Thread").unstarted(() -> {
            try {
                while (true) {
                    if (VariableChangeListener.checkForChanges()) {
                        VariableChangeListener.cacheCurrentValues();
                        writeConfigurations();
                    }
                    Thread.sleep(2500); //每隔一坤秒保存一次配置文件
                }
            } catch (Exception _) {}
        });
    }

    private static void writeConfigurations() {
        configJson = new JSONObject();

        configJson.put("Theme", LIGHT_THEME ? "Light" : "Dark");
        configJson.put("BackgroundAmbiguity", BACKGROUND_AMBIGUITY);
        configJson.put("BackgroundBrightness", BACKGROUND_BRIGHTNESS);
        configJson.put("BackgroundImageURI", BACKGROUND_IMAGE_URI);
        configJson.put("ThemeColor", "#" + Toolkit.colorToString(THEME_COLOR));

        configJson.put("ConnectThreadCount", maxConnectThread);
        configJson.put("RetryCount", retryCount);
        configJson.put("TimeoutSeconds", timeoutSeconds);

        configJson.put("TextPieceLength", textPieceSize);
        configJson.put("TextSplitSymbols", XCldrStub.Joiner.on("").join(textSplitSymbols));
        configJson.put("WebChapterAppendVolumeName", isAppendVolumeName);
        configJson.put("SaveFileAppendOrdinal", isAppendOrdinal);

        configJson.put("LogLevel", logLevel);

        try (FileWriter fw = new FileWriter(configPath)) {
            fw.write(JSON.toJSONString(configJson, true));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static class VariableChangeListener {
        private static boolean cached_LIGHT_THEME;
        private static double cached_BACKGROUND_AMBIGUITY;
        private static double cached_BACKGROUND_BRIGHTNESS;
        private static String cached_BACKGROUND_IMAGE_URI;
        private static Color cached_THEME_COLOR;

        private static int cachedMaxConnectThread;
        private static int cachedRetryCount;
        private static int cachedTimeoutSeconds;

        private static int cachedTextPieceSize;
        private static List<Character> cachedTextSplitSymbols = new ArrayList<>();
        private static boolean cachedIsAppendVolumeName;
        private static boolean cachedIsAppendOrdinal;

        static void cacheCurrentValues() {
            cached_LIGHT_THEME = LIGHT_THEME;
            cached_BACKGROUND_AMBIGUITY = BACKGROUND_AMBIGUITY;
            cached_BACKGROUND_BRIGHTNESS = BACKGROUND_BRIGHTNESS;
            cached_BACKGROUND_IMAGE_URI = BACKGROUND_IMAGE_URI;
            cached_THEME_COLOR = THEME_COLOR;

            cachedMaxConnectThread = maxConnectThread;
            cachedRetryCount = retryCount;
            cachedTimeoutSeconds = timeoutSeconds;

            cachedTextPieceSize = textPieceSize;
            cachedTextSplitSymbols = new ArrayList<>(textSplitSymbols);
            cachedIsAppendVolumeName = isAppendVolumeName;
            cachedIsAppendOrdinal = isAppendOrdinal;
        }

        static boolean checkForChanges() {
            return LIGHT_THEME != cached_LIGHT_THEME ||
                    Double.compare(BACKGROUND_AMBIGUITY, cached_BACKGROUND_AMBIGUITY) != 0 ||
                    Double.compare(BACKGROUND_BRIGHTNESS, cached_BACKGROUND_BRIGHTNESS) != 0 ||
                    !Objects.equals(BACKGROUND_IMAGE_URI, cached_BACKGROUND_IMAGE_URI) ||
                    !Objects.equals(THEME_COLOR, cached_THEME_COLOR) ||
                    maxConnectThread != cachedMaxConnectThread ||
                    retryCount != cachedRetryCount ||
                    timeoutSeconds != cachedTimeoutSeconds ||
                    textPieceSize != cachedTextPieceSize ||
                    !Objects.equals(textSplitSymbols, cachedTextSplitSymbols) ||
                    isAppendVolumeName != cachedIsAppendVolumeName ||
                    isAppendOrdinal != cachedIsAppendOrdinal;
        }
    }

}