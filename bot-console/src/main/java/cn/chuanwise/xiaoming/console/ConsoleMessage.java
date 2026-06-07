package cn.chuanwise.xiaoming.console;

import cn.chuanwise.xiaoming.bot.XiaoMingBot;
import cn.chuanwise.xiaoming.contact.message.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import net.mamoe.mirai.message.data.MessageSource;
import net.mamoe.mirai.message.data.PlainText;

/**
 * 控制台消息实现。
 */
public class ConsoleMessage implements Message {

    private final XiaoMingBot xiaoMingBot;
    private final String content;
    private final long time;
    private MessageChain messageChain;
    private MessageChain originalMessageChain;
    private MessageSource messageSource;

    public ConsoleMessage(XiaoMingBot xiaoMingBot, String content, long time) {
        this.xiaoMingBot = xiaoMingBot;
        this.content = content;
        this.time = time;
        PlainText pt = new PlainText(content);
        this.messageChain = new MessageChainBuilder().append(pt).build();
        this.originalMessageChain = this.messageChain;
    }

    @Override public XiaoMingBot getXiaoMingBot() { return xiaoMingBot; }
    @Override public void setXiaoMingBot(XiaoMingBot bot) {}

    @Override public MessageChain getMessageChain() { return messageChain; }
    @Override public void setMessageChain(MessageChain mc) { this.messageChain = mc; }

    @Override public MessageChain getOriginalMessageChain() { return originalMessageChain; }
    @Override public void setOriginalMessageChain(MessageChain mc) { this.originalMessageChain = mc; }

    @Override public long getTime() { return time; }

    @Override public String serialize() { return content; }
    @Override public String serializeOriginalMessage() { return content; }

    @Override public int[] getInternalMessageCode() {
        return messageChain != null ? new int[0] : new int[0];
    }

    @Override public int[] getMessageCode() {
        return messageChain != null ? new int[0] : new int[0];
    }

    public MessageSource getMessageSource() { return messageSource; }
    public void setMessageSource(MessageSource ms) { this.messageSource = ms; }

    @Override public String summary() {
        return content.length() > 50 ? content.substring(0, 47) + "..." : content;
    }
}
