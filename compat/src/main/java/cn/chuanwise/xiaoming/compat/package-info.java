/**
 * 小明兼容层模块。
 * <p>
 * 本模块为从小明单模块（4.x 及更早版本）升级到多模块（api + core + bot-mirai）
 * 的过渡提供完整的向后兼容性支持。
 * </p>
 *
 * <h3>核心功能</h3>
 * <ul>
 *   <li>{@link cn.chuanwise.xiaoming.compat.CompatLayer} — 兼容层主入口，初始化和配置</li>
 *   <li>{@link cn.chuanwise.xiaoming.compat.LegacyPluginAdapter} — 旧版插件适配器，包装旧版插件使其在新架构下运行</li>
 *   <li>{@link cn.chuanwise.xiaoming.compat.PluginDescriptorValidator} — 插件描述符验证器，检查旧版 plugin.json 兼容性</li>
 * </ul>
 *
 * <h3>插件开发者迁移指南</h3>
 * <ol>
 *   <li>旧版插件无需任何修改即可在新版小明上加载——兼容层自动处理</li>
 *   <li>新开发插件建议依赖 {@code api} 模块（仅接口），而非 {@code compat}</li>
 *   <li>如果需要访问实现类，可依赖 {@code core} 模块</li>
 * </ol>
 *
 * <h3>依赖关系</h3>
 * <pre>
 *   compat
 *     ├── api (api)      — 接口和注解
 *     └── core (api)     — 实现类
 * </pre>
 *
 * @author Chuanwise
 * @since 4.9
 */
package cn.chuanwise.xiaoming.compat;
