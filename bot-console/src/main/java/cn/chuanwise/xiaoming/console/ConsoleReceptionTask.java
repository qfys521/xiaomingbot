package cn.chuanwise.xiaoming.console;

import cn.chuanwise.xiaoming.contact.message.Message;
import cn.chuanwise.xiaoming.recept.ReceptionTaskImpl;
import cn.chuanwise.xiaoming.user.XiaoMingUser;

/**
 * 控制台模式的接待任务。
 * 仅用于暴露 protected 构造函数给 ConsoleBotImpl 使用。
 */
public class ConsoleReceptionTask extends ReceptionTaskImpl<XiaoMingUser<?>> {
    public ConsoleReceptionTask(XiaoMingUser<?> user, Message message) {
        super(user, message);
    }
}
