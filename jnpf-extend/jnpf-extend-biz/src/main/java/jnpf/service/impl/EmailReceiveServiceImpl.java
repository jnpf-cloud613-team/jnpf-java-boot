package jnpf.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.PaginationTime;
import jnpf.base.entity.EmailConfigEntity;
import jnpf.constant.FileTypeConstant;
import jnpf.message.model.mail.*;
import jnpf.base.service.SuperServiceImpl;
import jnpf.config.ConfigValueUtil;
import jnpf.base.entity.EmailReceiveEntity;
import jnpf.constant.MsgCode;
import jnpf.entity.EmailSendEntity;
import jnpf.exception.DataException;
import jnpf.mapper.EmailConfigMapper;
import jnpf.mapper.EmailReceiveMapper;
import jnpf.mapper.EmailSendMapper;
import jnpf.service.EmailReceiveService;
import jnpf.util.*;
import jnpf.util.type.StringNumber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 邮件接收
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2019年9月26日 上午9:18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailReceiveServiceImpl extends SuperServiceImpl<EmailReceiveMapper, EmailReceiveEntity> implements EmailReceiveService {


    private final EmailSendMapper emailSendMapper;

    private final EmailConfigMapper emailConfigMapper;

    private final Pop3Util pop3Util;

    private final ConfigValueUtil configValueUtil;


    @Override
    public List<EmailReceiveEntity> getReceiveList(PaginationTime paginationTime) {
        return this.baseMapper.getReceiveList(paginationTime);
    }

    @Override
    public List<EmailReceiveEntity> getDashboardReceiveList() {
        return this.baseMapper.getDashboardReceiveList();
    }

    @Override
    public List<EmailReceiveEntity> getStarredList(PaginationTime paginationTime) {
        return this.baseMapper.getStarredList(paginationTime);
    }

    @Override
    public List<EmailSendEntity> getDraftList(PaginationTime paginationTime) {
        return emailSendMapper.getDraftList(paginationTime);
    }

    @Override
    public List<EmailSendEntity> getSentList(PaginationTime paginationTime) {
        return emailSendMapper.getSentList(paginationTime);
    }

    @Override
    public EmailConfigEntity getConfigInfo() {
        String userId = UserProvider.getUser().getUserId();
        QueryWrapper<EmailConfigEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(EmailConfigEntity::getCreatorUserId, userId);
        return emailConfigMapper.selectOne(queryWrapper);
    }

    @Override
    public EmailConfigEntity getConfigInfo(String userId) {
        QueryWrapper<EmailConfigEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(EmailConfigEntity::getCreatorUserId, userId);
        return emailConfigMapper.selectOne(queryWrapper);
    }

    @Override
    public Object getInfo(String id) {
        EmailReceiveEntity receiveInfo = this.getById(id);
        Object object;
        if (receiveInfo != null) {
            //解析内容
            receiveInfo.setBodyText(receiveInfo.getBodyText());
            //更新已读
            receiveInfo.setIsRead(1);
            receiveInfo.setLastModifyTime(new Date());
            receiveInfo.setLastModifyUserId(UserProvider.getUser().getUserId());
            this.updateById(receiveInfo);
            object = receiveInfo;
        } else {
            object = emailSendMapper.selectById(id);
        }
        return object;
    }

    @Override
    public boolean delete(String id) {
        Object object = getInfo(id);
        if (ObjectUtil.isNotNull(object) && object instanceof EmailReceiveEntity) {
            //删除邮件
            EmailConfigEntity mailConfig = getConfigInfo();
            EmailReceiveEntity mailReceiveEntity = (EmailReceiveEntity) object;
            MailAccount mailAccount = new MailAccount();
            mailAccount.setAccount(mailConfig.getAccount());
            mailAccount.setPassword(mailConfig.getPassword());
            mailAccount.setPop3Port(mailConfig.getPop3Port());
            mailAccount.setPop3Host(mailConfig.getPop3Host());
            pop3Util.deleteMessage(mailAccount, mailReceiveEntity.getMID());
            this.removeById(mailReceiveEntity.getId());
            return true;
        } else if (object != null) {
            //删除数据
            EmailSendEntity entity = (EmailSendEntity) object;
            emailSendMapper.deleteById(entity.getId());
            return true;
        }
        return false;
    }

    @Override
    @DSTransactional
    public void saveDraft(EmailSendEntity entity) {
        entity.setState(-1);
        if (StringUtil.isNotEmpty(entity.getId())) {
            entity.setLastModifyTime(new Date());
            entity.setLastModifyUserId(UserProvider.getUser().getUserId());
            emailSendMapper.updateById(entity);
        } else {
            entity.setId(RandomUtil.uuId());
            entity.setCreatorUserId(UserProvider.getUser().getUserId());
            emailSendMapper.insert(entity);
        }
    }

    @Override
    public boolean receiveRead(String id, int isRead) {
        EmailReceiveEntity entity = (EmailReceiveEntity) getInfo(id);
        if (entity != null) {
            entity.setIsRead(isRead);
            return this.updateById(entity);
        }
        return false;
    }

    @Override
    public boolean receiveStarred(String id, int isStarred) {
        EmailReceiveEntity entity = (EmailReceiveEntity) getInfo(id);
        if (entity != null) {
            entity.setStarred(isStarred);
            return this.updateById(entity);
        }
        return false;
    }

    @Override
    public void saveConfig(EmailConfigEntity configEntity) throws DataException {
        EmailConfigEntity emailConfigEntity = getConfigInfo(UserProvider.getUser().getUserId());
        if (emailConfigEntity == null && UserProvider.getUser().getUserId() != null) {
            configEntity.setId(RandomUtil.uuId());
            configEntity.setCreatorTime(new Date());
            configEntity.setCreatorUserId(UserProvider.getUser().getUserId());
            emailConfigMapper.insert(configEntity);
        } else if (UserProvider.getUser().getUserId() != null) {
            if (emailConfigEntity != null) {
                configEntity.setId(emailConfigEntity.getId());
            }

            emailConfigMapper.updateById(configEntity);
        } else {
            throw new DataException(MsgCode.ETD114.get());
        }
    }

    @Override
    @DSTransactional
    public int saveSent(EmailSendEntity entity, EmailConfigEntity mailConfig) {
        int flag = 1;
        //拷贝文件,注意：从临时文件夹拷贝到邮件文件夹
        List<MailFile> attachmentList = JsonUtil.getJsonToList(entity.getAttachment(), MailFile.class);
        //邮件路径
        String mailFilePath = configValueUtil.getEmailFilePath();
        try {
            //写入数据
            //发送邮件
            //邮件发送信息
            List<File> attachmentFile = new ArrayList<>();
            MailModel mailModel = new MailModel();
            mailModel.setFrom(entity.getSender());
            mailModel.setRecipient(entity.getRecipient());
            mailModel.setCc(entity.getCc());
            mailModel.setBcc(entity.getBcc());
            mailModel.setSubject(entity.getSubject());
            mailModel.setBodyText(entity.getBodyText());
            mailModel.setAttachment(attachmentList);
            mailModel.setFromName(mailConfig.getSenderName());
            for (MailFile mailFile : mailModel.getAttachment()) {
                File file = new File(XSSEscape.escapePath(mailFilePath + mailFile.getFileId()));
                attachmentFile.add(file);
            }
            mailModel.setAttachmentFile(attachmentFile);
            //账号验证信息
            MailAccount mailAccount = new MailAccount();
            mailAccount.setAccount(mailConfig.getAccount());
            mailAccount.setPassword(mailConfig.getPassword());
            mailAccount.setPop3Host(mailConfig.getPop3Host());
            mailAccount.setPop3Port(mailConfig.getPop3Port());
            mailAccount.setSmtpHost(mailConfig.getSmtpHost());
            mailAccount.setSmtpPort(mailConfig.getSmtpPort());
            mailAccount.setSsl(mailConfig.getEmailSsl() == 1);
            mailAccount.setAccountName(mailConfig.getSenderName());
            SmtpUtil smtpUtil = new SmtpUtil(mailAccount);
            smtpUtil.sendMail(mailModel);
            flag = 0;
            //插入数据库
            if (entity.getId() != null) {
                entity.setState(1);
                emailSendMapper.updateById(entity);
            } else {
                entity.setId(RandomUtil.uuId());
                entity.setCreatorUserId(UserProvider.getUser().getUserId());
                if (mailConfig.getAccount() != null) {
                    entity.setSender(mailConfig.getAccount());
                }
                entity.setState(1);
                emailSendMapper.insert(entity);
            }
        } catch (Exception e) {
            for (MailFile mailFile : attachmentList) {
                FileUtil.deleteFile(mailFilePath + mailFile.getFileId());
            }
            log.error(e.getMessage());
        }
        return flag;
    }

    @Override
    @DSTransactional
    public int receive(EmailConfigEntity mailConfig) {
        //账号验证信息
        MailAccount mailAccount = new MailAccount();
        mailAccount.setAccount(mailConfig.getAccount());
        mailAccount.setPassword(mailConfig.getPassword());
        mailAccount.setPop3Host(mailConfig.getPop3Host());
        mailAccount.setPop3Port(mailConfig.getPop3Port());
        mailAccount.setSmtpHost(mailConfig.getSmtpHost());
        mailAccount.setSmtpPort(mailConfig.getSmtpPort());
        mailAccount.setDestDir(FileUploadUtils.getLocalBasePath() + FilePathUtil.getFilePath(FileTypeConstant.MAIL));
        mailAccount.setSsl(StringNumber.ONE.equals(mailConfig.getEmailSsl().toString()));
        PopMailModel popMailModel = pop3Util.popMail(mailAccount);

        int receiveCount = popMailModel.getReceiveCount();
        List<ReceiveModel> mailList = popMailModel.getMailList();
        if (!mailList.isEmpty()) {
            List<String> mids = mailList.stream().map(ReceiveModel::getMID).collect(Collectors.toList());
            //查询数据库状态
            QueryWrapper<EmailReceiveEntity> wrapper = new QueryWrapper<>();
            wrapper.lambda().in(EmailReceiveEntity::getMID, mids);
            List<EmailReceiveEntity> emails = this.list(wrapper);
            this.remove(wrapper);
            //邮件赋值状态
            for (ReceiveModel receiveModel : mailList) {
                EmailReceiveEntity entity = JsonUtil.getJsonToBean(receiveModel, EmailReceiveEntity.class);
                entity.setCreatorUserId(UserProvider.getUser().getUserId());
                //通过数据库进行赋值，没有就默认0
                Optional<EmailReceiveEntity> first = emails.stream().filter(m -> m.getMID().equals(entity.getMID())).findFirst();
                if (first.isPresent()) {
                    int stat = emails.stream().anyMatch(m -> m.getMID().equals(entity.getMID())) ? first.get().getIsRead() : 0;


                    entity.setIsRead(stat);
                }

                long count = emails.stream().filter(m -> m.getMID().equals(entity.getMID())).count();
                if (count != 0) {
                    receiveCount--;
                }
                this.save(entity);
            }
        }
        return receiveCount;
    }
}
