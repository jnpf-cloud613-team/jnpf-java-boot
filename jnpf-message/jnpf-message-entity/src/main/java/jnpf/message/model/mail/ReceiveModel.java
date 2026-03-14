package jnpf.message.model.mail;

import lombok.Data;

import java.util.Date;

@Data
public class ReceiveModel {
    private String id;
    private String mAccount;
    private String mID;
    private String sender;
    private String senderName;
    private String subject;
    private String bodyText;
    private String attachment;
    private Integer isRead;
    private Date fdate;
    private Integer starred;
}
