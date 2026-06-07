package cn.chuanwise.xiaoming.qq;

import cn.chuanwise.xiaoming.bot.XiaoMingBot;
import cn.chuanwise.xiaoming.contact.contact.XiaoMingContact;
import cn.chuanwise.xiaoming.contact.message.Message;
import cn.chuanwise.xiaoming.interactor.context.InteractorContext;
import cn.chuanwise.xiaoming.property.PropertyType;
import cn.chuanwise.xiaoming.recept.ReceptionTask;
import cn.chuanwise.xiaoming.recept.Receptionist;
import cn.chuanwise.xiaoming.user.XiaoMingUser;
import io.github.kloping.qqbot.api.message.MessageReceiveEvent;
import net.mamoe.mirai.message.data.MessageChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * QQ 频道用户实现。
 */
public class QqUser implements XiaoMingUser<XiaoMingContact<?>> {

    private final QqBotImpl bot;
    private final String userId;
    private final MessageReceiveEvent lastEvent;
    private final boolean isDirect;
    private final Map<PropertyType<?>, Object> properties = new HashMap<>();
    private final Logger logger;
    private Receptionist receptionist;
    private ReceptionTask<XiaoMingUser<XiaoMingContact<?>>> receptionTask;
    private InteractorContext interactorContext;

    public QqUser(QqBotImpl bot, String userId, MessageReceiveEvent lastEvent, boolean isDirect) {
        this.bot = bot;
        this.userId = userId;
        this.lastEvent = lastEvent;
        this.isDirect = isDirect;
        this.logger = LoggerFactory.getLogger("QqUser-" + userId);
    }

    @Override public XiaoMingBot getXiaoMingBot() { return bot; }
    @Override public void setXiaoMingBot(XiaoMingBot b) {}

    @Override public Logger getLogger() { return logger; }

    @Override public long getCode() {
        try { return Long.parseLong(userId); }
        catch (NumberFormatException e) { return 0; }
    }

    @Override public String getCompleteName() { return "QQ用户(" + userId + ")"; }

    @Override public XiaoMingContact<?> getContact() { return null; }

    @Override public Receptionist getReceptionist() { return receptionist; }
    @Override public void setReceptionist(Receptionist r) { this.receptionist = r; }

    @Override public ReceptionTask<XiaoMingUser<XiaoMingContact<?>>> getReceptionTask() { return receptionTask; }
    @Override public void setReceptionTask(ReceptionTask<XiaoMingUser<XiaoMingContact<?>>> t) { this.receptionTask = t; }

    @Override public InteractorContext getInteractorContext() { return interactorContext; }
    @Override public void setInteractorContext(InteractorContext c) { this.interactorContext = c; }

    @Override public Optional<Message> sendMessage(String msg, Object... args) {
        String formatted = bot.getLanguageManager().formatAdditional(msg, x -> null, args);
        try {
            lastEvent.getRawMessage().send(formatted);
        } catch (Exception e) {
            try { lastEvent.send(formatted); } catch (Exception ignored) {}
        }
        return Optional.of(new QqMessage(bot, formatted, System.currentTimeMillis(), lastEvent));
    }

    @Override public Optional<Message> sendMessage(MessageChain mc) {
        String text = mc != null ? mc.contentToString() : "";
        return sendMessage(text);
    }

    @Override public Optional<Message> sendPrivateMessage(String msg, Object... args) {
        return sendMessage(msg, args);
    }

    // sendError from MessageSendable returns M (Optional<Message>), not void
    @Override public Optional<Message> sendError(String miraiCode, Object... contexts) {
        return sendMessage("ヾ(≧へ≦)〃 " + miraiCode, contexts);
    }

    @Override public void nudge() {}

    @Override public boolean onNextMessage(MessageChain messages) {
        return onNextMessage(messages != null ? messages.contentToString() : "");
    }

    // TagMarkable / OriginalTagMarkable
    private final Set<String> tags = new HashSet<>(Set.of("all"));
    @Override public Set<String> getTags() { return Collections.unmodifiableSet(tags); }
    @Override public Set<String> getOriginalTags() { return Set.of("all"); }
    @Override public void flush() {}
    @Override public boolean addTag(String tag) { return tags.add(tag); }
    @Override public boolean hasTag(String tag) { return tags.contains(tag); }
    @Override public boolean removeTag(String tag) { return tags.remove(tag); }

    // PropertyHandler
    @Override public Map<PropertyType, Object> getProperties() { return (Map) properties; }

    @Override public <T> cn.chuanwise.toolkit.container.Container<T> getProperty(PropertyType<T> type) {
        return cn.chuanwise.toolkit.container.Container.of((T) properties.get(type));
    }
    @Override public <T> void setProperty(PropertyType<T> type, T value) { properties.put(type, value); }
    @Override public <T> cn.chuanwise.toolkit.container.Container<T> removeProperty(PropertyType<T> type) {
        return cn.chuanwise.toolkit.container.Container.of((T) properties.remove(type));
    }
    @Override
    public <T> cn.chuanwise.toolkit.container.Container<T> waitProperty(PropertyType<T> type, long timeout) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeout;
        while (System.currentTimeMillis() < deadline) {
            cn.chuanwise.toolkit.container.Container<T> v = getProperty(type);
            if (v.isPresent()) return v;
            Thread.sleep(100);
        }
        return cn.chuanwise.toolkit.container.Container.empty();
    }
}
