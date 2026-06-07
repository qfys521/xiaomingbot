package cn.chuanwise.xiaoming.compat;

import cn.chuanwise.xiaoming.plugin.PluginHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 插件描述符验证器。
 * <p>
 * 旧版插件使用 xiaoming.json 或 plugin.json 描述插件元数据。
 * 本验证器检查旧版描述符是否与新版多模块结构兼容，
 * 并提供迁移建议。
 * </p>
 *
 * @author Chuanwise
 * @since 4.9
 */
public final class PluginDescriptorValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger("XiaoMingCompat");

    private PluginDescriptorValidator() {}

    /**
     * 验证结果
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> warnings;
        private final List<String> errors;
        private final boolean isLegacy;

        public ValidationResult(boolean valid, boolean isLegacy) {
            this.valid = valid;
            this.isLegacy = isLegacy;
            this.warnings = new ArrayList<>();
            this.errors = new ArrayList<>();
        }

        public boolean isValid() { return valid && errors.isEmpty(); }
        public boolean isLegacy() { return isLegacy; }
        public List<String> getWarnings() { return Collections.unmodifiableList(warnings); }
        public List<String> getErrors() { return Collections.unmodifiableList(errors); }

        void addWarning(String warning) { warnings.add(warning); }
        void addError(String error) { errors.add(error); }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("插件验证结果: ").append(isValid() ? "✓ 通过" : "✗ 失败");
            if (isLegacy) sb.append(" [旧版插件]");
            if (!warnings.isEmpty()) {
                sb.append("\n  警告 (").append(warnings.size()).append("):");
                warnings.forEach(w -> sb.append("\n    ⚠ ").append(w));
            }
            if (!errors.isEmpty()) {
                sb.append("\n  错误 (").append(errors.size()).append("):");
                errors.forEach(e -> sb.append("\n    ✗ ").append(e));
            }
            return sb.toString();
        }
    }

    /**
     * 验证插件描述符。
     *
     * @param handler 插件信息
     * @return 验证结果
     */
    public static ValidationResult validate(PluginHandler handler) {
        if (handler == null) {
            ValidationResult result = new ValidationResult(false, false);
            result.addError("插件信息为空");
            return result;
        }

        boolean isLegacy = CompatLayer.isLegacyPlugin(handler);
        ValidationResult result = new ValidationResult(true, isLegacy);

        // 1. 检查插件名
        String name = handler.getName();
        if (name == null || name.isEmpty()) {
            result.addError("插件名缺失");
        }

        // 2. 检查主类名
        String mainClass = handler.getMainClassName();
        if (mainClass == null || mainClass.isEmpty()) {
            result.addError("插件主类名缺失 (main)");
        }

        // 3. 对旧版插件进行额外检查
        if (isLegacy) {
            validateLegacyPlugin(handler, result);
        }

        // 4. 检查依赖声明
        validateDependencies(handler, result);

        return result;
    }

    private static void validateLegacyPlugin(PluginHandler handler, ValidationResult result) {
        result.addWarning("检测到旧版插件（单模块时代编译）");

        // 检查版本号格式
        String version = handler.getVersion();
        if (version != null && version.equals(PluginHandler.DEFAULT_VERSION)) {
            result.addWarning("插件未声明版本号，建议添加 version 字段");
        }

        // 旧版插件可能需要额外的兼容处理
        result.addWarning("旧版插件将通过兼容层加载，部分新特性可能不可用");
    }

    private static void validateDependencies(PluginHandler handler, ValidationResult result) {
        String[] depends = handler.getDepends();
        if (depends != null && depends.length > 0) {
            // 依赖声明存在，不检查运行时是否可解析（运行时检查）
            LOGGER.debug("插件 '{}' 声明了 {} 个硬依赖", handler.getName(), depends.length);
        }

        String[] softDepends = handler.getSoftDepends();
        if (softDepends != null && softDepends.length > 0) {
            LOGGER.debug("插件 '{}' 声明了 {} 个软依赖", handler.getName(), softDepends.length);
        }
    }

    /**
     * 批量验证多个插件
     */
    public static Map<String, ValidationResult> validateAll(Collection<PluginHandler> handlers) {
        Map<String, ValidationResult> results = new LinkedHashMap<>();
        for (PluginHandler handler : handlers) {
            results.put(handler.getName(), validate(handler));
        }
        return results;
    }
}
