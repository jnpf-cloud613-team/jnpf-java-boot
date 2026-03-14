package jnpf.flowable.mapper;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.mapper.SuperMapper;
import jnpf.constant.MsgCode;
import jnpf.exception.WorkFlowException;
import jnpf.flowable.entity.TemplateJsonEntity;
import jnpf.flowable.enums.TemplateJsonStatueEnum;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public interface TemplateJsonMapper extends SuperMapper<TemplateJsonEntity> {

    default List<TemplateJsonEntity> getListByTemplateIds(List<String> id) {
        List<TemplateJsonEntity> list = new ArrayList<>();
        if (!id.isEmpty()) {
            QueryWrapper<TemplateJsonEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().in(TemplateJsonEntity::getTemplateId, id);
            list.addAll(this.selectList(queryWrapper));
        }
        return list;
    }

    default List<TemplateJsonEntity> getList(String templateId) {
        QueryWrapper<TemplateJsonEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(TemplateJsonEntity::getTemplateId, templateId);
        List<TemplateJsonEntity> list = this.selectList(queryWrapper);

        List<TemplateJsonEntity> resList = new ArrayList<>();
        if (CollUtil.isNotEmpty(list)) {
            // 启用中
            List<TemplateJsonEntity> enableList = list.stream().filter(e -> ObjectUtil.equals(e.getState(), TemplateJsonStatueEnum.START.getCode()))
                    .sorted(Comparator.comparing(TemplateJsonEntity::getCreatorTime).reversed()).collect(Collectors.toList());
            // 已归档
            List<TemplateJsonEntity> historicList = list.stream().filter(e -> ObjectUtil.equals(e.getState(), TemplateJsonStatueEnum.HISTORY.getCode()))
                    .sorted(Comparator.comparing(TemplateJsonEntity::getCreatorTime).reversed()).collect(Collectors.toList());
            // 设计中
            List<TemplateJsonEntity> designList = list.stream().filter(e -> ObjectUtil.equals(e.getState(), TemplateJsonStatueEnum.DESIGN.getCode()))
                    .sorted(Comparator.comparing(TemplateJsonEntity::getCreatorTime).reversed()).collect(Collectors.toList());

            resList.addAll(enableList);
            resList.addAll(historicList);
            resList.addAll(designList);
        }

        return resList;
    }

    default List<TemplateJsonEntity> getListOfEnable() {
        QueryWrapper<TemplateJsonEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(TemplateJsonEntity::getState, TemplateJsonStatueEnum.START.getCode());
        return this.selectList(queryWrapper);
    }

    default TemplateJsonEntity getInfo(String id) throws WorkFlowException {
        QueryWrapper<TemplateJsonEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(TemplateJsonEntity::getId, id);
        TemplateJsonEntity jsonEntity = this.selectOne(queryWrapper);
        if (jsonEntity == null) {
            throw new WorkFlowException(MsgCode.FA001.get());
        }
        return jsonEntity;
    }

    default boolean update(String id, TemplateJsonEntity entity) {
        entity.setId(id);
        return this.updateById(entity) > 0;
    }

    default void delete(List<String> idList) {
        if (!idList.isEmpty()) {
            this.deleteByIds(idList);
        }
    }

}
