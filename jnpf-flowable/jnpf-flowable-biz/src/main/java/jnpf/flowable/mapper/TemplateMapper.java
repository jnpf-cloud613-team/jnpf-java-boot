package jnpf.flowable.mapper;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.ImmutableList;
import jnpf.base.mapper.SuperMapper;
import jnpf.constant.MsgCode;
import jnpf.exception.WorkFlowException;
import jnpf.flowable.entity.TemplateEntity;
import jnpf.flowable.enums.TemplateStatueEnum;
import jnpf.flowable.model.template.TemplatePagination;
import jnpf.flowable.model.util.FlowNature;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public interface TemplateMapper extends SuperMapper<TemplateEntity> {


    default List<TemplateEntity> getListByCreUser(String creUser) {
        QueryWrapper<TemplateEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(TemplateEntity::getVisibleType, FlowNature.AUTHORITY);
        queryWrapper.lambda().eq(TemplateEntity::getCreatorUserId, creUser);
        return this.selectList(queryWrapper);
    }

    default List<TemplateEntity> getList(List<String> ids) {
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }
        QueryWrapper<TemplateEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(TemplateEntity::getId, ids).eq(TemplateEntity::getStatus, TemplateStatueEnum.UP.getCode());
        return this.selectList(queryWrapper);
    }

    default List<TemplateEntity> getListByIds(List<String> ids) {
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }
        QueryWrapper<TemplateEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(TemplateEntity::getId, ids);
        return this.selectList(queryWrapper);
    }

    default List<TemplateEntity> getListOfHidden(List<String> ids) {
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }
        QueryWrapper<TemplateEntity> queryWrapper = new QueryWrapper<>();
        List<Integer> statusList = ImmutableList.of(TemplateStatueEnum.UP.getCode(), TemplateStatueEnum.DOWN_CONTINUE.getCode());
        queryWrapper.lambda().in(TemplateEntity::getId, ids).in(TemplateEntity::getStatus, statusList);
        return this.selectList(queryWrapper);
    }

    default List<TemplateEntity> getList(TemplatePagination pagination) {
        QueryWrapper<TemplateEntity> queryWrapper = new QueryWrapper<>();
        String keyword = pagination.getKeyword();
        if (ObjectUtil.isNotEmpty(keyword)) {
            queryWrapper.lambda().and(t -> t.like(TemplateEntity::getEnCode, keyword).or().like(TemplateEntity::getFullName, keyword));
        }
        String category = pagination.getCategory();
        if (ObjectUtil.isNotEmpty(category)) {
            queryWrapper.lambda().eq(TemplateEntity::getCategory, category);
        }
        Integer type = pagination.getType();
        if (ObjectUtil.isNotEmpty(type)) {
            queryWrapper.lambda().eq(TemplateEntity::getType, type);
        }
        Integer enabledMark = pagination.getEnabledMark();
        if (ObjectUtil.isNotEmpty(enabledMark)) {
            queryWrapper.lambda().eq(TemplateEntity::getEnabledMark, enabledMark);
        }
        String systemId = pagination.getSystemId();
        if (ObjectUtil.isNotEmpty(systemId)) {
            queryWrapper.lambda().eq(TemplateEntity::getSystemId, systemId);
        }
        queryWrapper.lambda().orderByAsc(TemplateEntity::getSortCode).orderByDesc(TemplateEntity::getCreatorTime);
        Page<TemplateEntity> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
        IPage<TemplateEntity> userPage = this.selectPage(page, queryWrapper);
        return pagination.setData(userPage.getRecords(), page.getTotal());
    }

    default TemplateEntity getInfo(String id) throws WorkFlowException {
        QueryWrapper<TemplateEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(TemplateEntity::getId, id);
        TemplateEntity templateEntity = this.selectOne(queryWrapper);
        if (templateEntity == null) {
            throw new WorkFlowException(MsgCode.WF122.get());
        }
        return templateEntity;
    }

    default boolean isExistByFullName(String fullName, String id, String systemId) {
        QueryWrapper<TemplateEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(TemplateEntity::getFullName, fullName);
        if (!StringUtils.isEmpty(id)) {
            queryWrapper.lambda().ne(TemplateEntity::getId, id);
        }
        if (!StringUtils.isEmpty(systemId)) {
            queryWrapper.lambda().eq(TemplateEntity::getSystemId, systemId);
        }
        return this.selectCount(queryWrapper) > 0;
    }

    default boolean isExistByEnCode(String enCode, String id, String systemId) {
        QueryWrapper<TemplateEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(TemplateEntity::getEnCode, enCode);
        if (!StringUtils.isEmpty(id)) {
            queryWrapper.lambda().ne(TemplateEntity::getId, id);
        }
        if (!StringUtils.isEmpty(systemId)) {
            queryWrapper.lambda().eq(TemplateEntity::getSystemId, systemId);
        }
        return this.selectCount(queryWrapper) > 0;
    }

    default boolean update(String id, TemplateEntity entity) throws WorkFlowException {
        if (isExistByFullName(entity.getFullName(), id, entity.getSystemId())) {
            throw new WorkFlowException(MsgCode.EXIST001.get());
        }
        if (isExistByEnCode(entity.getEnCode(), id, entity.getSystemId())) {
            throw new WorkFlowException(MsgCode.EXIST002.get());
        }
        entity.setId(id);
        return this.updateById(entity)>0;
    }

    default List<TemplateEntity> getListBySystemId(String systemId) {
        QueryWrapper<TemplateEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(TemplateEntity::getSystemId, systemId);
        return this.selectList(queryWrapper);
    }
}
