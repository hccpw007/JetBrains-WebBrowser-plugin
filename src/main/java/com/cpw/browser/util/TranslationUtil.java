// 多语言翻译工具类，根据用户设置的语言偏好加载对应语言包
package com.cpw.browser.util;

import com.cpw.browser.settings.BrowserSettingsState;

import java.io.InputStream;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

public final class TranslationUtil {

    // 语言包文件路径模板
    private static final String BUNDLE_PATH = "/messages/MyMessageBundle";
    // 默认语言包文件
    private static final String DEFAULT_BUNDLE = BUNDLE_PATH + ".properties";
    // 语言包文件后缀
    private static final String BUNDLE_SUFFIX = ".properties";

    private TranslationUtil() {
    }

    // 缓存：语言代码 -> Properties
    private static final Map<String, Properties> bundleCache = new HashMap<>();
    // 当前语言代码
    private static String currentLang = null;

    // 根据当前语言设置获取翻译文本
    public static String getText(String key) {
        Properties props = getProperties();
        String value = props.getProperty(key);
        return value != null ? value : key;
    }

    // 根据当前语言设置获取翻译文本，并格式化参数
    public static String getText(String key, Object... args) {
        String pattern = getText(key);
        try {
            return MessageFormat.format(pattern, args);
        } catch (IllegalArgumentException e) {
            return pattern;
        }
    }

    // 获取当前语言设置对应的 Properties，带缓存
    private static Properties getProperties() {
        String lang = BrowserSettingsState.getInstance().getLanguage();
        if (lang == null || lang.isEmpty()) {
            lang = "default";
        }
        // 缓存未命中或语言已变更时重新加载
        if (!lang.equals(currentLang) || !bundleCache.containsKey(lang)) {
            loadBundle(lang);
            currentLang = lang;
        }
        return bundleCache.get(lang);
    }

    // 加载指定语言的 Properties 文件
    private static void loadBundle(String lang) {
        Properties props = new Properties();

        // 先加载默认（英语）包作为基础
        loadPropertiesFile(DEFAULT_BUNDLE, props);

        // 非默认语言时加载对应的语言包，覆盖默认值
        if (!"default".equals(lang)) {
            String langFile = BUNDLE_PATH + "_" + lang + BUNDLE_SUFFIX;
            loadPropertiesFile(langFile, props);
        }

        bundleCache.put(lang, props);
    }

    // 从 classpath 加载 Properties 文件，追加到已有 Properties 对象
    private static void loadPropertiesFile(String path, Properties target) {
        try (InputStream is = TranslationUtil.class.getResourceAsStream(path)) {
            if (is != null) {
                target.load(is);
            }
        } catch (Exception ignored) {
        }
    }

    // 将语言设置代码转换为 Locale 对象
    public static Locale getLocale(String lang) {
        if (lang == null || "default".equals(lang)) {
            return Locale.getDefault();
        }
        return switch (lang) {
            case "zh" -> Locale.SIMPLIFIED_CHINESE;
            case "en" -> Locale.ENGLISH;
            case "ja" -> Locale.JAPANESE;
            case "ko" -> Locale.KOREAN;
            case "fr" -> Locale.FRENCH;
            case "de" -> Locale.GERMAN;
            default -> Locale.getDefault();
        };
    }
}
