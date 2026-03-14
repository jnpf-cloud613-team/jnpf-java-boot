package jnpf.base.service.impl;


import jnpf.base.mapper.SignMapper;
import jnpf.base.service.SignService;
import jnpf.base.service.SuperServiceImpl;
import jnpf.permission.entity.SignEntity;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 个人签名
 *
 * @author JNPF开发平台组
 * @copyright 引迈信息技术有限公司
 * @date 2022年9月2日 上午9:18
 */
@Service
public class SignServiceImpl extends SuperServiceImpl<SignMapper, SignEntity> implements SignService {

    @Override
    public List<SignEntity> getList() {
        return this.baseMapper.getList();
    }

    @Override
    public boolean create(SignEntity entity) {
        return this.baseMapper.create(entity);
    }

    @Override
    public boolean delete(String id) {
        return this.removeById(id);
    }


    @Override
    public boolean updateDefault(String id) {
        return this.baseMapper.updateDefault(id);
    }

    @Override
    public SignEntity getDefaultByUserId(String id) {
        return this.baseMapper.getDefaultByUserId(id);
    }
}
