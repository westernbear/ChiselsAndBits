package mod.chiselsandbits.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import mod.chiselsandbits.core.Log;
import net.fabricmc.api.EnvType;
import net.minecraft.client.Minecraft;

public final class LanguageHandler {
    private LanguageHandler() {}

    public static void loadLangPath(final String path) {
        LanguageCache.INSTANCE.load(path);
    }

    public static String translateKey(final String key) {
        final String normalizedKey = key.toLowerCase(Locale.US);
        return LanguageCache.INSTANCE.languageMap.getOrDefault(normalizedKey, normalizedKey);
    }

    private static class LanguageCache {
        private static final LanguageCache INSTANCE = new LanguageCache();
        private Map<String, String> languageMap = Map.of();

        private LanguageCache() {
            load("assets/" + Constants.MOD_ID + "/lang/%s.json");
        }

        private void load(final String path) {
            final String defaultLocale = "en_us";
            String locale = EnvExecutor.callWhenOn(
                    EnvType.CLIENT,
                    () -> () -> Minecraft.getInstance() == null || Minecraft.getInstance().options == null
                            ? null
                            : Minecraft.getInstance().options.languageCode);

            if (locale == null) {
                locale = defaultLocale;
            }

            final ClassLoader loader = Thread.currentThread().getContextClassLoader();
            InputStream stream = loader.getResourceAsStream(String.format(path, locale));
            if (stream == null && !defaultLocale.equals(locale)) {
                stream = loader.getResourceAsStream(String.format(path, defaultLocale));
            }

            if (stream == null) {
                return;
            }

            try (InputStream input = stream;
                    InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                final Map<String, String> loaded =
                        new Gson().fromJson(reader, new TypeToken<Map<String, String>>() {}.getType());
                languageMap = loaded == null ? Map.of() : loaded;
            } catch (IOException | RuntimeException e) {
                Log.logError("Could not load language.", e);
            }
        }
    }
}
