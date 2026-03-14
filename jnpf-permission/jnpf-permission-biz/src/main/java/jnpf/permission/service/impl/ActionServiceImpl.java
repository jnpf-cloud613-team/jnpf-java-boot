package jnpf.permission.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jnpf.base.service.SuperServiceImpl;
import jnpf.permission.entity.ActionEntity;
import jnpf.permission.mapper.ActionMapper;
import jnpf.permission.model.action.ActionPagination;
import jnpf.permission.service.ActionService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ActionServiceImpl extends SuperServiceImpl<ActionMapper, ActionEntity>
        implements ActionService {

    @Override
    public Boolean insertOrUpdate(ActionEntity actionEntity) {
        if (actionEntity.getType() == null || actionEntity.getId() == null) {
            //新建是自定义
            actionEntity.setType(1);
        }
        //判重
        LambdaQueryWrapper<ActionEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ActionEntity::getEnCode, actionEntity.getEnCode());
        ActionEntity one = this.getOne(queryWrapper);
        if (BeanUtil.isNotEmpty(one)&&!actionEntity.getId().equals(one.getId())) {
            return false;
        }
        return this.saveOrUpdate(actionEntity);
    }

    @Override
    public Boolean deleteById(String actionId) {
        ActionEntity byId = this.getById(actionId);
        if (byId == null) {
            return false;
        }
        return this.removeById(actionId);
    }

    @Override
    public List<ActionEntity> getActionList(ActionPagination actionPagination) {
        if (actionPagination == null) {
            return new ArrayList<>();
        }
        QueryWrapper<ActionEntity> queryWrapper = new QueryWrapper<>();
        String keyword = actionPagination.getKeyword();
        if (StringUtils.isNotBlank(keyword)) {
            queryWrapper.lambda().like(ActionEntity::getFullName, actionPagination.getKeyword())
                    .or().like(ActionEntity::getEnCode, actionPagination.getKeyword())
                    .or().like(ActionEntity::getDescription, actionPagination.getKeyword());
        }
        if (actionPagination.getType() != null) {
            queryWrapper.lambda().eq(ActionEntity::getType, actionPagination.getType());
        }
        queryWrapper.lambda().orderByAsc(ActionEntity::getId);
        Page<ActionEntity> page = new Page<>(actionPagination.getCurrentPage(), actionPagination.getPageSize());
        Page<ActionEntity> actionEntityPage = this.page(page, queryWrapper);
        return actionPagination.setData(actionEntityPage.getRecords(), page.getTotal());

    }
}
