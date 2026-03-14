package jnpf.message.util;

import com.alibaba.fastjson.JSONObject;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jnpf.message.model.message.EmailModel;

import java.io.UnsupportedEncodingException;
import java.util.Properties;

/**
 * 邮件类
 *
 * @版本： V3.1.0
 * @版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @作者： JNPF开发平台组
 * @日期： 2021/4/20 14:52
 */
public class EmailUtil {
    EmailUtil() {
    }

    private static final String EMAIL_REGEX = "^[\\w.+%-]+@[\\w.-]+\\.[a-zA-Z]{2,}$";

    public static boolean isEmail(String email) {
        return email.matches(EMAIL_REGEX);
    }

    public static JSONObject sendMail(EmailModel emailModel) {
        JSONObject retMsg = new JSONObject();
        // 邮件发送人
        String from = emailModel.getEmailAccount();
        // 邮件接收人的邮件地址
        String to = emailModel.getEmailToUsers();

        //定义Properties对象,设置环境信息
        Properties props = System.getProperties();

        // 设置邮件服务器的地址
        // 指定的smtp服务器
        props.setProperty("mail.smtp.host", emailModel.getEmailSmtpHost());
        props.setProperty("mail.smtp.auth", "true");
        //ssl安全链接
        props.setProperty("mail.smtp.ssl.enable", emailModel.getEmailSsl());
        //设置发送邮件使用的协议
        props.setProperty("mail.transport.protocol", "smtp");
        if ("587".equals(emailModel.getEmailSmtpPort())) {
            props.put("mail.smtp.starttls.enable", "true");
        }
        //创建Session对象,session对象表示整个邮件的环境信息
        Session session = Session.getInstance(props);
        //设置输出调试信息
        session.setDebug(true);
        try {
            // Message的实例对象表示一封电子邮件
            MimeMessage message = new MimeMessage(session);
            // 设置发件人的地址
            message.setFrom(new InternetAddress(from, emailModel.getEmailSenderName(), "UTF-8"));
            // 设置收件人信息
            InternetAddress[] sendTo = InternetAddress.parse(to);
            message.setRecipients(Message.RecipientType.TO, sendTo);

            // 设置主题
            message.setSubject(emailModel.getEmailTitle());
            // 设置邮件的文本内容
            message.setContent((emailModel.getEmailContent()), "text/html;charset=utf-8");

            // 获取发送邮件的对象
            Transport transport = session.getTransport();
            // 连接邮件服务器
            transport.connect(emailModel.getEmailSmtpHost(), Integer.parseInt(emailModel.getEmailSmtpPort()), emailModel.getEmailAccount(), emailModel.getEmailPassword());
            // 发送消息
            transport.sendMessage(message, sendTo);

            transport.close();

            retMsg.put("code", true);
            retMsg.put("error", "");
            return retMsg;
        } catch (MessagingException | UnsupportedEncodingException e) {
            retMsg.put("code", false);
            retMsg.put("error", e.toString());
            return retMsg;
        }
    }
}

