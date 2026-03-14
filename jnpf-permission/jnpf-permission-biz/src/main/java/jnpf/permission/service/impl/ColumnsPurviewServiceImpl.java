package jnpf.permission.service.impl;

import jnpf.base.service.SuperServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.permission.entity.ColumnsPurviewEntity;
import jnpf.permission.mapper.ColumnsPurviewMapper;
import jnpf.permission.service.ColumnsPurviewService;
import jnpf.util.DateUtil;
import jnpf.util.RandomUtil;
import jnpf.util.UserProvider;
import org.springframework.stereotype.Service;

/**
 * 模块列表权限业务实现类
 *
 * @author ：JNPF开发平台组
 * @version: V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date ：2022/3/15 9:40
 */
@Service
public class ColumnsPurviewServiceImpl extends SuperServiceImpl<ColumnsPurviewMapper, ColumnsPurviewEntity> implements ColumnsPurviewService {

    @Override
    public ColumnsPurviewEntity getInfo(String moduleId) {
        QueryWrapper<ColumnsPurviewEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ColumnsPurviewEntity::getModuleId, moduleId);
        return this.getOne(queryWrapper);
    }

    @Override
    public boolean update(String moduleId, ColumnsPurviewEntity entity) {
        ColumnsPurviewEntity entitys = getInfo(moduleId);
        // id不存在则是保存
        if (entitys == null) {
            entity.setId(RandomUtil.uuId());
            entity.setCreatorUserId(UserProvider.getUser().getUserId());
            return this.save(entity);
        } else {
            // 修改
            entity.setId(entitys.getId());
            entity.setLastModifyUserId(UserProvider.getUser().getUserId());
            entity.setLastModifyTime(DateUtil.getNowDate());
        }
        return this.saveOrUpdate(entity);
    }

}
