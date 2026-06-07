package cn.chuanwise.xiaoming.console;

import cn.chuanwise.xiaoming.bot.XiaoMingBot;
import cn.chuanwise.xiaoming.contact.contact.ConsoleContact;
import cn.chuanwise.xiaoming.contact.message.Message;
import net.mamoe.mirai.contact.Friend;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;
import net.mamoe.mirai.utils.ExternalResource;
import net.mamoe.mirai.message.data.Image;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * 控制台联系人实现（无 Mirai 依赖）。
 */
public class ConsoleContactImpl implements ConsoleContact {

    private final XiaoMingBot xiaoMingBot;
    private final long code;

    public ConsoleContactImpl(XiaoMingBot xiaoMingBot, long code) {
        this.xiaoMingBot = xiaoMingBot;
        this.code = code;
    }

    @Override public XiaoMingBot getXiaoMingBot() { return xiaoMingBot; }
    @Override public void setXiaoMingBot(XiaoMingBot bot) {}

    @Override public long getCode() { return code; }
    @Override public String getName() { return "Console"; }
    @Override public String getAlias() { return "Console"; }
    @Override public String getAliasAndCode() { return "Console(" + code + ")"; }
    @Override public String getAvatarUrl() { return ""; }

    @Override
    public Friend getMiraiContact() {
        return null; // 控制台模式无 Mirai
    }

    @Override
    public Optional<Message> sendMessage(String message, Object... arguments) {
        String formatted = getXiaoMingBot().getLanguageManager()
                .formatAdditional(message, x -> null, arguments);
        System.out.println("[小明] " + formatted);
        return Optional.of(new ConsoleMessage(xiaoMingBot, formatted, System.currentTimeMillis()));
    }

    @Override
    public Optional<Message> sendMessage(MessageChain messages) {
        String text = messages != null ? messages.contentToString() : "";
        System.out.println("[小明] " + text);
        return Optional.of(new ConsoleMessage(xiaoMingBot, text, System.currentTimeMillis()));
    }

    @Override
    public Optional<Message> nextMessage(long timeout, Predicate<Message> filter) {
        return Optional.empty();
    }

    @Override
    public Image uploadImage(ExternalResource resource) {
        return null;
    }

    @Override public Set<String> getTags() { return Set.of("console"); }
    @Override public Set<String> getOriginalTags() { return Set.of("console"); }
    @Override public void flush() {}
    @Override public boolean addTag(String tag) { return false; }
    @Override public boolean hasTag(String tag) { return "console".equals(tag); }
    @Override public boolean removeTag(String tag) { return false; }
}
