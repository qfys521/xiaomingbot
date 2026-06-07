package cn.chuanwise.xiaoming.console;

import cn.chuanwise.xiaoming.bot.XiaoMingBot;
import cn.chuanwise.xiaoming.bot.XiaoMingBotImpl;
import cn.chuanwise.xiaoming.contact.contact.ConsoleContact;
import cn.chuanwise.xiaoming.contact.message.Message;
import cn.chuanwise.xiaoming.event.MessageEvent;
import cn.chuanwise.xiaoming.event.XiaoMingStopEvent;
import cn.chuanwise.xiaoming.exception.XiaoMingRuntimeException;
import cn.chuanwise.xiaoming.recept.Receptionist;
import cn.chuanwise.xiaoming.user.ConsoleXiaoMingUser;
import cn.chuanwise.xiaoming.user.ConsoleXiaoMingUserImpl;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.Friend;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.NormalMember;
import net.mamoe.mirai.event.EventChannel;
import net.mamoe.mirai.event.events.BotEvent;
import net.mamoe.mirai.utils.BotConfiguration;
import net.mamoe.mirai.utils.MiraiLogger;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

/**
 * 控制台小明机器人。
 * 不依赖任何 IM 平台，直接在终端中运行。
 *
 * @author Chuanwise
 * @since 5.0.0
 */
public class ConsoleBotImpl extends XiaoMingBotImpl {

    private Scanner scanner;
    private ConsoleXiaoMingUser consoleUser;
    private volatile boolean running = false;

    /**
     * @param consoleQq 机器人标识 QQ 号
     */
    public ConsoleBotImpl(long consoleQq) {
        super(createFakeBot(consoleQq));
    }

    /**
     * 使用 Java 动态代理创建一个假 Mirai Bot，
     * 满足 XiaoMingBotImpl 的构造函数要求。
     */
    @SuppressWarnings("unchecked")
    private static Bot createFakeBot(long qq) {
        return (Bot) Proxy.newProxyInstance(
                Bot.class.getClassLoader(),
                new Class<?>[]{Bot.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        switch (method.getName()) {
                            case "getId":       return qq;
                            case "isOnline":    return true;
                            case "getNick":     return "ConsoleBot";
                            case "getFriends":  return Collections.emptyList();
                            case "getGroups":   return Collections.emptyList();
                            case "getFriend":   return null;
                            case "getGroup":    return null;
                            case "getEventChannel":
                                // 返回一个假的 EventChannel
                                return Proxy.newProxyInstance(
                                        EventChannel.class.getClassLoader(),
                                        new Class<?>[]{EventChannel.class},
                                        (p, m, a) -> null);
                            case "getAsFriend": return null;
                            case "login":       return null; // 不登录
                            case "close":       return null; // 不关闭
                            case "getLogger":   return null;
                            case "getBotConfiguration": return null;
                            case "toString":    return "ConsoleBot(" + qq + ")";
                            case "hashCode":    return Long.hashCode(qq);
                            case "equals":      return args != null && args.length > 0 &&
                                                        args[0] instanceof Bot &&
                                                        ((Bot) args[0]).getId() == qq;
                            default:
                                // 对其他方法返回 null 或默认值
                                Class<?> returnType = method.getReturnType();
                                if (returnType == boolean.class) return false;
                                if (returnType == int.class)    return 0;
                                if (returnType == long.class)   return 0L;
                                if (returnType == List.class)   return Collections.emptyList();
                                return null;
                        }
                    }
                });
    }

    // ==================== 启动与关闭 ====================

    @Override
    public void start() {
        setStatus(Status.ENABLING);
        printConsoleBanner();

        getLogger().info("正在以控制台模式启动小明机器人……");
        getLogger().info("控制台模式：无需 QQ 登录，直接在终端中交互");

        initConsole();

        setStatus(Status.ENABLED);
        getLogger().info("控制台小明启动完成！");
        System.out.println("输入 'exit' 退出，输入 'help' 查看帮助");
    }

    @Override
    public synchronized void stop() {
        if (isDisabled()) {
            throw new XiaoMingRuntimeException("can not stop a stopped xiaoming bot");
        }

        running = false;

        XiaoMingStopEvent event = new XiaoMingStopEvent();
        getEventManager().callEvent(event);
        if (event.isCancelled()) return;

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

        getLogger().info("正在关闭线程池");
        getStatistician().onClose();
        getScheduler().stopNow();

        if (scanner != null) scanner.close();

        setStatus(Status.DISABLED);
        getLogger().info("控制台小明已关闭 (๑•̀ㅂ•́)و✧");
    }

    // ==================== REPL ====================

    public void enterRepl() {
        running = true;
        scanner = new Scanner(System.in, StandardCharsets.UTF_8);

        System.out.println();
        System.out.println("=== 控制台小明 REPL ===");
        System.out.print("> ");

        while (running && scanner.hasNextLine()) {
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) { System.out.print("> "); continue; }

            switch (input.toLowerCase()) {
                case "exit":
                case "quit":
                    System.out.println("正在关闭……");
                    running = false;
                    break;
                case "help":  showHelp();  break;
                case "plugins": showPlugins(); break;
                case "status": showStatus(); break;
                default:
                    handleInput(input);
                    break;
            }
            if (running) System.out.print("> ");
        }
        stop();
    }

    private void handleInput(String content) {
        try {
            if (consoleUser == null) {
                System.out.println("[系统] 控制台用户未就绪");
                return;
            }
            ConsoleMessage msg = new ConsoleMessage(this, content, System.currentTimeMillis());
            MessageEvent event = new MessageEvent(consoleUser, msg);
            event.setXiaoMingBot(this);
            getContactManager().onNextMessageEvent(event);
            consoleUser.onNextMessage(msg);
        } catch (Exception e) {
            System.err.println("[错误] " + e.getMessage());
        }
    }

    // ==================== 初始化 ====================

    private void initConsole() {
        // 创建必要目录（原 XiaoMingBotImpl.makeDirectories()）
        File workingDir = getWorkingDirectory();
        setConfigurationDirectory(new File(workingDir, "configurations"));
        setReportDirectory(new File(workingDir, "reports"));
        setLogDirectory(new File(workingDir, "logs"));
        for (File dir : new File[]{
                getConfigurationDirectory(),
                new File(workingDir, "plugins"),
                new File(workingDir, "resources"),
                getReportDirectory(),
                getLogDirectory()
        }) {
            if (!dir.isDirectory() && !dir.mkdirs()) {
                throw new cn.chuanwise.xiaoming.exception.XiaoMingInitializeException(
                        "无法创建目录：" + dir.getAbsolutePath());
            }
        }

        load();

        // 设置控制台用户
        ConsoleContact contact = new ConsoleContactImpl(this, getCode());
        consoleUser = new ConsoleXiaoMingUserImpl(contact);
        consoleUser.setXiaoMingBot(this);
        setConsoleXiaoMingUser(consoleUser);

        if (getReceptionistManager() != null) {
            Receptionist r = getReceptionistManager().getReceptionist(getCode());
            if (r != null) consoleUser.setReceptionist(r);
        }

        // 注册内核交互器和监听器（原 registerCoreModules()）
        cn.chuanwise.xiaoming.interactor.InteractorManager im = getInteractorManager();
        im.registerInteractors(new cn.chuanwise.xiaoming.interactor.interactors.PluginInteractors(), null);
        im.registerInteractors(new cn.chuanwise.xiaoming.interactor.interactors.ReceptionistInteractors(), null);
        im.registerInteractors(new cn.chuanwise.xiaoming.interactor.interactors.ResourceInteractors(), null);
        im.registerInteractors(new cn.chuanwise.xiaoming.interactor.interactors.AccountInteractors(), null);
        im.registerInteractors(new cn.chuanwise.xiaoming.interactor.interactors.CoreInteractors(), null);
        im.registerInteractors(new cn.chuanwise.xiaoming.interactor.interactors.ConfigurationInteractors(), null);
        im.registerInteractors(new cn.chuanwise.xiaoming.interactor.interactors.GroupInformationInteractors(), null);
        im.registerInteractors(new cn.chuanwise.xiaoming.interactor.interactors.PermissionInteractors(), null);

        cn.chuanwise.xiaoming.listener.EventManager em = getEventManager();
        em.registerListeners(getReceptionistManager(), null);
        em.registerListeners(new cn.chuanwise.xiaoming.listener.CoreListeners(), null);

        try {
            getPluginManager().initialize();
        } catch (Throwable t) {
            getLogger().error("加载插件时出现异常", t);
        }
    }

    // ==================== 显示 ====================

    private void printConsoleBanner() {
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
                "version: " + XiaoMingBot.VERSION + " (Console Mode)\n" +
                "github: " + GITHUB + "\n");
    }

    private void showHelp() {
        System.out.println();
        System.out.println("┌──────────────────────────────────────────┐");
        System.out.println("│       控制台小明 v" + XiaoMingBot.VERSION + "                    │");
        System.out.println("├──────────────────────────────────────────┤");
        System.out.println("│  输入文本  → 与小明对话                  │");
        System.out.println("│  help      → 显示帮助                    │");
        System.out.println("│  plugins   → 列出插件                    │");
        System.out.println("│  status    → 运行状态                    │");
        System.out.println("│  exit/quit → 退出                        │");
        System.out.println("└──────────────────────────────────────────┘");
        System.out.println();
    }

    private void showPlugins() {
        System.out.println();
        var handlers = getPluginManager().getPluginHandlers();
        if (handlers.isEmpty()) {
            System.out.println("  (无已加载插件)");
        } else {
            handlers.forEach(h -> System.out.printf("  %-20s %s  (%s)%n",
                    h.getName(),
                    h.isEnabled() ? "✓" : "✗",
                    h.getVersion()));
        }
        System.out.println();
    }

    private void showStatus() {
        System.out.println();
        System.out.println("  小明版本: " + XiaoMingBot.VERSION);
        System.out.println("  运行模式: Console");
        System.out.println("  机器人 ID: " + getCodeString());
        System.out.println("  插件数量: " + getPluginManager().getPluginHandlers().size());
        System.out.println();
    }
}
