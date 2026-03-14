package jnpf.portal.service;

import jnpf.base.entity.SystemEntity;
import jnpf.base.service.SuperService;
import jnpf.exception.WorkFlowException;
import jnpf.permission.entity.UserEntity;
import jnpf.portal.entity.PortalDataEntity;
import jnpf.portal.model.*;

import java.util.List;

/**
 * 门户数据接口
 * <p>
 * 后期门户模板数据，将会以platform再做区分，
 * 故将formData抽离成单独的表
 *
 * @author YanYu
 * @since 2023-04-19
 */
public interface PortalDataService extends SuperService<PortalDataEntity> {


    String getModelDataForm(PortalModPrimary primary) throws IllegalAccessException;

    void releaseModule(ReleaseModel releaseModel, String portalId) throws IllegalAccessException, WorkFlowException;

    void deleteAll(String portalId);

    /**
     * 创建或更新门户自定义信息
     */
    void createOrUpdate(PortalCustomPrimary primary, String formData) throws IllegalAccessException;

    /**
     * 创建或更新门户模板信息
     */
    void createOrUpdate(PortalModPrimary primary, String formData) throws IllegalAccessException;

    /**
     * 创建或更新门户发布信息
     */
    void createOrUpdate(PortalReleasePrimary primary, String formData) throws IllegalAccessException;

    /**
     * 获取门户显示信息
     *
     * @param menuId   菜单id
     * @param platform 平台：app/pc
     */
    PortalInfoAuthVO getDataFormView(String menuId, String platform);

    /**
     * 设置默认门户
     */
    void setCurrentDefault(SystemEntity systemEntity, UserEntity userEntity, String platform, String portalId);

    /**
     * 获取当前系统默认门户
     *
     * @param systemId
     * @param userId
     * @param platform
     * @return
     */
    String getCurrentDefault(List<String> authPortalIds, String systemId, String userId, String platform);

    List<PortalListVO> selectorMenu();
}
