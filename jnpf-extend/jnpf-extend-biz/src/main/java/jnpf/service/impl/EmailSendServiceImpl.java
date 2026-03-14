package jnpf.service.impl;

import jnpf.base.service.SuperServiceImpl;
import jnpf.entity.EmailSendEntity;
import jnpf.mapper.EmailSendMapper;
import jnpf.service.EmailSendService;
import org.springframework.stereotype.Service;


/**
 * 邮件发送
 *
 * @copyright 引迈信息技术有限公司
 * @author JNPF开发平台组
 * @date 2019年9月26日 上午9:18
 */
@Service
public class EmailSendServiceImpl extends SuperServiceImpl<EmailSendMapper, EmailSendEntity> implements EmailSendService {

}
