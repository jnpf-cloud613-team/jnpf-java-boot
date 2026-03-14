package jnpf.message.service;


import jnpf.base.model.synthird.PaginationSynThirdInfo;
import jnpf.base.model.synthird.SynThirdTotal;
import jnpf.base.service.SuperService;
import jnpf.message.entity.SynThirdInfoEntity;
import jnpf.message.model.SynThirdInfoVo;
import jnpf.permission.entity.OrganizeEntity;

import java.util.List;

/**
 * 第三方工具的公司-部门-用户同步表模型
 *
 * @版本： V3.1.0
 * @版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @作者： JNPF开发平台组
 * @日期： 2021/4/23 17:29
 */
public interface SynThirdInfoService extends SuperService<SynThirdInfoEntity> {

    /**
     * 获取指定第三方工具、指定数据类型的数据列表
     *
     * @param thirdType
     * @param dataType
     * @return
     */
    List<SynThirdInfoEntity> getList(String thirdType, String dataType);

    List<SynThirdInfoEntity> getList(String thirdType, String dataType, String enableMark);

    /**
     * 获取同步的详细信息
     *
     * @param id
     * @return
     */
    SynThirdInfoEntity getInfo(String id);

    void create(SynThirdInfoEntity entity);

    boolean update(String id, SynThirdInfoEntity entity);

    void delete(SynThirdInfoEntity entity);

    /**
     * 获取指定第三方工具、指定数据类型、对象ID的同步信息
     *
     * @param thirdType
     * @param dataType
     * @param id
     * @return
     */
    SynThirdInfoEntity getInfoBySysObjId(String thirdType, String dataType, String id);

    /**
     * 获取指定第三方工具、指定数据类型的同步统计信息
     *
     * @param thirdType
     * @param dataType
     * @return
     */
    SynThirdTotal getSynTotal(String thirdType, String dataType);

    /**
     * @param thirdToSysType
     * @param dataTypeOrg
     * @param sysToThirdType
     * @return
     */
    List<SynThirdInfoEntity> syncThirdInfoByType(String thirdToSysType, String dataTypeOrg, String sysToThirdType);

    boolean getBySysObjId(String id, String thirdType);

    String getSysByThird(String valueOf);

    String getSysByThird(String valueOf, Integer type);

    void initBaseDept(Long dingRootDeptId, String accessToken, String thirdType);

    /**
     * 获取指定第三方工具、指定数据类型、第三方对象ID的同步信息 20220331
     *
     * @param thirdType
     * @param dataType
     * @param thirdObjId
     * @return
     */
    SynThirdInfoEntity getInfoByThirdObjId(String thirdType, String dataType, String thirdObjId);

    /**
     * 解除绑定
     *
     * @param type
     */
    void clearAllSyn(Integer type);

    /**
     * 根据钉钉绑定组织有序获取下级组织
     */
    List<OrganizeEntity> getOrganizeEntitiesBind(String department);

    List<SynThirdInfoVo> getListJoin(PaginationSynThirdInfo paginationSynThirdInfo);

    List<SynThirdInfoEntity> getListByDepartment(String thirdTypeDing, String dataTypeOrg, String dingDepartment);

}
