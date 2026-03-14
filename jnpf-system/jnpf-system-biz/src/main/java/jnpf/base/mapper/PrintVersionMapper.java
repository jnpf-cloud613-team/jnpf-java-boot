package jnpf.base.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.entity.PrintVersionEntity;
import jnpf.util.RandomUtil;
import jnpf.util.UserProvider;

import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * @author JNPF开发平台组
 * @version v5.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/5/6 14:07:21
 */
public interface PrintVersionMapper extends SuperMapper<PrintVersionEntity> {

    default void create(PrintVersionEntity entity) {
        String versionId = RandomUtil.uuId();
        entity.setId(versionId);
        entity.setCreatorUserId(UserProvider.getUser().getUserId());
        entity.setCreatorTime(new Date());
        List<PrintVersionEntity> verList = getList(entity.getId());
        int version = verList.stream().map(PrintVersionEntity::getVersion).max(Comparator.naturalOrder()).orElse(0) + 1;
        entity.setVersion(version);
        entity.setState(0);
        entity.setSortCode(0l);
        this.insert(entity);
    }

    default List<PrintVersionEntity> getList(String templateId) {
        QueryWrapper<PrintVersionEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(PrintVersionEntity::getTemplateId, templateId);
        queryWrapper.lambda().orderByDesc(PrintVersionEntity::getSortCode).orderByAsc(PrintVersionEntity::getState);
        return this.selectList(queryWrapper);
    }

}
