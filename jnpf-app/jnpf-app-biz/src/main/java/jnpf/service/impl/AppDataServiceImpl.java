package jnpf.service.impl;


import jnpf.base.service.SuperServiceImpl;
import jnpf.entity.AppDataEntity;
import jnpf.mapper.AppDataMapper;
import jnpf.service.AppDataService;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * app常用数据
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021-08-08
 */
@Service
public class AppDataServiceImpl extends SuperServiceImpl<AppDataMapper, AppDataEntity> implements AppDataService {


    @Override
    public List<AppDataEntity> getList() {
        return this.baseMapper.getList();
    }

    @Override
    public AppDataEntity getInfo(String objectId) {
        return this.baseMapper.getInfo(objectId);
    }

    @Override
    public boolean isExistByObjectId(String objectId, String systemId) {
        return this.baseMapper.isExistByObjectId(objectId, systemId);
    }

    @Override
    public void create(AppDataEntity entity) {
        this.baseMapper.create(entity);
    }

    @Override
    public void delete(AppDataEntity entity) {
        this.baseMapper.delete(entity);
    }


}
