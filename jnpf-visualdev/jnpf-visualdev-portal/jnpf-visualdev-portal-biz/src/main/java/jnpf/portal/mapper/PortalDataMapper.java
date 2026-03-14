package jnpf.portal.mapper;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.MyBatisPrimaryBase;
import jnpf.base.mapper.SuperMapper;
import jnpf.constant.MsgCode;
import jnpf.exception.DataException;
import jnpf.portal.entity.PortalDataEntity;
import jnpf.portal.model.PortalCustomPrimary;
import jnpf.portal.model.PortalModPrimary;
import jnpf.portal.model.PortalReleasePrimary;
import jnpf.util.DateUtil;

import java.util.List;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author YanYu
 * @since 2023-04-19
 */
public interface PortalDataMapper extends SuperMapper<PortalDataEntity> {

    default String getModelDataForm(PortalModPrimary primary) throws IllegalAccessException {
        PortalDataEntity one = selectOne(primary.getQuery());
        if (one != null) {
            return one.getFormData();
        } else {
            insert(primary.getEntity());
        }
        return "";
    }

    default void deleteAll(String portalId) {
        QueryWrapper<PortalDataEntity> query = new QueryWrapper<>();
        query.lambda().eq(PortalDataEntity::getPortalId, portalId);
        this.deleteByIds(this.selectList(query));
    }

    /**
     * 创建或更新
     * <p>
     * 门户ID ->（平台、系统ID、用户ID）-> 排版信息
     * 基础：门户ID绑定排版信息（一对多）、 条件：平台、系统ID、用户ID
     */
    default void createOrUpdate(PortalCustomPrimary primary, String formData) throws IllegalAccessException {
        creUpCom(primary, formData);
    }

    default void createOrUpdate(PortalModPrimary primary, String formData) throws IllegalAccessException {
        creUpCom(primary, formData);
    }

    default void createOrUpdate(PortalReleasePrimary primary, String formData) throws IllegalAccessException {
        creUpCom(primary, formData);
    }

    default void creUpCom(MyBatisPrimaryBase<PortalDataEntity> primary, String formData) throws IllegalAccessException {
        // 自定义数据变量条件：0、门户 1、用户 2、系统 3、平台
        List<PortalDataEntity> list = this.selectList(primary.getQuery());
        if (list.isEmpty()) {
            PortalDataEntity creEntity = primary.getEntity();
            creEntity.setFormData(formData);
            insert(creEntity);
        } else if (list.size() == 1) {
            PortalDataEntity upEntity = list.get(0);
            upEntity.setFormData(formData);
            upEntity.setLastModifyTime(DateUtil.getNowDate());
            updateById(upEntity);
        } else {
            throw new DataException(MsgCode.VS414.get());
        }
    }
}
