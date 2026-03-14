package jnpf.message.service;

import jnpf.message.model.SentMessageForm;

import java.util.List;

public interface SendMsgService {

    void sendMessage(SentMessageForm sentMessageForm);

    List<String> sendScheduleMessage(SentMessageForm sentMessageForm);
}
