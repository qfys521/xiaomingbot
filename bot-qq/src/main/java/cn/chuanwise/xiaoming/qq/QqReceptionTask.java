package cn.chuanwise.xiaoming.qq;

import cn.chuanwise.xiaoming.contact.message.Message;
import cn.chuanwise.xiaoming.recept.ReceptionTaskImpl;
import cn.chuanwise.xiaoming.user.XiaoMingUser;

/**
 * QQ 频道的接待任务。
 * 暴露 ReceptionTaskImpl 的 protected 构造函数。
 */
public class QqReceptionTask extends ReceptionTaskImpl<XiaoMingUser<?>> {
    public QqReceptionTask(XiaoMingUser<?> user, Message message) {
        super(user, message);
    }
}
