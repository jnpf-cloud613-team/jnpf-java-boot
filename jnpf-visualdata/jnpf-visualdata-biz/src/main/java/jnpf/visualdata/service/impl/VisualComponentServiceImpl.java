package jnpf.visualdata.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jnpf.base.service.SuperServiceImpl;
import jnpf.constant.MsgCode;
import jnpf.exception.DataException;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;
import jnpf.visualdata.entity.VisualComponentEntity;
import jnpf.visualdata.mapper.VisualComponentMapper;
import jnpf.visualdata.model.visual.VisualPaginationModel;
import jnpf.visualdata.service.VisualComponentService;
import jnpf.visualdata.service.VisualService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 大屏组件库
 *
 * @author JNPF开发平台组
 * @version V3.5.0
 * @copyright 引迈信息技术有限公司
 * @date 2023年7月7日
 */
@Service
@RequiredArgsConstructor
public class VisualComponentServiceImpl extends SuperServiceImpl<VisualComponentMapper, VisualComponentEntity> implements VisualComponentService {


    private final VisualService visualService;
    @Override
    public List<VisualComponentEntity> getList(VisualPaginationModel pagination) {
        QueryWrapper<VisualComponentEntity> queryWrapper = new QueryWrapper<>();
        if(ObjectUtil.isNotEmpty(pagination.getName())){
            queryWrapper.lambda().like(VisualComponentEntity::getName, pagination.getName());
        }
        queryWrapper.lambda().eq(VisualComponentEntity::getSystemId,visualService.getSystemIdByReq());
        queryWrapper.lambda().eq(VisualComponentEntity::getType, pagination.getType());
        Page<VisualComponentEntity> page = new Page<>(pagination.getCurrent(), pagination.getSize());
        IPage<VisualComponentEntity> iPages = this.page(page, queryWrapper);
        return pagination.setData(iPages);
    }

    @Override
    public List<VisualComponentEntity> getList() {
        QueryWrapper<VisualComponentEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(VisualComponentEntity::getSystemId,visualService.getSystemIdByReq());
        return this.list(queryWrapper);
    }

    @Override
    public VisualComponentEntity getInfo(String id) {
        QueryWrapper<VisualComponentEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(VisualComponentEntity::getId, id);
        return this.getOne(queryWrapper);
    }

    @Override
    public void create(VisualComponentEntity entity) {
        entity.setSystemId(visualService.getSystemIdByReq());
        this.creUpdateCheck(entity,true);
        entity.setId(RandomUtil.uuId());
        this.save(entity);
    }

    @Override
    public boolean update(String id, VisualComponentEntity entity) {
        entity.setSystemId(visualService.getSystemIdByReq());
        this.creUpdateCheck(entity,true);
        entity.setId(id);
        return this.updateById(entity);
    }

    @Override
    public void delete(VisualComponentEntity entity) {
        if (entity != null) {
            this.removeById(entity.getId());
        }
    }

    public  void creUpdateCheck(VisualComponentEntity entity, Boolean fullNameCheck) {
        String title = entity.getName();
        String systemId = entity.getSystemId();

        // 名称长度验证（假设长度限制为80）
        if (StringUtil.isNotEmpty(title)&&title.length() > 80) {
            throw new DataException(MsgCode.EXIST005.get());
        }

        // 动态构建查询条件
        LambdaQueryWrapper<VisualComponentEntity> queryWrapper = new LambdaQueryWrapper<>();
        if (Boolean.TRUE.equals(fullNameCheck)) {
            queryWrapper.eq(VisualComponentEntity::getName, title)
                    .eq(VisualComponentEntity::getSystemId, systemId);
            List<VisualComponentEntity> list = this.list(queryWrapper);
            if (!list.isEmpty()) {
                if (StringUtil.isNotEmpty(entity.getId())&&list.get(0).getId().equals(entity.getId())) {
                    return;
                }
                throw new DataException(MsgCode.EXIST003.get());
            }
        }
    }


}
