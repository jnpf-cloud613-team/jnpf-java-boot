package jnpf.base.service.impl;


import jnpf.base.entity.SignatureUserEntity;
import jnpf.base.mapper.SignatureUserMapper;
import jnpf.base.service.SignatureUserService;
import jnpf.base.service.SuperServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 电子签章
 *
 * @author JNPF开发平台组
 * @copyright 引迈信息技术有限公司
 * @date 2022年9月2日 上午9:18
 */
@Service
public class SignatureUserServiceImpl extends SuperServiceImpl<SignatureUserMapper, SignatureUserEntity> implements SignatureUserService {

    @Override
    public List<SignatureUserEntity> getList(List<String> signatureId, List<String> userId) {
        return this.baseMapper.getList(signatureId, userId);
    }

    @Override
    public List<SignatureUserEntity> getList(String signatureId) {
        return this.baseMapper.getList(signatureId);
    }

    @Override
    public List<SignatureUserEntity> getListByUserId(String userId) {
        return this.baseMapper.getListByUserId(userId);
    }

    @Override
    public void create(SignatureUserEntity entity) {
        this.baseMapper.create(entity);
    }

    @Override
    public void delete(String signatureId) {
        this.baseMapper.delete(signatureId);
    }
}
