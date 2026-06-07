package cn.chuanwise.xiaoming.qq;

import cn.chuanwise.xiaoming.bot.XiaoMingBot;

/**
 * QQ 频道小明入口。
 *
 * <pre>
 * 用法：
 *   java -jar bot-qq-all.jar &lt;appid&gt; &lt;token&gt; [secret]
 *
 * 示例：
 *   java -jar bot-qq-all.jar 123456 "your-token"
 *   java -jar bot-qq-all.jar 123456 "your-token" "your-secret"
 * </pre>
 *
 * @author Chuanwise
 * @since 5.0.0
 */
public class QqBotMain {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("用法: java -jar bot-qq-all.jar <appid> <token> [secret]");
            System.err.println();
            System.err.println("  appid  - QQ 开放平台机器人 AppID");
            System.err.println("  token  - QQ 开放平台机器人 Token");
            System.err.println("  secret - QQ 开放平台机器人 Secret（V2 群聊，可选）");
            System.exit(1);
        }

        String appid = args[0];
        String token = args[1];
        String secret = args.length > 2 ? args[2] : null;

        System.out.println();
        System.out.println("╔════════════════════════════════════╗");
        System.out.println("║    XiaoMingBot QQ 频道模式         ║");
        System.out.println("║    v" + XiaoMingBot.VERSION + "                         ║");
        System.out.println("╚════════════════════════════════════╝");
        System.out.println();
        System.out.println("AppID: " + appid);

        QqBotImpl bot = new QqBotImpl(appid, token, secret);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!bot.isDisabled()) {
                System.out.println();
                bot.stop();
            }
        }));

        try {
            bot.start();

            // 保持主线程存活
            System.out.println("QQ 频道小明已启动，按 Ctrl+C 退出");
            Thread.currentThread().join();

        } catch (InterruptedException e) {
            System.out.println("正在退出……");
        } catch (Exception e) {
            System.err.println("QQ 频道小明运行异常: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
