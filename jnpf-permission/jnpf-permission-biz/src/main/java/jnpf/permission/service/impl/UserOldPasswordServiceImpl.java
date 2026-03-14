package jnpf.permission.service.impl;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import jnpf.base.service.SuperServiceImpl;
import jnpf.permission.entity.UserOldPasswordEntity;
import jnpf.permission.mapper.UserOldPasswordMapper;
import jnpf.permission.service.UserOldPasswordService;
import org.springframework.stereotype.Service;

/**
 * 用户信息
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月26日 上午9:18
 */
@Service
@DSTransactional
public class UserOldPasswordServiceImpl extends SuperServiceImpl<UserOldPasswordMapper, UserOldPasswordEntity> implements UserOldPasswordService {

}
