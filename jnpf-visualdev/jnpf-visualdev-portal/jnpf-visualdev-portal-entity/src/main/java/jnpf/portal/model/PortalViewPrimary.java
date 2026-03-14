package jnpf.portal.model;

import jnpf.base.MyBatisPrimaryBase;
import jnpf.base.UserInfo;
import jnpf.constant.JnpfConst;
import jnpf.portal.entity.PortalEntity;
import jnpf.util.UserProvider;
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
public class PortalViewPrimary extends MyBatisPrimaryBase<PortalEntity> {

    private String creatorId;

    private String portalId;

    private String platForm = JnpfConst.WEB;

    private String systemId;

    public PortalViewPrimary(String platForm, String portalId) {
        if (platForm != null) this.platForm = platForm;
        this.portalId = portalId;
        UserInfo userInfo = UserProvider.getUser();
        this.systemId = JnpfConst.WEB.equals(platForm) ? userInfo.getSystemId() : userInfo.getAppSystemId();
        this.creatorId = userInfo.getId();
    }

}
