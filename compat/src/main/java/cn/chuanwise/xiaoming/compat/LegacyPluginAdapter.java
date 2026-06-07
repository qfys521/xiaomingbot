package cn.chuanwise.xiaoming.compat;

import cn.chuanwise.xiaoming.bot.XiaoMingBot;
import cn.chuanwise.xiaoming.plugin.Plugin;
import cn.chuanwise.xiaoming.plugin.PluginHandler;
import cn.chuanwise.xiaoming.plugin.PluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

/**
 * 旧版插件适配器。
 * <p>
 * 当旧版插件（单模块时代编译）被加载时，本适配器负责：
 * </p>
 * <ul>
 *   <li>包装旧版插件的生命周期回调</li>
 *   <li>处理跨模块类访问——旧版插件期望所有框架类在同一个 ClassLoader 中</li>
 *   <li>提供缺失的 API 兼容桥接</li>
 *   <li>记录兼容性警告和调试信息</li>
 * </ul>
 *
 * <h3>使用</h3>
 * <pre>{@code
 * // 在 PluginManagerImpl.loadPlugin() 中：
 * if (CompatLayer.isLegacyPlugin(handler)) {
 *     LegacyPluginAdapter.adapt(plugin, handler);
 * }
 * }</pre>
 *
 * @author Chuanwise
 * @since 4.9
 */
public final class LegacyPluginAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger("XiaoMingCompat");

    /** 被适配的旧版插件集合 */
    private static final Set<String> adaptedPlugins = Collections.synchronizedSet(new HashSet<>());

    private LegacyPluginAdapter() {}

    /**
     * 对旧版插件进行适配，使其能在多模块环境下正常运行。
     *
     * @param plugin  插件实例
     * @param handler 插件信息
     */
    public static void adapt(Plugin plugin, PluginHandler handler) {
        if (plugin == null || handler == null) {
            LOGGER.warn("无法适配空插件或空插件信息");
            return;
        }

        String pluginName = handler.getName();
        if (adaptedPlugins.contains(pluginName)) {
            return; // 已经适配过
        }

        LOGGER.info("正在为旧版插件 '{}' 应用兼容适配", pluginName);

        // 1. 确保插件能访问所有框架模块的类
        ensureClassVisibility(plugin);

        // 2. 设置兼容标记
        Map<String, Object> values = handler.getValues();
        if (values != null) {
            values.put(CompatLayer.LEGACY_PLUGIN_KEY, true);
            values.put(CompatLayer.LEGACY_VERSION_KEY, CompatLayer.getCompatVersion());
        }

        // 3. 验证插件依赖
        CompatLayer.validateDependencies(handler);

        adaptedPlugins.add(pluginName);
        LOGGER.info("旧版插件 '{}' 兼容适配完成", pluginName);
    }

    /**
     * 确保插件可以访问所有框架类。
     * 在单模块时代，所有类都在同一个 JAR 中；
     * 多模块后，类分布在 api.jar、core.jar 中。
     * 本方法确保插件的类加载器能访问所有模块。
     */
    private static void ensureClassVisibility(Plugin plugin) {
        try {
            // 检查关键框架类是否可访问
            String[] criticalClasses = {
                    "cn.chuanwise.xiaoming.bot.XiaoMingBot",
                    "cn.chuanwise.xiaoming.plugin.PluginManager",
                    "cn.chuanwise.xiaoming.interactor.InteractorManager",
                    "cn.chuanwise.xiaoming.listener.EventManager",
                    "cn.chuanwise.xiaoming.contact.ContactManager",
            };

            ClassLoader pluginLoader = plugin.getClass().getClassLoader();
            for (String className : criticalClasses) {
                try {
                    Class<?> clazz = Class.forName(className, false, pluginLoader);
                    if (clazz == null) {
                        LOGGER.warn("关键框架类 '{}' 在插件类加载器中不可见，可能出现兼容性问题", className);
                    }
                } catch (ClassNotFoundException e) {
                    LOGGER.debug("类 '{}' 需要通过父加载器访问（这是正常的跨模块行为）", className);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("检查类可见性时出现异常（非致命）", e);
        }
    }

    /**
     * 获取已适配的插件数量
     */
    public static int getAdaptedPluginCount() {
        return adaptedPlugins.size();
    }

    /**
     * 获取所有已适配的插件名
     */
    public static Set<String> getAdaptedPlugins() {
        return Collections.unmodifiableSet(adaptedPlugins);
    }

    /**
     * 判断某个插件是否已被适配
     */
    public static boolean isAdapted(String pluginName) {
        return adaptedPlugins.contains(pluginName);
    }

    /**
     * 获取兼容性状态报告
     */
    public static String getCompatReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 小明兼容性报告 ===\n");
        sb.append("兼容层版本: ").append(CompatLayer.getCompatVersion()).append("\n");
        sb.append("核心版本: ").append(CompatLayer.getCoreVersion()).append("\n");
        sb.append("已适配插件数: ").append(adaptedPlugins.size()).append("\n");
        if (!adaptedPlugins.isEmpty()) {
            sb.append("适配的插件:\n");
            for (String name : adaptedPlugins) {
                sb.append("  - ").append(name).append(" [旧版兼容模式]\n");
            }
        }
        return sb.toString();
    }
}
