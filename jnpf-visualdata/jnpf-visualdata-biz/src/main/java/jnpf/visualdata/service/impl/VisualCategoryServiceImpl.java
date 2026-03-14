package jnpf.visualdata.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.druid.util.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jnpf.base.service.SuperServiceImpl;
import jnpf.constant.MsgCode;
import jnpf.exception.DataException;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;
import jnpf.visualdata.entity.VisualCategoryEntity;
import jnpf.visualdata.mapper.VisualCategoryMapper;
import jnpf.visualdata.model.visual.VisualPaginationModel;
import jnpf.visualdata.service.VisualCategoryService;
import jnpf.visualdata.service.VisualService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 大屏分类
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021年6月15日
 */
@Service
@RequiredArgsConstructor
public class VisualCategoryServiceImpl extends SuperServiceImpl<VisualCategoryMapper, VisualCategoryEntity> implements VisualCategoryService {

    private final VisualService visualService;

    @Override
    public List getList(VisualPaginationModel pagination, boolean isPage) {
        QueryWrapper<VisualCategoryEntity> queryWrapper = new QueryWrapper<>();
        if (ObjectUtil.isNotEmpty(pagination.getCategoryValue())) {
            queryWrapper.lambda().like(VisualCategoryEntity::getCategorykey, pagination.getCategoryValue());
        }
        queryWrapper.lambda().eq(VisualCategoryEntity::getSystemId, visualService.getSystemIdByReq());
        queryWrapper.lambda().orderByAsc(VisualCategoryEntity::getCategorykey);
        if (isPage) {
            Page<VisualCategoryEntity> page = new Page<>(pagination.getCurrent(), pagination.getSize());
            Page<VisualCategoryEntity> iPages = this.page(page, queryWrapper);
            return pagination.setData(iPages);
        }
        return this.list(queryWrapper);
    }

    @Override
    public List<VisualCategoryEntity> getList() {
        QueryWrapper<VisualCategoryEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(VisualCategoryEntity::getSystemId, visualService.getSystemIdByReq());
        return this.list(queryWrapper);
    }

    @Override
    public boolean isExistByValue(String value, String id, String systemId) {
        QueryWrapper<VisualCategoryEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(VisualCategoryEntity::getCategoryvalue, value);
        if (!StringUtils.isEmpty(id)) {
            queryWrapper.lambda().ne(VisualCategoryEntity::getId, id);
        }
        if (!StringUtils.isEmpty(systemId)) {
            queryWrapper.lambda().eq(VisualCategoryEntity::getSystemId, systemId);
        }
        return this.count(queryWrapper) > 0;
    }

    @Override
    public VisualCategoryEntity getInfo(String id) {
        QueryWrapper<VisualCategoryEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(VisualCategoryEntity::getId, id);
        return this.getOne(queryWrapper);
    }

    @Override
    public void create(VisualCategoryEntity entity) {
        entity.setSystemId(visualService.getSystemIdByReq());
        this.creUpdateCheck(entity, true);
        entity.setId(RandomUtil.uuId());
        this.save(entity);
    }

    @Override
    public boolean update(String id, VisualCategoryEntity entity) {
        entity.setSystemId(visualService.getSystemIdByReq());
        this.creUpdateCheck(entity, true);
        entity.setId(id);
        return this.updateById(entity);
    }

    @Override
    public void delete(VisualCategoryEntity entity) {
        if (entity != null) {
            this.removeById(entity.getId());
        }
    }

    public void creUpdateCheck(VisualCategoryEntity entity, Boolean fullNameCheck) {
        String title = entity.getCategorykey();
        String systemId = entity.getSystemId();

        // 名称长度验证（假设长度限制为80）
        if (StringUtil.isNotEmpty(title) && title.length() > 80) {
            throw new DataException(MsgCode.EXIST005.get());
        }

        // 动态构建查询条件
        LambdaQueryWrapper<VisualCategoryEntity> queryWrapper = new LambdaQueryWrapper<>();
        if (Boolean.TRUE.equals(fullNameCheck)) {
            queryWrapper.eq(VisualCategoryEntity::getCategorykey, title)
                    .eq(VisualCategoryEntity::getSystemId, systemId);
            List<VisualCategoryEntity> list = this.list(queryWrapper);
            if (!list.isEmpty()) {
                if (StringUtil.isNotEmpty(entity.getId()) && list.get(0).getId().equals(entity.getId())) {
                    return;
                }
                throw new DataException(MsgCode.EXIST003.get());
            }
        }
    }

    @Override
    public List<VisualCategoryEntity> getListBySystemId(String systemId) {
        QueryWrapper<VisualCategoryEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(VisualCategoryEntity::getSystemId, systemId);
        return this.list(queryWrapper);
    }

    @Override
    public void deleteBySystemId(String systemId) {
        QueryWrapper<VisualCategoryEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(VisualCategoryEntity::getSystemId, systemId);
        this.baseMapper.deleteByIds(this.list(queryWrapper));
    }
}
