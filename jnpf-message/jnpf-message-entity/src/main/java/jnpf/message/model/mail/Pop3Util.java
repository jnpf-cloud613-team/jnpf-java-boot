package jnpf.message.model.mail;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.internet.MimeUtility;
import jnpf.util.DateUtil;
import jnpf.util.JsonUtil;
import jnpf.util.RandomUtil;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/12 15:31
 */
@Slf4j
@Component
public class Pop3Util {

    private static final String MES_TYPE = "message/rfc822";
    private static final String MULTIPART_TYPE = "multipart/*";

    /**
     * 邮箱验证
     *
     * @param mailAccount
     * @return
     */
    public String checkConnected(MailAccount mailAccount) {
        try {
            Properties props = getProperties(mailAccount.getSsl());
            Session session = getSession(props);

            try (Store store = getStore(session, mailAccount)) {
                store.connect(); // 实际测试连接
                return "true";
            }
        } catch (Exception e) {
            log.error("邮件服务器连接失败: {}", e.getMessage(), e);
            return e.getMessage();
        }
    }

    /**
     * 接收邮件
     */
    public PopMailModel popMail(MailAccount mailAccount) {
        PopMailModel map = new PopMailModel();
        try {
            Properties props = getProperties(mailAccount.getSsl());
            Session session = getSession(props);
            @Cleanup Store store = getStore(session, mailAccount);
            @Cleanup Folder folder = getFolder(store);
            int receiveCount = folder.getMessageCount();
            Message[] messages = folder.getMessages();
            List<ReceiveModel> entity = parseMessage(messages, mailAccount);
            map.setReceiveCount(receiveCount);
            map.setMailList(entity);
            return map;
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return map;
    }

    /**
     * 删除邮件
     *
     * @param mailAccount
     * @param mid
     */
    public void deleteMessage(MailAccount mailAccount, String mid) {
        try {
            Properties props = getProperties(false);
            Session session = getSession(props);
            @Cleanup Store store = getStore(session, mailAccount);
            @Cleanup Folder folder = getFolder(store);
            Message[] messages = folder.getMessages();
            deleteMessage(messages, mid);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    /**
     * 获取Properties
     *
     * @param ssl
     */
    private Properties getProperties(boolean ssl) {
        Properties props = new Properties();
        props.setProperty("mail.store.protocol", "pop3");
        props.setProperty("mail.pop3.auth", "true");
        // 设置连接超时时间
        props.put("mail.pop3.connectiontimeout", "35000");
        // 设置读取超时时间
        props.put("mail.pop3.timeout", "10000");
        // 设置写入超时时间
        props.put("mail.pop3.writetimeout", "10000");
        if (ssl) {
            props.put("mail.pop3.ssl.enable", "true");
            props.put("mail.pop3.socketFactory.fallback", "false");
            props.setProperty("mail.pop3.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        }
        return props;
    }

    /**
     * 获取Session
     *
     * @param props
     */
    private Session getSession(Properties props) {
        Session session = Session.getInstance(props);
        session.setDebug(true);
        return session;
    }

    /**
     * 获取Store
     */
    private Store getStore(Session session, MailAccount mailAccount) throws MessagingException {
        Store store = session.getStore();
        store.connect(mailAccount.getPop3Host(), mailAccount.getPop3Port(), mailAccount.getAccount(), mailAccount.getPassword());
        return store;
    }

    /**
     * 获取Folder
     */
    private Folder getFolder(Store store) throws MessagingException {
        Folder folder = store.getFolder("INBOX");
        folder.open(Folder.READ_ONLY);
        return folder;
    }

    /**
     * 解析邮件
     *
     * @param messages 要解析的邮件列表
     */
    private List<ReceiveModel> parseMessage(Message[] messages, MailAccount mailAccount) throws MessagingException {
        List<ReceiveModel> receiveEntity = new ArrayList<>();
        if (messages == null || messages.length < 1) {
            throw new MessagingException("未找到要解析的邮件!");
        }
        List<MailFile> mailFiles;
        String mailfiles = "";
        for (int i = 0, count = messages.length; i < count; i++) {
            try {
                MimeMessage msg = (MimeMessage) messages[i];
                boolean isContainerAttachment = isContainAttachment(msg);
                if (isContainerAttachment) {
                    //保存附件
                    String destDir = mailAccount.getDestDir();
                    mailFiles = saveAttachment(msg, destDir);
                    mailfiles = JsonUtil.getObjectToString(mailFiles);
                } else {
                    mailfiles = "[]";
                }
                StringBuilder content = new StringBuilder(30);
                getMailTextContent(msg, content);
                ReceiveModel entity = new ReceiveModel();
                entity.setId(RandomUtil.uuId());
                entity.setMAccount(getReceiveAddress(msg, null));
                entity.setMID(getMessageId(msg));
                if (getFrom(msg) == null) {
                    entity.setSender("00000");
                    entity.setSenderName("匿名");
                } else {
                    entity.setSender(getFrom(msg).split("_")[0]);
                    entity.setSenderName(getFrom(msg).split("_")[1]);
                }
                entity.setSubject(getSubject(msg));
                entity.setBodyText(content.toString());
                entity.setAttachment(mailfiles);
                entity.setFdate(msg.getSentDate());
                entity.setIsRead(0);
                receiveEntity.add(entity);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
        return receiveEntity;
    }

    /**
     * 解析邮件
     *
     * @param messages 要解析的邮件列表
     */
    private void deleteMessage(Message[] messages, String mid) throws MessagingException {
        if (messages == null || messages.length < 1) {
            throw new MessagingException("未找到要解析的邮件!");
        }
        for (int i = 0; i < messages.length; i++) {
            Message message = messages[i];
            MimeMessage msg = (MimeMessage) messages[i];
            if (deleteMessageId(msg, mid)) {
                message.setFlag(Flags.Flag.DELETED, true);
            }
        }
    }

    /**
     * 判断mid是否一致
     *
     * @param msg
     * @param mid
     * @return
     * @throws MessagingException
     */
    private boolean deleteMessageId(MimeMessage msg, String mid) throws MessagingException {
        String messageId = msg.getMessageID();
        messageId = messageId.replace("<", "");
        messageId = messageId.replace(">", "");
        return messageId.equals(mid);
    }

    /**
     * 获得邮件主题
     *
     * @param msg 邮件内容
     * @return 解码后的邮件主题
     */
    private String getSubject(MimeMessage msg) throws UnsupportedEncodingException, MessagingException {
        return MimeUtility.decodeText(msg.getSubject());
    }

    /**
     * 获得邮件发件人
     *
     * @param msg 邮件内容
     * @return 姓名 <Email地址>
     * @throws MessagingException
     * @throws UnsupportedEncodingException
     */
    private String getFrom(MimeMessage msg) throws MessagingException, UnsupportedEncodingException {
        String from = "";
        Address[] froms = msg.getFrom();
        InternetAddress address = (InternetAddress) froms[0];
        String person = address.getPersonal();
        if (person != null) {
            person = MimeUtility.decodeText(person) + " ";
        } else {
            person = "";
        }
        from = person + "_" + address.getAddress();
        return from;
    }

    /**
     * 获取邮件的id
     */
    private String getMessageId(MimeMessage msg) throws MessagingException {
        String messageId = msg.getMessageID();
        if (messageId == null) {
            return "";
        }
        messageId = messageId.replace("<", "");
        messageId = messageId.replace(">", "");
        return messageId;
    }

    /**
     * 根据收件人类型，获取邮件收件人、抄送和密送地址。如果收件人类型为空，则获得所有的收件人
     * <p>Message.RecipientType.TO  收件人</p>
     * <p>Message.RecipientType.CC  抄送</p>
     * <p>Message.RecipientType.BCC 密送</p>
     *
     * @param msg  邮件内容
     * @param type 收件人类型
     * @return 收件人1 <邮件地址1>, 收件人2 <邮件地址2>, ...
     * @throws MessagingException
     */
    private String getReceiveAddress(MimeMessage msg, Message.RecipientType type) throws MessagingException {
        StringBuilder receiveAddress = new StringBuilder();
        Address[] addresss = null;
        if (type == null) {
            addresss = msg.getAllRecipients();
        } else {
            addresss = msg.getRecipients(type);
        }
        if (addresss == null || addresss.length < 1) {
            return null;
        }
        for (Address address : addresss) {
            InternetAddress internetAddress = (InternetAddress) address;
            receiveAddress.append(internetAddress.toUnicodeString()).append(",");
        }
        //删除最后一个逗号
        receiveAddress.deleteCharAt(receiveAddress.length() - 1);
        return receiveAddress.toString();
    }

    /**
     * 判断邮件中是否包含附件
     *
     * @return 邮件中存在附件返回true，不存在返回false
     * @throws MessagingException
     * @throws IOException
     */
    private boolean isContainAttachment(Part part) {
        boolean flag = false;
        try {
            if (part.isMimeType(MULTIPART_TYPE)) {
                MimeMultipart multipart = (MimeMultipart) part.getContent();
                int partCount = multipart.getCount();
                for (int i = 0; i < partCount; i++) {
                    BodyPart bodyPart = multipart.getBodyPart(i);
                    String disp = bodyPart.getDisposition();
                    if (disp != null && (disp.equalsIgnoreCase(Part.ATTACHMENT) || disp.equalsIgnoreCase(Part.INLINE))) {
                        flag = true;
                    } else if (bodyPart.isMimeType(MULTIPART_TYPE)) {
                        flag = isContainAttachment(bodyPart);
                    } else {
                        String contentType = bodyPart.getContentType();
                        if (contentType.contains("application")) {
                            flag = true;
                        }
                        if (contentType.contains("name")) {
                            flag = true;
                        }
                    }
                    if (flag) {
                        break;
                    }
                }
            } else if (part.isMimeType(MES_TYPE)) {
                flag = isContainAttachment((Part) part.getContent());
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return flag;
    }

    /**
     * 获得邮件文本内容
     *
     * @param part    邮件体
     * @param content 存储邮件文本内容的字符串
     * @throws MessagingException
     * @throws IOException
     */
    private void getMailTextContent(Part part, StringBuilder content) {
        try {
            boolean isContainTextAttach = part.getContentType().contains("name");
            if (part.isMimeType("text/html") && !isContainTextAttach) {
                content.append(part.getContent().toString());
            } else if (part.isMimeType(MES_TYPE)) {
                getMailTextContent((Part) part.getContent(), content);
            } else if (part.isMimeType(MULTIPART_TYPE)) {
                Multipart multipart = (Multipart) part.getContent();
                int partCount = multipart.getCount();
                for (int i = 0; i < partCount; i++) {
                    BodyPart bodyPart = multipart.getBodyPart(i);
                    getMailTextContent(bodyPart, content);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 保存附件
     *
     * @param part    邮件中多个组合体中的其中一个组合体
     * @param destDir 附件保存目录
     * @throws UnsupportedEncodingException
     * @throws MessagingException
     * @throws FileNotFoundException
     * @throws IOException
     */
    private List<MailFile> saveAttachment(Part part, String destDir) {
        List<MailFile> mailFiles = new ArrayList<>();
        try {
            if (part.isMimeType(MULTIPART_TYPE)) {
                Multipart multipart = (Multipart) part.getContent();
                int partCount = multipart.getCount();
                for (int i = 0; i < partCount; i++) {
                    BodyPart bodyPart = multipart.getBodyPart(i);
                    String disp = bodyPart.getDisposition();
                    if (disp != null && (disp.equalsIgnoreCase(Part.ATTACHMENT) || disp.equalsIgnoreCase(Part.INLINE))) {
                        MailFile mailFile = new MailFile();
                        @Cleanup InputStream is = bodyPart.getInputStream();
                        //解决附件中文乱码
                        String fileName = MimeUtility.decodeText(bodyPart.getFileName());
                        String fileType = fileName.split("\\.")[1];

                        mailFile.setFileId(RandomUtil.uuId() + "." + fileType);
                        saveFile(is, destDir, decodeText(mailFile.getFileId()));
                        File file = new File(destDir + decodeText(fileName));
                        mailFile.setFileName(fileName);
                        mailFile.setFileSize(String.valueOf(file.length()));
                        mailFile.setFileState("-1");
                        mailFile.setFileTime(DateUtil.getNow());
                        mailFiles.add(mailFile);
                    } else if (bodyPart.isMimeType(MULTIPART_TYPE)) {
                        saveAttachment(bodyPart, destDir);
                    } else {
                        String contentType = bodyPart.getContentType();
                        if (contentType.indexOf("name") != -1 || contentType.indexOf("application") != -1) {
                            saveFile(bodyPart.getInputStream(), destDir, decodeText(bodyPart.getFileName()));
                            File file = new File(destDir + decodeText(bodyPart.getFileName()));
                            MailFile mailFile = new MailFile();
                            mailFile.setFileId(RandomUtil.uuId());
                            mailFile.setFileName(file.getName());
                            mailFile.setFileSize(String.valueOf(file.length()));
                            mailFile.setFileState("-1");
                            mailFile.setFileTime(DateUtil.getNow());
                            mailFiles.add(mailFile);
                        }
                    }
                }
            } else if (part.isMimeType(MES_TYPE)) {
                saveAttachment((Part) part.getContent(), destDir);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mailFiles;
    }

    /**
     * 读取输入流中的数据保存至指定目录
     *
     * @param is       输入流
     * @param fileName 文件名
     * @param destDir  文件存储目录
     * @throws FileNotFoundException
     * @throws IOException
     */
    private void saveFile(InputStream is, String destDir, String fileName) throws IOException {
        File destFile = new File(destDir, fileName);
        try (BufferedInputStream bis = new BufferedInputStream(is);
             FileOutputStream fileOutputStream = new FileOutputStream(destFile);
             BufferedOutputStream bos = new BufferedOutputStream(fileOutputStream)) {
            byte[] buffer = new byte[8192]; // 使用缓冲区提高性能
            int len;
            while ((len = bis.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            bos.flush();
        }
    }

    /**
     * 文本解码
     *
     * @param encodeText 解码MimeUtility.encodeText(String text)方法编码后的文本
     * @return 解码后的文本
     * @throws UnsupportedEncodingException
     */
    private String decodeText(String encodeText) throws UnsupportedEncodingException {
        if (encodeText == null || "".equals(encodeText)) {
            return "";
        } else {
            return MimeUtility.decodeText(encodeText);
        }
    }
}
