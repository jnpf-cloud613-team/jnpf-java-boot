package jnpf.flowable.mapper;

import jnpf.base.mapper.SuperMapper;
import jnpf.flowable.entity.DelegateEntity;
import org.apache.commons.lang3.StringUtils;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/5/13 16:54
 */
public interface DelegateMapper extends SuperMapper<DelegateEntity> {

    default DelegateEntity getInfo(String id) {
        if (StringUtils.isBlank(id)) {
            return null;
        }
        return this.selectById(id);
    }

    default void delete(DelegateEntity entity) {
        this.deleteById(entity.getId());
    }
}
