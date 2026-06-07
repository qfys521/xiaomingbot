package cn.chuanwise.xiaoming.compat;

import cn.chuanwise.xiaoming.bot.XiaoMingBot;
import cn.chuanwise.xiaoming.classloader.XiaoMingClassLoader;
import cn.chuanwise.xiaoming.plugin.Plugin;
import cn.chuanwise.xiaoming.plugin.PluginHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.*;

/**
 * 兼容层主入口。
 * <p>
 * 小明从单模块升级为多模块（api + core + bot-mirai）后，
 * 旧版插件通过本兼容层可以无缝加载运行，无需修改插件代码。
 * </p>
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 * // 在 XiaoMingBot 启动时初始化兼容层
 * CompatLayer.initialize(xiaoMingBot);
 *
 * // 加载旧版插件时使用兼容类加载器
 * ClassLoader compatLoader = CompatLayer.createCompatClassLoader(pluginFile);
 * }</pre>
 *
 * @author Chuanwise
 * @since 4.9
 */
public final class CompatLayer {
    private static final Logger LOGGER = LoggerFactory.getLogger("XiaoMingCompat");
    private static volatile boolean initialized = false;
    private static XiaoMingBot xiaoMingBot;

    /** 旧版插件标记：plugin.json 中如果有此字段，表示是旧版单模块编译的插件 */
    public static final String LEGACY_PLUGIN_KEY = "xiaoming-legacy";
    public static final String LEGACY_VERSION_KEY = "xiaoming-legacy-version";

    /** 兼容层版本 */
    public static final String COMPAT_VERSION = "1.0.0";

    /** 已知兼容的旧版小明版本列表 */
    private static final Set<String> KNOWN_COMPATIBLE_VERSIONS = new HashSet<>(Arrays.asList(
            "4.9.20251228-all-last",
            "4.8",
            "4.7",
            "4.6",
            "3.x"
    ));

    private CompatLayer() {}

    /**
     * 初始化兼容层，应在 XiaoMingBot 启动后调用。
     */
    public static void initialize(XiaoMingBot bot) {
        if (initialized) {
            LOGGER.warn("兼容层已经初始化过，跳过重复初始化");
            return;
        }
        xiaoMingBot = bot;
        initialized = true;
        LOGGER.info("小明兼容层 v{} 已初始化", COMPAT_VERSION);
        LOGGER.info("当前核心版本: {}", XiaoMingBot.VERSION);
        LOGGER.info("兼容层会确保旧版插件（{} 之前版本编译）可以正常加载", XiaoMingBot.VERSION);
    }

    /**
     * 检查兼容层是否已初始化
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * 获取当前核心版本
     */
    public static String getCoreVersion() {
        return XiaoMingBot.VERSION;
    }

    /**
     * 获取兼容层版本
     */
    public static String getCompatVersion() {
        return COMPAT_VERSION;
    }

    /**
     * 判断插件是否为旧版插件（单模块时代编译）
     */
    public static boolean isLegacyPlugin(PluginHandler handler) {
        if (handler == null || handler.getValues() == null) {
            return false;
        }
        Map<String, Object> values = handler.getValues();
        return values.containsKey(LEGACY_PLUGIN_KEY)
                || values.containsKey(LEGACY_VERSION_KEY);
    }

    /**
     * 判断给定的插件版本是否已知兼容
     */
    public static boolean isKnownCompatibleVersion(String version) {
        if (version == null) return true; // 未声明版本的插件，假定兼容
        return KNOWN_COMPATIBLE_VERSIONS.stream()
                .anyMatch(v -> version.startsWith(v.replace("x", ""))
                        || version.equals(v));
    }

    /**
     * 为旧版插件创建增强的类加载器。
     * 该加载器可以正确解析跨模块（api/core/bot-mirai）的类引用。
     */
    public static ClassLoader createCompatClassLoader(File pluginFile) {
        if (!initialized) {
            throw new IllegalStateException("兼容层尚未初始化，请先调用 CompatLayer.initialize()");
        }

        XiaoMingClassLoader baseLoader = xiaoMingBot.getXiaoMingClassLoader();
        try {
            baseLoader.addURL(pluginFile.toURI().toURL());
        } catch (Exception e) {
            LOGGER.error("无法将插件文件添加到类加载器: " + pluginFile.getAbsolutePath(), e);
        }
        return baseLoader;
    }

    /**
     * 获取所有模块的类路径 URL（用于类加载器扩展）
     */
    public static List<URL> getModuleClassPathUrls() {
        List<URL> urls = new ArrayList<>();
        // 当前类加载器中已有的 URL 都会被包含
        ClassLoader currentLoader = CompatLayer.class.getClassLoader();
        if (currentLoader instanceof java.net.URLClassLoader) {
            urls.addAll(Arrays.asList(((java.net.URLClassLoader) currentLoader).getURLs()));
        }
        return urls;
    }

    /**
     * 记录插件加载信息（用于调试兼容性问题）
     */
    public static void logPluginLoad(Plugin plugin) {
        if (plugin == null) return;
        PluginHandler handler = plugin.getHandler();
        boolean isLegacy = isLegacyPlugin(handler);
        LOGGER.info("加载插件: {} v{} {}",
                plugin.getName(),
                plugin.getVersion(),
                isLegacy ? "[旧版兼容模式]" : "[新版原生模式]");
    }

    /**
     * 验证插件的依赖是否都能在当前多模块环境中找到
     */
    public static boolean validateDependencies(PluginHandler handler) {
        if (handler == null) return false;
        String[] depends = handler.getDepends();
        if (depends == null || depends.length == 0) return true;

        for (String depend : depends) {
            if (!xiaoMingBot.getPluginManager().isExists(depend)) {
                LOGGER.warn("插件 '{}' 依赖的 '{}' 未找到", handler.getName(), depend);
            }
        }
        return true;
    }
}
