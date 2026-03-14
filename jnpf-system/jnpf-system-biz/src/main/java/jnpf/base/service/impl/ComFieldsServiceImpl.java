package jnpf.base.service.impl;

import jnpf.base.service.SuperServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.entity.ComFieldsEntity;
import jnpf.base.mapper.BaseComFieldsMapper;
import jnpf.base.service.ComFieldsService;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/12 15:31
 */
@Service
public class ComFieldsServiceImpl extends SuperServiceImpl<BaseComFieldsMapper, ComFieldsEntity> implements ComFieldsService {

    @Override
    public List<ComFieldsEntity> getList() {
        QueryWrapper<ComFieldsEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().orderByAsc(ComFieldsEntity::getSortCode).orderByDesc(ComFieldsEntity::getCreatorTime);
        return this.list(queryWrapper);
    }

    @Override
    public ComFieldsEntity getInfo(String id) {
        QueryWrapper<ComFieldsEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ComFieldsEntity::getId, id);
        return this.getOne(queryWrapper);
    }

    @Override
    public boolean isExistByFullName(String fullName, String id) {
        QueryWrapper<ComFieldsEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ComFieldsEntity::getFieldName, fullName);
        if (!StringUtil.isEmpty(id)) {
            queryWrapper.lambda().ne(ComFieldsEntity::getId, id);
        }
        return this.count(queryWrapper) > 0;
    }



    @Override
    public void create(ComFieldsEntity entity) {
        entity.setId(RandomUtil.uuId());
        entity.setCreatorTime(new Date());
        entity.setCreatorUserId(UserProvider.getUser().getUserId());
        entity.setEnabledMark(1);
        this.save(entity);
    }

    @Override
    public boolean update(String id, ComFieldsEntity entity) {
        entity.setId(id);
        entity.setLastModifyTime(new Date());
        entity.setLastModifyUserId(UserProvider.getUser().getUserId());
        return this.updateById(entity);
    }

    @Override
    public void delete(ComFieldsEntity entity) {
        if (entity != null) {
            this.removeById(entity.getId());
        }
    }

}
