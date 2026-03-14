package jnpf.service.impl;

import jnpf.base.service.SuperServiceImpl;
import jnpf.base.Pagination;
import jnpf.mapper.ContractMapper;
import jnpf.service.ContractService;
import jnpf.entity.ContractEntity;
import jnpf.util.RandomUtil;
import org.springframework.stereotype.Service;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;

import java.util.List;

/**
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/16 9:47
 */
@Service
public class ContractServiceImpl extends SuperServiceImpl<ContractMapper, ContractEntity> implements ContractService {


    @Override
    public List<ContractEntity> getlist(Pagination pagination) {
        return getBaseMapper().getlist(pagination);
    }

    @Override
    public ContractEntity getInfo(String id) {
        return this.getById(id);
    }

    @Override
    @DSTransactional
    public void create(ContractEntity entity) {
        entity.setId(RandomUtil.uuId());
        this.save(entity);
    }

    @Override
    @DSTransactional
    public void update(ContractEntity entity) {
        this.updateById(entity);
    }

    @Override
    public void delete(ContractEntity entity) {
        if (entity != null) {
            this.removeById(entity.getId());
        }
    }
}
