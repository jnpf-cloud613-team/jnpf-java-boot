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
import jnpf.visualdata.entity.VisualRecordEntity;
import jnpf.visualdata.mapper.VisualRecordMapper;
import jnpf.visualdata.model.visual.VisualPaginationModel;
import jnpf.visualdata.service.VisualRecordService;
import jnpf.visualdata.service.VisualService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 大屏数据集
 *
 * @author JNPF开发平台组
 * @version V3.5.0
 * @copyright 引迈信息技术有限公司
 * @date 2023年7月7日
 */
@Service
@RequiredArgsConstructor
public class VisualRecordServiceImpl extends SuperServiceImpl<VisualRecordMapper, VisualRecordEntity> implements VisualRecordService {


    private final VisualService visualService;

    @Override
    public List<VisualRecordEntity> getList(VisualPaginationModel pagination) {
        QueryWrapper<VisualRecordEntity> queryWrapper = new QueryWrapper<>();
        if(ObjectUtil.isNotEmpty(pagination.getName())){
            queryWrapper.lambda().like(VisualRecordEntity::getName, pagination.getName());
        }
        queryWrapper.lambda().eq(VisualRecordEntity::getSystemId, visualService.getSystemIdByReq());
        Page<VisualRecordEntity> page = new Page<>(pagination.getCurrent(), pagination.getSize());
        IPage<VisualRecordEntity> iPages = this.page(page, queryWrapper);
        return pagination.setData(iPages);
    }

    @Override
    public List<VisualRecordEntity> getList() {
        QueryWrapper<VisualRecordEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(VisualRecordEntity::getSystemId, visualService.getSystemIdByReq());
        return this.list(queryWrapper);
    }

    @Override
    public VisualRecordEntity getInfo(String id) {
        QueryWrapper<VisualRecordEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(VisualRecordEntity::getId, id);
        return this.getOne(queryWrapper);
    }

    @Override
    public void create(VisualRecordEntity entity) {
        entity.setSystemId(visualService.getSystemIdByReq());
        entity.setId(RandomUtil.uuId());
        this.creUpdateCheck(entity,true);
        this.save(entity);
    }

    @Override
    public boolean update(String id, VisualRecordEntity entity) {
        entity.setSystemId(visualService.getSystemIdByReq());
        entity.setId(id);
        this.creUpdateCheck(entity,true);
        return this.updateById(entity);
    }

    @Override
    public void delete(VisualRecordEntity entity) {
        if (entity != null) {
            this.removeById(entity.getId());
        }
    }

    public  void creUpdateCheck(VisualRecordEntity entity, Boolean fullNameCheck) {
        String title = entity.getName();
        String systemId = entity.getSystemId();

        // 名称长度验证（假设长度限制为80）
        if (StringUtil.isNotEmpty(title)&&title.length() > 80) {
            throw new DataException(MsgCode.EXIST005.get());
        }

        // 动态构建查询条件
        LambdaQueryWrapper<VisualRecordEntity> queryWrapper = new LambdaQueryWrapper<>();
        if (Boolean.TRUE.equals(fullNameCheck)) {
            queryWrapper.eq(VisualRecordEntity::getName, title)
                    .eq(VisualRecordEntity::getSystemId, systemId);
            List<VisualRecordEntity> list = this.list(queryWrapper);
            if (!list.isEmpty()) {
                if (StringUtil.isNotEmpty(entity.getId())&&list.get(0).getId().equals(entity.getId())) {
                    return;
                }
                throw new DataException(MsgCode.EXIST003.get());
            }
        }
    }


}
