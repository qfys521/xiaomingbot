package cn.chuanwise.xiaoming.qq;

import cn.chuanwise.xiaoming.bot.XiaoMingBot;
import cn.chuanwise.xiaoming.contact.message.Message;
import io.github.kloping.qqbot.api.message.MessageReceiveEvent;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import net.mamoe.mirai.message.data.PlainText;

/**
 * QQ 频道消息实现。
 */
public class QqMessage implements Message {

    private final XiaoMingBot xiaoMingBot;
    private final String content;
    private final long time;
    private final MessageReceiveEvent rawEvent;
    private MessageChain messageChain;

    public QqMessage(XiaoMingBot bot, String content, long time, MessageReceiveEvent rawEvent) {
        this.xiaoMingBot = bot;
        this.content = content;
        this.time = time;
        this.rawEvent = rawEvent;
        this.messageChain = new MessageChainBuilder().append(new PlainText(content)).build();
    }

    @Override public XiaoMingBot getXiaoMingBot() { return xiaoMingBot; }
    @Override public void setXiaoMingBot(XiaoMingBot bot) {}

    @Override public MessageChain getMessageChain() { return messageChain; }
    @Override public void setMessageChain(MessageChain mc) { this.messageChain = mc; }

    @Override public MessageChain getOriginalMessageChain() { return messageChain; }
    @Override public void setOriginalMessageChain(MessageChain mc) {}

    @Override public long getTime() { return time; }
    @Override public String serialize() { return content; }
    @Override public String serializeOriginalMessage() { return content; }

    @Override public int[] getInternalMessageCode() { return new int[0]; }
    @Override public int[] getMessageCode() { return new int[0]; }

    @Override public String summary() {
        return content.length() > 50 ? content.substring(0, 47) + "..." : content;
    }

    public MessageReceiveEvent getRawEvent() { return rawEvent; }
}
