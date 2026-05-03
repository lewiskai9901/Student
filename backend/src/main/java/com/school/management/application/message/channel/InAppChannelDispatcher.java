package com.school.management.application.message.channel;

import com.school.management.domain.message.model.MsgNotification;
import org.springframework.stereotype.Component;

/**
 * 站内消息通道 — 落库即送达, 用户在消息中心列表里看到.
 *
 * MessageDispatcher 已经把 notification 落库. 这里只是确认 sendStatus=SENT 即可.
 */
@Component
public class InAppChannelDispatcher implements ChannelDispatcher {

    @Override
    public String channelCode() { return "IN_APP"; }

    @Override
    public String displayName() { return "站内消息"; }

    @Override
    public DeliveryResult deliver(MsgNotification notification) {
        // 站内信落库后用户即可在 / message/list 看到, 不需要外部投递
        return DeliveryResult.ok();
    }
}
