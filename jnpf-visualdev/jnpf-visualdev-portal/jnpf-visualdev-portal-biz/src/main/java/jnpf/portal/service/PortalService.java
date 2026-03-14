package jnpf.portal.service;

import jnpf.base.model.VisualFunctionModel;
import jnpf.base.model.base.SystemListVO;
import jnpf.base.model.export.PortalExportDataVo;
import jnpf.base.service.SuperService;
import jnpf.portal.entity.PortalEntity;
import jnpf.portal.model.PortalPagination;
import jnpf.portal.model.PortalSelectModel;

import java.util.List;


/**
 * base_portal
 * 版本： V3.0.0
 * 版权： 引迈信息技术有限公司
 * 作者： 管理员/admin
 * 日期： 2020-10-21 14:23:30
 */

public interface PortalService extends SuperService<PortalEntity> {

    PortalEntity getInfo(String id);

    /**
     * 是否重名(应用内判重)
     */
    boolean isExistByFullName(String fullName, String id, String systemId);

    /**
     * 是否重码
     */
    boolean isExistByEnCode(String encode, String id);

    /**
     * 设置自动生成编码
     *
     * @param entity
     */
    void setAutoEnCode(PortalEntity entity);

    void create(PortalEntity entity);

    Boolean update(String id, PortalEntity entity);

    void delete(PortalEntity entity);

    List<PortalEntity> getList(PortalPagination pagination);

    List<PortalSelectModel> getModSelectList();

    /**
     * 获取门户模型集合
     *
     * @param pagination 分页信息
     * @return 模型集合
     */
    List<VisualFunctionModel> getModelList(PortalPagination pagination);


    /**
     * 获取可选系统列表
     *
     * @param id
     * @return
     */
    List<SystemListVO> systemFilterList(String id, String category);

    /**
     * 获取发布信息
     *
     * @param id
     * @return
     */
    VisualFunctionModel getReleaseInfo(String id);

    List<PortalExportDataVo> getExportList(String systemId);

    void deleteBySystemId(String systemId);
}
