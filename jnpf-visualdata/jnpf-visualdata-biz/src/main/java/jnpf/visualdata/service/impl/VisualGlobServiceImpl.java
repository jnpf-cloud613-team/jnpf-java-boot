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
import jnpf.visualdata.entity.VisualGlobEntity;
import jnpf.visualdata.mapper.VisualGlobMapper;
import jnpf.visualdata.model.visual.VisualPaginationModel;
import jnpf.visualdata.service.VisualGlobService;
import jnpf.visualdata.service.VisualService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 全局变量
 *
 * @author JNPF开发平台组
 * @version V3.5.0
 * @copyright 引迈信息技术有限公司
 * @date 2023年7月7日
 */
@Service
@RequiredArgsConstructor
public class VisualGlobServiceImpl extends SuperServiceImpl<VisualGlobMapper, VisualGlobEntity> implements VisualGlobService {


    private final VisualService visualService;

    @Override
    public List<VisualGlobEntity> getList(VisualPaginationModel pagination) {
        QueryWrapper<VisualGlobEntity> queryWrapper = new QueryWrapper<>();
        if(ObjectUtil.isNotEmpty(pagination.getGlobalName())){
            queryWrapper.lambda().like(VisualGlobEntity::getGlobalName, pagination.getGlobalName());
        }
        queryWrapper.lambda().eq(VisualGlobEntity::getSystemId,visualService.getSystemIdByReq());
        Page<VisualGlobEntity> page = new Page<>(pagination.getCurrent(), pagination.getSize());
        IPage<VisualGlobEntity> iPages = this.page(page, queryWrapper);
        return pagination.setData(iPages);
    }

    @Override
    public List<VisualGlobEntity> getList() {
        QueryWrapper<VisualGlobEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(VisualGlobEntity::getSystemId,visualService.getSystemIdByReq());
        return this.list(queryWrapper);
    }

    @Override
    public VisualGlobEntity getInfo(String id) {
        QueryWrapper<VisualGlobEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(VisualGlobEntity::getId, id);
        return this.getOne(queryWrapper);
    }

    @Override
    public void create(VisualGlobEntity entity) {
        entity.setSystemId(visualService.getSystemIdByReq());
        entity.setId(RandomUtil.uuId());
        this.creUpdateCheck(entity,true);
        this.save(entity);
    }

    @Override
    public boolean update(String id, VisualGlobEntity entity) {
        entity.setSystemId(visualService.getSystemIdByReq());
        entity.setId(id);
        this.creUpdateCheck(entity,true);
        return this.updateById(entity);
    }

    @Override
    public void delete(VisualGlobEntity entity) {
        if (entity != null) {
            this.removeById(entity.getId());
        }
    }

    public  void creUpdateCheck(VisualGlobEntity entity, Boolean fullNameCheck) {
        String title = entity.getGlobalName();
        String systemId = entity.getSystemId();

        // 名称长度验证（假设长度限制为80）
        if (StringUtil.isNotEmpty(title)&&title.length() > 80) {
            throw new DataException(MsgCode.EXIST005.get());
        }

        // 动态构建查询条件
        LambdaQueryWrapper<VisualGlobEntity> queryWrapper = new LambdaQueryWrapper<>();
        if (Boolean.TRUE.equals(fullNameCheck)) {
            queryWrapper.eq(VisualGlobEntity::getGlobalName, title)
                    .eq(VisualGlobEntity::getSystemId, systemId);
            List<VisualGlobEntity> list = this.list(queryWrapper);
            if (!list.isEmpty()) {
                if (StringUtil.isNotEmpty(entity.getId())&&list.get(0).getId().equals(entity.getId())) {
                    return;
                }
                throw new DataException(MsgCode.EXIST003.get());
            }
        }
    }


}
