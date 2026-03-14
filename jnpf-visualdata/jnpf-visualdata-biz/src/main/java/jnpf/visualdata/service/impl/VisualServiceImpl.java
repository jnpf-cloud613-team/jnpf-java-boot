package jnpf.visualdata.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jnpf.base.entity.SystemEntity;
import jnpf.base.service.SystemService;
import jnpf.constant.MsgCode;
import jnpf.base.service.SuperServiceImpl;
import jnpf.exception.DataException;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;
import jnpf.util.context.RequestContext;
import jnpf.visualdata.entity.VisualConfigEntity;
import jnpf.visualdata.entity.VisualEntity;

import jnpf.visualdata.mapper.VisualMapper;
import jnpf.visualdata.model.visual.VisualPaginationModel;
import jnpf.visualdata.service.VisualConfigService;
import jnpf.visualdata.service.VisualService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 大屏基本信息
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021年6月15日
 */
@Service
@RequiredArgsConstructor
public class VisualServiceImpl extends SuperServiceImpl<VisualMapper, VisualEntity> implements VisualService {

    

    private final VisualConfigService configService;

    private final SystemService systemService;

    @Override
    public List getList(VisualPaginationModel pagination) {
        QueryWrapper<VisualEntity> queryWrapper = new QueryWrapper<>();
        SystemEntity infoByEnCode = systemService.getInfoByEnCode(RequestContext.getAppCode());
        if(ObjectUtil.isNotEmpty(pagination.getTitle())){
            queryWrapper.lambda().like(VisualEntity::getTitle, pagination.getTitle());
        }
        queryWrapper.lambda().eq(VisualEntity::getCategory, pagination.getCategory());
        queryWrapper.lambda().eq(VisualEntity::getSystemId, infoByEnCode.getId());
        queryWrapper.lambda().orderByDesc(VisualEntity::getCreateTime);
        Page<VisualEntity> page = new Page<>(pagination.getCurrent(), pagination.getSize());
        Page<VisualEntity> iPages = this.page(page, queryWrapper);
        return pagination.setData(iPages);
    }

    @Override
    public List<VisualEntity> getList() {
        SystemEntity infoByEnCode = systemService.getInfoByEnCode(RequestContext.getAppCode());
        QueryWrapper<VisualEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(VisualEntity::getSystemId, infoByEnCode.getId());
        queryWrapper.lambda().orderByDesc(VisualEntity::getCreateTime);

        return this.list(queryWrapper);
    }

    @Override
    public VisualEntity getInfo(String id) {
        QueryWrapper<VisualEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(VisualEntity::getId, id);
        return this.getOne(queryWrapper);
    }

    @Override
    public void create(VisualEntity entity, VisualConfigEntity configEntity) {
        getSystemId(entity);
        entity.setId(RandomUtil.uuId());
        entity.setCreateTime(new Date());
        entity.setUpdateUser(UserProvider.getLoginUserId());
        entity.setStatus(1);
        entity.setIsDeleted(0);
        this.creUpdateCheck(entity,true);
        this.save(entity);
        configEntity.setVisualId(entity.getId());
        configService.create(configEntity);
    }

    private void getSystemId(VisualEntity entity) {
        SystemEntity infoByEnCode = systemService.getInfoByEnCode(RequestContext.getAppCode());
        entity.setSystemId(infoByEnCode.getId());
    }
    @Override
    public String getSystemIdByReq() {
        SystemEntity infoByEnCode = systemService.getInfoByEnCode(RequestContext.getAppCode());
        return null==infoByEnCode.getId()?"":infoByEnCode.getId();
    }
    @Override
    public boolean update(String id, VisualEntity entity, VisualConfigEntity configEntity) {
        getSystemId(entity);
        entity.setId(id);
        entity.setUpdateTime(new Date());
        entity.setUpdateUser(UserProvider.getLoginUserId());
        this.creUpdateCheck(entity,true);
        boolean flag = this.updateById(entity);
        if (configEntity != null) {
            configService.update(configEntity.getId(), configEntity);
        }
        return flag;
    }

    @Override
    public void delete(VisualEntity entity) {
        if (entity != null) {
            this.removeById(entity.getId());
            VisualConfigEntity config = configService.getInfo(entity.getId());
            configService.delete(config);
        }
    }

    @Override
    public void createImport(VisualEntity entity, VisualConfigEntity configEntity) throws DataException {
        try {
            getSystemId(entity);
            this.creUpdateCheck(entity,true);
            entity.setId(RandomUtil.uuId());
            this.save(entity);
            configService.create(configEntity);
        }catch (Exception e){
            throw new DataException(MsgCode.IMP003.get());
        }

    }
    public  void creUpdateCheck(VisualEntity entity, Boolean fullNameCheck) {
        String title = entity.getTitle();
        String systemId = entity.getSystemId();

        // 名称长度验证（假设长度限制为80）
        if (StringUtil.isNotEmpty(title)&&title.length() > 80) {
            throw new DataException(MsgCode.EXIST005.get());
        }

        // 动态构建查询条件
        LambdaQueryWrapper<VisualEntity> queryWrapper = new LambdaQueryWrapper<>();
        if (Boolean.TRUE.equals(fullNameCheck)) {
            queryWrapper.eq(VisualEntity::getTitle, title)
                    .eq(VisualEntity::getSystemId, systemId);
            List<VisualEntity> list = this.list(queryWrapper);
            if (!list.isEmpty()) {
                if (StringUtil.isNotEmpty(entity.getId())&&list.get(0).getId().equals(entity.getId())) {
                    return;
                }
                throw new DataException(MsgCode.EXIST003.get());
            }
        }
    }

    @Override
    public List<VisualEntity> getListBySystemId(String systemId) {
        QueryWrapper<VisualEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(VisualEntity::getSystemId, systemId);
        queryWrapper.lambda().orderByDesc(VisualEntity::getCreateTime);

        return this.list(queryWrapper);
    }
}
