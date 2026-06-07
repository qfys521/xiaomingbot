package cn.chuanwise.xiaoming.qq;

import cn.chuanwise.xiaoming.bot.XiaoMingBot;
import cn.chuanwise.xiaoming.bot.XiaoMingBotImpl;
import cn.chuanwise.xiaoming.contact.message.Message;
import cn.chuanwise.xiaoming.event.MessageEvent;
import cn.chuanwise.xiaoming.exception.XiaoMingRuntimeException;
import cn.chuanwise.xiaoming.user.XiaoMingUser;
import io.github.kloping.qqbot.Starter;
import io.github.kloping.qqbot.api.Intents;
import io.github.kloping.qqbot.api.message.MessageChannelReceiveEvent;
import io.github.kloping.qqbot.api.message.MessageDirectReceiveEvent;
import io.github.kloping.qqbot.api.message.MessageReceiveEvent;
import io.github.kloping.qqbot.entities.Bot;
import io.github.kloping.qqbot.entities.ex.PlainText;
import io.github.kloping.qqbot.impl.ListenerHost;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageChainBuilder;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * QQ 频道机器人实现。
 * 基于 <a href="https://github.com/Kloping/qqpd-bot-java">Kloping/qqpd-bot-java</a>。
 *
 * <pre>
 * 启动方式：
 *   QqBotImpl bot = new QqBotImpl("appid", "token");
 *   bot.start();
 * </pre>
 *
 * @author Chuanwise
 * @since 5.0.0
 */
public class QqBotImpl extends XiaoMingBotImpl {

    private final String appid;
    private final String token;
    private final String secret;
    private Starter starter;

    /**
     * @param appid  QQ 机器人 AppID
     * @param token  QQ 机器人 Token
     */
    public QqBotImpl(String appid, String token) {
        this(appid, token, null);
    }

    /**
     * @param appid  QQ 机器人 AppID
     * @param token  QQ 机器人 Token
     * @param secret QQ 机器人 Secret（V2 群聊需要）
     */
    public QqBotImpl(String appid, String token, String secret) {
        super(createFakeMiraiBot(appid));
        this.appid = appid;
        this.token = token;
        this.secret = secret;
    }

    private static long parseAppid(String appid) {
        try {
            return Long.parseLong(appid);
        } catch (NumberFormatException e) {
            return 10000L;
        }
    }

    @SuppressWarnings("unchecked")
    private static net.mamoe.mirai.Bot createFakeMiraiBot(String appid) {
        final long qq = parseAppid(appid);
        return (net.mamoe.mirai.Bot) Proxy.newProxyInstance(
                net.mamoe.mirai.Bot.class.getClassLoader(),
                new Class<?>[]{net.mamoe.mirai.Bot.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getId":    return qq;
                        case "isOnline": return true;
                        case "getNick":  return "QQ-Bot-" + appid;
                        case "login":    return null;
                        case "close":    return null;
                        default:
                            Class<?> rt = method.getReturnType();
                            if (rt == boolean.class) return false;
                            if (rt == long.class) return 0L;
                            return null;
                    }
                });
    }

    // ==================== 启动 / 关闭 ====================

    @Override
    public void start() {
        setStatus(Status.ENABLING);
        printBanner();

        getLogger().info("正在启动 QQ 频道机器人 (appid=" + appid + ")……");

        // 1. 初始化框架
        initQqBot();

        // 2. 创建并启动 Starter
        starter = new Starter(appid, token, secret);
        starter.getConfig().setCode(Intents.PRIVATE_INTENTS.getCode());
        starter.run();

        // 3. 注册事件桥接
        starter.registerListenerHost(new QqEventListener(this));

        getLogger().info("QQ 频道机器人已连接到 QQ 开放平台");

        setStatus(Status.ENABLED);
        getLogger().info("QQ 频道小明启动完成！");
    }

    @Override
    public synchronized void stop() {
        if (isDisabled()) {
            throw new XiaoMingRuntimeException("can not stop a stopped xiaoming bot");
        }

        getFileSaver().readyToSave(getAccountManager());
        getFileSaver().readyToSave(getGroupInformationManager());
        getFileSaver().readyToSave(getConfiguration());
        getFileSaver().readyToSave(getStatistician());

        setStatus(Status.DISABLING);

        getLogger().info("正在卸载所有插件");
        try {
            getPluginManager().getPluginHandlers().forEach(h -> getPluginManager().unloadPlugin(h));
        } catch (Exception e) {
            getLogger().error("卸载插件时出现异常", e);
        }

        getLogger().info("正在关闭 QQ 连接");
        getStatistician().onClose();
        getScheduler().stopNow();

        setStatus(Status.DISABLED);
        getLogger().info("QQ 频道小明已关闭 (๑•̀ㅂ•́)و✧");
    }

    // ==================== 初始化 ====================

    private void initQqBot() {
        File workingDir = getWorkingDirectory();
        setConfigurationDirectory(new File(workingDir, "configurations"));
        setReportDirectory(new File(workingDir, "reports"));
        setLogDirectory(new File(workingDir, "logs"));
        setPluginDirectory(new File(workingDir, "plugins"));
        setResourceDirectory(new File(workingDir, "resources"));

        for (File dir : new File[]{
                getConfigurationDirectory(), getPluginDirectory(),
                getResourceDirectory(), getReportDirectory(), getLogDirectory()
        }) {
            if (!dir.isDirectory() && !dir.mkdirs()) {
                throw new cn.chuanwise.xiaoming.exception.XiaoMingInitializeException(
                        "无法创建目录：" + dir.getAbsolutePath());
            }
        }

        load();

        // 注册内核模块
        cn.chuanwise.xiaoming.interactor.InteractorManager im = getInteractorManager();
        im.registerInteractors(new cn.chuanwise.xiaoming.interactor.interactors.PluginInteractors(), null);
        im.registerInteractors(new cn.chuanwise.xiaoming.interactor.interactors.ReceptionistInteractors(), null);
        im.registerInteractors(new cn.chuanwise.xiaoming.interactor.interactors.ResourceInteractors(), null);
        im.registerInteractors(new cn.chuanwise.xiaoming.interactor.interactors.AccountInteractors(), null);
        im.registerInteractors(new cn.chuanwise.xiaoming.interactor.interactors.CoreInteractors(), null);
        im.registerInteractors(new cn.chuanwise.xiaoming.interactor.interactors.ConfigurationInteractors(), null);
        im.registerInteractors(new cn.chuanwise.xiaoming.interactor.interactors.GroupInformationInteractors(), null);
        im.registerInteractors(new cn.chuanwise.xiaoming.interactor.interactors.PermissionInteractors(), null);

        getEventManager().registerListeners(getReceptionistManager(), null);
        getEventManager().registerListeners(new cn.chuanwise.xiaoming.listener.CoreListeners(), null);

        try {
            getPluginManager().initialize();
        } catch (Throwable t) {
            getLogger().error("加载插件时出现异常", t);
        }
    }

    private void printBanner() {
        getLogger().warn("\n" +
                " __   __ _                __  __  _               \n" +
                " \\ \\ / /(_)              |  \\/  |(_)              \n" +
                "  \\ V /  _   __ _   ___  | \\  / | _  _ __    __ _ \n" +
                "   > <  | | / _` | / _ \\ | |\\/| || || '_ \\  / _` |\n" +
                "  / . \\ | || (_| || (_) || |  | || || | | || (_| |\n" +
                " /_/ \\_\\|_| \\__,_| \\___/ |_|  |_||_||_| |_| \\__, |\n" +
                "                                             __/ |\n" +
                "                                            |___/ \n" +
                "                                        @" + SPONSOR + "\n" +
                "version: " + XiaoMingBot.VERSION + " (QQ Guild Mode)\n" +
                "github: " + GITHUB + "\n" +
                "appid:  " + appid + "\n");
    }

    // ==================== 事件桥接 ====================

    /**
     * 将 qqpd-bot-java 事件桥接到小明的交互器系统。
     */
    private class QqEventListener extends ListenerHost {

        private final QqBotImpl bot;

        QqEventListener(QqBotImpl bot) {
            this.bot = bot;
        }

        @ListenerHost.EventReceiver
        public void onChannelMessage(MessageChannelReceiveEvent event) {
            handleMessage(event, false);
        }

        @ListenerHost.EventReceiver
        public void onDirectMessage(MessageDirectReceiveEvent event) {
            handleMessage(event, true);
        }

        private void handleMessage(MessageReceiveEvent event, boolean isDirect) {
            try {
                String content = extractContent(event);
                if (content == null || content.isEmpty()) return;

                String userId = "unknown";
                try {
                    Method m = event.getClass().getMethod("getSenderId");
                    userId = String.valueOf(m.invoke(event));
                } catch (Exception ignored) {}

                // 创建小明消息
                QqMessage msg = new QqMessage(bot, content, System.currentTimeMillis(), event);

                // 获取或创建用户
                QqUser user = new QqUser(bot, userId, event, isDirect);

                // 通知联系人管理器
                MessageEvent msgEvent = new MessageEvent(user, msg);
                getContactManager().onNextMessageEvent(msgEvent);

                // 触发交互器链
                getStatistician().increaseCallNumber();
                getScheduler().run(new QqReceptionTask(user, msg));

            } catch (Exception e) {
                getLogger().error("处理 QQ 消息时出错", e);
            }
        }

        private String extractContent(MessageReceiveEvent event) {
            try {
                // 尝试获取消息文本
                Method m = event.getClass().getMethod("getMessage");
                Object msgObj = m.invoke(event);
                if (msgObj != null) {
                    // 如果返回的是 MessageChain
                    if (msgObj instanceof io.github.kloping.qqbot.entities.ex.msg.MessageChain) {
                        io.github.kloping.qqbot.entities.ex.msg.MessageChain chain =
                                (io.github.kloping.qqbot.entities.ex.msg.MessageChain) msgObj;
                        StringBuilder sb = new StringBuilder();
                        chain.forEach(e -> {
                            if (e instanceof io.github.kloping.qqbot.entities.ex.PlainText) {
                                sb.append(e.toString());
                            }
                        });
                        return sb.toString();
                    }
                    // 尝试 contentToString
                    Method cm = msgObj.getClass().getMethod("contentToString");
                    return (String) cm.invoke(msgObj);
                }
            } catch (Exception e) {
                // fallback: try toString
                try {
                    return event.getRawMessage().toString();
                } catch (Exception ignored) {}
            }
            return "";
        }

        @Override
        public boolean handleException(Throwable e) {
            getLogger().error("QQ 事件处理异常", e);
            return false;
        }
    }
}
