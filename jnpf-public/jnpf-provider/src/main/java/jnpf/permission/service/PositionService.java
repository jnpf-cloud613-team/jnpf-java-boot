package jnpf.permission.service;


import jnpf.base.Pagination;
import jnpf.base.service.SuperService;
import jnpf.permission.entity.PositionEntity;
import jnpf.permission.model.permission.PermissionModel;
import jnpf.permission.model.position.PosConModel;
import jnpf.permission.model.position.PositionListVO;
import jnpf.permission.model.position.PositionPagination;

import java.util.List;
import java.util.Map;

/**
 * 岗位信息
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月26日 上午9:18
 */
public interface PositionService extends SuperService<PositionEntity> {

    /**
     * 查询岗位列表
     *
     * @param pagination
     * @return
     */
    List<PositionEntity> getList(PositionPagination pagination);

    /**
     * 验证名称
     *
     * @param entity
     * @param isFilter 是否过滤
     * @return
     */
    boolean isExistByFullName(PositionEntity entity, boolean isFilter);

    /**
     * 验证编码
     *
     * @param enCode
     * @param id
     * @return
     */
    Boolean isExistByEnCode(String enCode, String id);

    /**
     * 创建
     *
     * @param entity 实体对象
     */
    void create(PositionEntity entity);

    /**
     * 更新
     *
     * @param id     主键值
     * @param entity 实体对象
     */
    boolean update(String id, PositionEntity entity);

    /**
     * 信息
     *
     * @param id 主键值
     * @return
     */
    PositionEntity getInfo(String id);

    /**
     * 删除
     *
     * @param entity 实体对象
     */
    void delete(PositionEntity entity);


    /**
     * 通过组织删除岗位，前提是验证过岗位未绑定其他数据
     *
     * @param orgId 实体对象
     */
    void deleteByOrgId(String orgId);

    /**
     * 验证岗位层级
     *
     * @param entity
     * @return
     */
    boolean checkLevel(PositionEntity entity);

    /**
     * 递归获取所有父级（层级）
     *
     * @param parentId
     * @return
     */
    List<PositionEntity> getParentList(String parentId);

    /**
     * 获取下级岗位
     *
     * @param parentId
     * @return
     */
    List<PositionEntity> getByParentId(String parentId);

    /**
     * 根据组织id查询所有岗位
     *
     * @param orgIds
     * @return
     */
    List<PositionEntity> getListByOrgIds(List<String> orgIds);

    /**
     * 根据组织id获取一级岗位
     *
     * @param orgIds
     * @return
     */
    List<PositionEntity> getListByOrgIdOneLevel(List<String> orgIds);

    /**
     * 列表
     *
     * @param filterEnabledMark
     * @return
     */
    List<PositionEntity> getList(boolean filterEnabledMark);

    /**
     * 获取全部下级
     *
     * @param id
     * @return
     */
    List<PositionEntity> getAllChild(String id);

    /**
     * 岗位名列表（在线开发）
     *
     * @param idList
     * @return
     */
    List<PositionEntity> getPosList(List<String> idList);

    List<PositionEntity> getListByIds(List<String> idList);

    /**
     * 根据id查询岗位列表
     *
     * @param idList
     * @return
     */
    List<PositionEntity> getListByIds(Pagination pagination, List<String> idList);

    Map<String, String> getPosMap();

    Map<String, String> getPosFullNameMap();

    Map<String, Object> getPosEncodeAndName();

    Map<String, Object> getPosEncodeAndName(boolean enabledMark);

    /**
     * 上移
     *
     * @param id 主键值
     */
    boolean first(String id);

    /**
     * 下移
     *
     * @param id 主键值
     */
    boolean next(String id);

    /**
     * 获取名称
     *
     * @return
     */
    List<PositionEntity> getPositionName(List<String> id, boolean filterEnabledMark);

    /**
     * 获取名称
     *
     * @return
     */
    List<PositionEntity> getPositionName(List<String> id, String keyword);

    /**
     * 获取岗位列表
     *
     * @param organizeIds 组织id
     * @param enabledMark
     * @return
     */
    List<PositionEntity> getListByOrganizeId(List<String> organizeIds, boolean enabledMark);

    /**
     * 获取用户组织底下所有的岗位
     *
     * @param organizeId
     * @param userId
     * @return
     */
    List<PositionEntity> getListByOrgIdAndUserId(String organizeId, String userId);

    /**
     * 通过名称获取岗位列表
     *
     * @param fullName 岗位名称
     * @param enCode   编码
     * @return
     */
    List<PositionEntity> getListByFullName(String fullName, String enCode);

    /**
     * 根据组织列表获取
     *
     * @param organizeIds
     * @return
     */
    List<PermissionModel> getListByOrganizeIds(List<String> organizeIds, boolean needCode, boolean enabledMark);


    /**
     * 岗位自定义范围回显
     *
     * @return
     */
    List<PositionListVO> selectedList(List<String> idStrList);

    /**
     * 岗位范围下拉
     *
     * @return
     */
    List<PositionEntity> positionCondition(List<String> idStrList);

    /**
     * 根据责任人查询岗位
     *
     * @param userId 责任人id
     * @return 返回数据
     */
    List<PositionEntity> getListByDutyUser(String userId);

    /**
     * 根据父id集合查询子岗位信息
     *
     * @param collect 父岗位id集合
     * @return 返回数据
     */
    List<PositionEntity> getListByParentIds(List<String> collect);

    /**
     * 联动修改岗位约束
     *
     * @param id
     * @param posConModel
     */
    void linkUpdate(String id, PosConModel posConModel);

    /**
     * ids列表字符串获取名称（当前岗位）
     *
     * @param idStr
     * @return
     */
    String getNameByIdStr(String idStr);

    /**
     * 获取子孙岗位
     *
     * @param idList
     * @return
     */
    List<PositionEntity> getProgeny(List<String> idList, Integer enabledMark);
}
