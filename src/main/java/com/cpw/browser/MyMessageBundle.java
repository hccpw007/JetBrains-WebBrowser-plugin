// 国际化消息资源工具类
package com.cpw.browser;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.PropertyKey;
import java.util.function.Supplier;

public final class MyMessageBundle {

    // 消息资源文件的基路径
    private static final String BUNDLE = "messages.MyMessageBundle";
    // DynamicBundle 实例，用于按需加载消息
    private static final DynamicBundle instance = new DynamicBundle(MyMessageBundle.class, BUNDLE);

    private MyMessageBundle() {
        // 工具类，禁止实例化
    }

    // 根据键名获取本地化消息
    public static @Nls String message(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        return instance.getMessage(key, params);
    }

    // 根据键名获取延迟加载的本地化消息
    public static Supplier<@Nls String> lazyMessage(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        return instance.getLazyMessage(key, params);
    }
}
