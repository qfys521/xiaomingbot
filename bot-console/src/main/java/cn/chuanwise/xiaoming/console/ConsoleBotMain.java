package cn.chuanwise.xiaoming.console;

import cn.chuanwise.xiaoming.bot.XiaoMingBot;

/**
 * 控制台小明入口。
 *
 * <pre>
 * 用法：
 *   java -jar bot-console-all.jar [qq号]
 *
 * 示例：
 *   java -jar bot-console-all.jar           # 默认 QQ 号 10000
 *   java -jar bot-console-all.jar 12345     # 指定 QQ 号 12345
 * </pre>
 *
 * @author Chuanwise
 * @since 5.0.0
 */
public class ConsoleBotMain {

    public static void main(String[] args) {
        long consoleQq = 10000L;

        if (args.length > 0) {
            try {
                consoleQq = Long.parseLong(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("无效的 QQ 号: " + args[0] + "，使用默认值 10000");
            }
        }

        System.out.println();
        System.out.println("╔════════════════════════════════════╗");
        System.out.println("║      XiaoMingBot 控制台模式        ║");
        System.out.println("║      v" + XiaoMingBot.VERSION + "                         ║");
        System.out.println("╚════════════════════════════════════╝");
        System.out.println();

        ConsoleBotImpl bot = new ConsoleBotImpl(consoleQq);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!bot.isDisabled()) {
                bot.stop();
            }
        }));

        try {
            bot.start();
            bot.enterRepl();
        } catch (Exception e) {
            System.err.println("控制台小明运行异常: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
