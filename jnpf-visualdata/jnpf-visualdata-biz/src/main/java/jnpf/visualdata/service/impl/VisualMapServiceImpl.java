package jnpf.visualdata.service.impl;

import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import jnpf.base.service.SuperServiceImpl;
import jnpf.constant.MsgCode;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;
import jnpf.visualdata.entity.VisualMapEntity;
import jnpf.visualdata.mapper.VisualMapMapper;
import jnpf.visualdata.model.visual.VisualPaginationModel;
import jnpf.visualdata.service.VisualMapService;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 大屏地图配置
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021年6月15日
 */
@Service
public class VisualMapServiceImpl extends SuperServiceImpl<VisualMapMapper, VisualMapEntity> implements VisualMapService {

    @Override
    public List<VisualMapEntity> getList(VisualPaginationModel pagination) {
        return getListWithColnums(pagination);
    }


    @Override
    public List<VisualMapEntity> getListWithColnums(VisualPaginationModel pagination, SFunction<VisualMapEntity, ?>... columns) {
        if(StringUtil.isEmpty(pagination.getParentId())) {
            return Collections.emptyList();
        }
        QueryWrapper<VisualMapEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .select(columns)
                .eq(VisualMapEntity::getParentId, pagination.getParentId())
                .orderByAsc(VisualMapEntity::getId);
        if(ObjectUtil.isNotEmpty(pagination.getName())){
            queryWrapper.lambda().like(VisualMapEntity::getName, pagination.getName());
        }
        return this.list(queryWrapper);
    }

    @Override
    public VisualMapEntity getInfo(String id) {
        if(StringUtil.isEmpty(id)) {
            return null;
        }
        QueryWrapper<VisualMapEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(VisualMapEntity::getId, id);
        return this.getOne(queryWrapper);
    }

    @Override
    public void create(VisualMapEntity entity) {
        entity.setId(RandomUtil.uuId());
        VisualMapEntity parent = getByCode(entity.getCode());
        Assert.isNull(parent, MsgCode.EXIST002::get);
        parent = getInfo(entity.getParentId());
        if(parent != null){
            entity.setParentCode(parent.getCode());
            entity.setAncestors(parent.getAncestors() + StrPool.COMMA + parent.getCode());
        }
        this.save(entity);
    }

    @Override
    public boolean update(String id, VisualMapEntity entity) {
        VisualMapEntity parent = getByCode(entity.getCode());
        Assert.isTrue(parent == null || Objects.equals(parent.getId(), id), MsgCode.EXIST002::get);
        entity.setId(id);
        return this.updateById(entity);
    }

    @Override
    public void delete(VisualMapEntity entity) {
        if (entity != null) {
            this.removeById(entity.getId());
        }
    }

    @Override
    public boolean hasChild(String id) {
        if(StringUtil.isEmpty(id)) {
            return false;
        }
        QueryWrapper<VisualMapEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(VisualMapEntity::getParentId, id);
        return this.exists(queryWrapper);
    }

    private VisualMapEntity getByCode(String code){
        if(StringUtil.isNotEmpty(code)){
            QueryWrapper<VisualMapEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().eq(VisualMapEntity::getCode, code);
            return getBaseMapper().selectOne(queryWrapper);
        }
        return null;
    }
}
