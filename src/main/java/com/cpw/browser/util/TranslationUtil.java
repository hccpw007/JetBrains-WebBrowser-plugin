// 多语言翻译工具类，根据用户设置的语言偏好加载对应 ResourceBundle
package com.cpw.browser.util;

import com.cpw.browser.settings.BrowserSettingsState;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public final class TranslationUtil {

    // 资源包基名
    private static final String BUNDLE_NAME = "messages.MyMessageBundle";

    private TranslationUtil() {
    }

    // 根据当前语言设置获取翻译文本
    public static String getText(String key) {
        ResourceBundle bundle = getBundle();
        try {
            if (bundle.containsKey(key)) {
                return bundle.getString(key);
            }
        } catch (MissingResourceException ignored) {
            // 忽略
        }
        return key;
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

    // 获取当前语言设置对应的 ResourceBundle
    private static ResourceBundle getBundle() {
        String lang = BrowserSettingsState.getInstance().getLanguage();
        Locale locale = getLocale(lang);
        return ResourceBundle.getBundle(BUNDLE_NAME, locale);
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
