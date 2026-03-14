package jnpf.visualdata.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jnpf.base.service.SuperServiceImpl;
import jnpf.constant.MsgCode;
import jnpf.exception.DataException;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;
import jnpf.visualdata.entity.VisualAssetsEntity;
import jnpf.visualdata.mapper.VisualAssetsMapper;
import jnpf.visualdata.model.visual.VisualPaginationModel;
import jnpf.visualdata.service.VisualAssetsService;
import jnpf.visualdata.service.VisualService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 静态资源
 *
 * @author JNPF开发平台组
 * @version V3.5.0
 * @copyright 引迈信息技术有限公司
 * @date 2023年7月7日
 */
@Service
@RequiredArgsConstructor
public class VisualAssetsServiceImpl extends SuperServiceImpl<VisualAssetsMapper, VisualAssetsEntity> implements VisualAssetsService {


    private final VisualService visualService;
    @Override
    public List getList(VisualPaginationModel pagination) {
        QueryWrapper<VisualAssetsEntity> queryWrapper = new QueryWrapper<>();
        if(ObjectUtil.isNotEmpty(pagination.getAssetsName())){
            queryWrapper.lambda().like(VisualAssetsEntity::getAssetsName, pagination.getAssetsName());
        }
        queryWrapper.lambda().eq(VisualAssetsEntity::getSystemId,visualService.getSystemIdByReq());
        Page<VisualAssetsEntity> page = new Page<>(pagination.getCurrent(), pagination.getSize());
        Page<VisualAssetsEntity> iPages = this.page(page, queryWrapper);
        return pagination.setData(iPages);
    }

    @Override
    public List<VisualAssetsEntity> getList() {
        QueryWrapper<VisualAssetsEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(VisualAssetsEntity::getSystemId,visualService.getSystemIdByReq());
        return this.list(queryWrapper);
    }

    @Override
    public VisualAssetsEntity getInfo(String id) {
        QueryWrapper<VisualAssetsEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(VisualAssetsEntity::getId, id);
        return this.getOne(queryWrapper);
    }

    @Override
    public void create(VisualAssetsEntity entity) {
        entity.setSystemId(visualService.getSystemIdByReq());
        this.creUpdateCheck(entity,true);
        entity.setId(RandomUtil.uuId());
        this.save(entity);
    }

    @Override
    public boolean update(String id, VisualAssetsEntity entity) {
        entity.setSystemId(visualService.getSystemIdByReq());
        this.creUpdateCheck(entity,true);
        entity.setId(id);
        return this.updateById(entity);
    }

    @Override
    public boolean delete(String id) {
        if (StringUtil.isNotEmpty(id)) {
            return this.removeById(id);
        }
        return false;
    }

    public  void creUpdateCheck(VisualAssetsEntity entity, Boolean fullNameCheck) {
        String title = entity.getAssetsName();
        String systemId = entity.getSystemId();

        // 名称长度验证（假设长度限制为80）
        if (StringUtil.isNotEmpty(title)&&title.length() > 80) {
            throw new DataException(MsgCode.EXIST005.get());
        }

        // 动态构建查询条件
        LambdaQueryWrapper<VisualAssetsEntity> queryWrapper = new LambdaQueryWrapper<>();
        if (Boolean.TRUE.equals(fullNameCheck)) {
            queryWrapper.eq(VisualAssetsEntity::getAssetsName, title)
                    .eq(VisualAssetsEntity::getSystemId, systemId);
            List<VisualAssetsEntity> list = this.list(queryWrapper);
            if (!list.isEmpty()) {
                if (StringUtil.isNotEmpty(entity.getId())&&list.get(0).getId().equals(entity.getId())) {
                    return;
                }
                throw new DataException(MsgCode.EXIST003.get());
            }
        }
    }
}
