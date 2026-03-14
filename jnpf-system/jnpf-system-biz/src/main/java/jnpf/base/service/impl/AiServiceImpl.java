package jnpf.base.service.impl;

import jnpf.base.mapper.AiMapper;
import jnpf.base.model.ai.AiPagination;
import jnpf.base.service.AiService;
import jnpf.base.service.SuperServiceImpl;
import jnpf.base.entity.AiEntity;
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
public class AiServiceImpl extends SuperServiceImpl<AiMapper, AiEntity> implements AiService {


    @Override
    public List<AiEntity> getList(AiPagination pagination) {
        return this.baseMapper.getList(pagination);
    }

    @Override
    public boolean isExistByFullName(String fullName, String id) {
        return this.baseMapper.isExistByFullName(fullName,id);
    }

    @Override
    public AiEntity getInfo(String id) {
        return this.baseMapper.getInfo(id);
    }

    @Override
    public boolean create(AiEntity entity) {
        return this.baseMapper.create(entity);
    }

    @Override
    public boolean update(String id, AiEntity entity) {
        return this.baseMapper.update(id, entity);
    }

    @Override
    public void delete(AiEntity entity) {
        this.baseMapper.delete(entity);
    }

    @Override
    public AiEntity getDefault() {
        return this.baseMapper.getDefault();
    }
}
