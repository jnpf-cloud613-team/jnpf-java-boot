package jnpf.portal.model;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.MyBatisPrimaryBase;
import jnpf.portal.constant.PortalConst;
import jnpf.portal.entity.PortalDataEntity;
import lombok.Data;

/**
 * 类功能
 *
 * @author JNPF开发平台组 YanYu
 * @version v3.4.8
 * @copyrignt 引迈信息技术有限公司
 * @date 2023-04-21
 */
@Data
public class PortalModPrimary extends MyBatisPrimaryBase<PortalDataEntity> {

    /**
     * 门户ID
     */
    private String portalId;

    /**
     * 类型（model：模型、custom：自定义）
     */
    private String type = PortalConst.MODEL;

    public PortalModPrimary(String portalId){
        this.portalId = portalId;
    }

    @Override
    public QueryWrapper<PortalDataEntity> getQuery(){
        queryWrapper.lambda().eq(PortalDataEntity::getType, type);
        if(this.portalId != null) queryWrapper.lambda().eq(PortalDataEntity::getPortalId, portalId);
        return queryWrapper;
    }

}
