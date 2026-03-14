package jnpf.permission.service;

import jnpf.base.service.SuperService;
import jnpf.permission.entity.PositionEntity;
import jnpf.permission.entity.RoleEntity;
import jnpf.permission.entity.StandingEntity;
import jnpf.permission.model.standing.StandingModel;
import jnpf.permission.model.standing.StandingPagination;

import java.util.List;

/**
 * 身份管理service
 *
 * @author JNPF开发平台组
 * @version v6.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2025/3/4 18:23:22
 */
public interface StandingService extends SuperService<StandingEntity> {

    /**
     * 列表
     *
     * @param pagination 关键字
     * @return
     */
    List<StandingEntity> getList(StandingPagination pagination);

    /**
     * 创建
     *
     * @param entity
     */
    void crete(StandingEntity entity);

    /**
     * 修改
     *
     * @param id
     * @param entity
     */
    Boolean update(String id, StandingEntity entity);


    /**
     * 详情
     *
     * @param id
     * @return
     */
    StandingEntity getInfo(String id);

    /**
     * 删除
     *
     * @param entity
     */
    void delete(StandingEntity entity);

    /**
     * 判断名称是否重复
     *
     * @param fullName
     * @param id
     * @return
     */
    Boolean isExistByFullName(String fullName, String id);

    /**
     * 判断编码是否重复
     *
     * @param enCode
     * @param id
     * @return
     */
    Boolean isExistByEnCode(String enCode, String id);

    /**
     * 获取身份列表
     *
     * @param idList 身份主键列表
     * @return
     */
    List<StandingEntity> getListByIds(List<String> idList);

    /**
     * 获取角色列表
     *
     * @param pagination
     * @return
     */
    List<RoleEntity> getRolePage(StandingPagination pagination);

    /**
     * 获取岗位列表
     *
     * @param pagination
     * @return
     */
    List<PositionEntity> getPosPage(StandingPagination pagination);

    /**
     * 根据不同的岗位和角色获取身份列表
     *
     * @return
     */
    List<StandingModel> getByObjectIds(List<String> objectIds);
}
