package jnpf.permission.service;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import jnpf.base.service.SuperService;
import jnpf.permission.entity.OrganizeEntity;
import jnpf.permission.model.organize.OrganizeListVO;
import jnpf.permission.model.organize.OrganizePagination;

import java.util.List;
import java.util.Map;

/**
 * 组织机构
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月26日 上午9:18
 */
public interface OrganizeService extends SuperService<OrganizeEntity> {
    /**
     * 组织异步列表
     *
     * @return
     */
    List<OrganizeEntity> getList(OrganizePagination pagination);

    /**
     * 验证名称
     * 原来是，同级下不同类型可以重名，改成同级下所有类型都不能重名
     *
     * @param entity
     * @param isCheck  组织名称是否不分级判断
     * @param isFilter 是否需要过滤id
     * @return
     */
    boolean isExistByFullName(OrganizeEntity entity, boolean isCheck, boolean isFilter);

    /**
     * 验证编码
     *
     * @param enCode
     * @param id
     * @return
     */
    boolean isExistByEnCode(String enCode, String id);


    /**
     * 创建
     *
     * @param entity 实体对象
     */
    void create(OrganizeEntity entity);

    /**
     * 更新
     *
     * @param id     主键值
     * @param entity 实体对象
     */
    boolean update(String id, OrganizeEntity entity);

    /**
     * 信息
     *
     * @param id 主键值
     * @return
     */
    OrganizeEntity getInfo(String id);

    /**
     * 删除
     *
     * @param orgId 实体对象
     */
    List<OrganizeEntity> delete(String orgId);

    /**
     * 验证层级
     *
     * @param entity 实体对象
     */
    boolean checkLevel(OrganizeEntity entity);

    /**
     * 验证下级组织类型
     *
     * @param entity 实体对象
     */
    boolean checkOrgType(OrganizeEntity entity);

    /**
     * 获取所有组织全路径名称
     *
     * @return
     */
    Map<String, Object> getAllOrgsTreeName();

    Map<String, String> getAllOrgsTreeName(boolean enabledMark);

    /**
     * 根据id获取列表
     *
     * @return
     */
    List<OrganizeEntity> getListAll(List<String> idAll, String keyWord);

    /**
     * 根据id获取列表
     *
     * @return
     */
    List<OrganizeEntity> getListByIds(List<String> idList);

    /**
     * 获取全部父级组织
     *
     * @param parentId
     * @return
     */
    List<OrganizeEntity> getParentList(String parentId);

    /**
     * @param idList
     * @return
     */
    List<OrganizeEntity> getListByParentIds(List<String> idList);

    /**
     * 设置组织的树形id和名称
     *
     * @param entity
     */
    void setOrgTreeIdAndName(OrganizeEntity entity);

    /**
     * 列表
     *
     * @return
     */
    List<OrganizeEntity> getDepsByParentId(String id);

    /**
     * 列表
     *
     * @param filterEnabledMark
     * @return
     */
    List<OrganizeEntity> getList(boolean filterEnabledMark);

    /**
     * 列表
     *
     * @return
     */
    List<OrganizeEntity> getList(String keyword, boolean filterEnabledMark);

    /**
     * 获取组织信息
     *
     * @param keyword
     * @param filterEnabledMark
     * @param category
     * @return OrgId, OrgEntity
     */
    Map<String, OrganizeEntity> getOrgMaps(String keyword, boolean filterEnabledMark, String category, SFunction<OrganizeEntity, ?>... columns);

    /**
     * 获取组织信息
     *
     * @return OrgId, OrgEntity
     */
    Map<String, OrganizeEntity> getOrgMapsAll(SFunction<OrganizeEntity, ?>... columns);

    /**
     * 列表
     *
     * @param fullName 组织名称
     * @return
     */
    OrganizeEntity getInfoByFullName(String fullName);

    /**
     * 获取部门名列表
     *
     * @return
     */
    List<OrganizeEntity> getOrgEntityList(List<String> idList, Boolean enable);

    /**
     * 全部组织（id : name）
     *
     * @return
     */
    Map<String, Object> getOrgMap();

    /**
     * 全部组织（Encode/name : id）
     *
     * @param category
     * @return
     */
    Map<String, Object> getOrgEncodeAndName(String category);

    /**
     * 全部组织（name : id）
     *
     * @param category
     * @return
     */
    Map<String, Object> getOrgNameAndId(String category);


    /**
     * 通过名称查询id
     *
     * @param fullName 名称
     * @return
     */
    OrganizeEntity getByFullName(String fullName);


    /**
     * 获取父级id
     *
     * @param organizeId           组织id
     * @param organizeParentIdList 父级id集合
     */
    void getOrganizeIdTree(String organizeId, List<String> organizeParentIdList);

    /**
     * 获取父级id
     *
     * @param organizeId           组织id
     * @param organizeParentIdList 父级id集合
     */
    void getOrganizeId(String organizeId, List<OrganizeEntity> organizeParentIdList);

    /**
     * 判断是否允许删除
     *
     * @param orgId 主键值
     * @return
     */
    String allowDelete(String orgId);

    /**
     * 获取名称
     *
     * @return
     */
    List<OrganizeEntity> getOrganizeName(List<String> id);

    /**
     * 获取名称
     *
     * @return
     */
    Map<String, OrganizeEntity> getOrganizeName(List<String> id, String keyword, boolean filterEnabledMark, String category);

    /**
     * 获取所有当前用户的组织及子组织
     *
     * @param organizeId
     * @param filterEnabledMark
     * @return
     */
    List<String> getUnderOrganizations(String organizeId, boolean filterEnabledMark);

    /**
     * 获取所有当前用户的组织及子组织 (有分级权限验证)
     *
     * @param organizeId
     * @return
     */
    List<String> getUnderOrganizationss(String organizeId);

    /**
     * 通过id判断是否有子集
     *
     * @param id 主键
     * @return
     */
    List<OrganizeEntity> getListByParentId(String id);

    /**
     * 获取用户所有所在组织
     *
     * @return 组织对象集合
     */
    List<OrganizeEntity> getAllOrgByUserId(String userId);

    /**
     * 通过组织id树获取名称
     *
     * @param idNameMaps 预先获取的组织ID名称映射
     * @param orgIdTree  组织id树
     * @param regex      分隔符
     * @return 组织对象集合
     */
    String getFullNameByOrgIdTree(Map<String, String> idNameMaps, String orgIdTree, String regex);

    /**
     * 获取父级组织id
     *
     * @param entity
     * @return
     */
    String getOrganizeIdTree(OrganizeEntity entity);

    /**
     * 获取顶级组织
     *
     * @param parentId
     * @return
     */
    List<OrganizeEntity> getOrganizeByParentId(String parentId);

    /**
     * 查询用户的所属公司下的部门
     *
     * @return
     */
    List<OrganizeEntity> getDepartmentAll(String organizeId);

    /**
     * 获取所在公司
     *
     * @param organizeId
     * @return
     */
    OrganizeEntity getOrganizeCompany(String organizeId);

    /**
     * 获取所在公司下部门
     *
     * @return
     */
    void getOrganizeDepartmentAll(String organize, List<OrganizeEntity> list);

    /**
     * 获取组织id树
     *
     * @param entity
     * @return
     */
    List<String> getOrgIdTree(OrganizeEntity entity);

    /**
     * 向上递归取组织id
     *
     * @param orgID
     * @return
     */
    List<String> upWardRecursion(List<String> orgIDs, String orgID);


    /**
     * 获取名称及id组成map
     *
     * @return
     */
    Map<String, String> getInfoList();

    /**
     * 组织自定义范围回显
     *
     * @return
     */
    List<OrganizeListVO> selectedList(List<String> idStrList);

    /**
     * 组织范围下拉
     *
     * @return
     */
    List<OrganizeEntity> organizeCondition(List<String> idStrList);

    /**
     * ids列表字符串获取名称（当前组织）
     *
     * @param idStr
     * @return
     */
    String getNameByIdStr(String idStr);

    /**
     * 获取子孙组织
     *
     * @param idList
     * @return
     */
    List<OrganizeEntity> getProgeny(List<String> idList, Integer enabledMark);
}
