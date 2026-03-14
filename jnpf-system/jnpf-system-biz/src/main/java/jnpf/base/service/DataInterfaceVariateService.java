package jnpf.base.service;

import jnpf.base.Page;
import jnpf.base.entity.DataInterfaceVariateEntity;

import java.util.List;
import java.util.Map;

/**
 * 数据接口业务层
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021-03-23
 */
public interface DataInterfaceVariateService extends SuperService<DataInterfaceVariateEntity> {

    /**
     * 列表
     * @param id
     * @param page
     * @return
     */
    List<DataInterfaceVariateEntity> getList(String id, Page page);

    /**
     * 详情
     *
     * @param id
     * @return
     */
    DataInterfaceVariateEntity getInfo(String id);

    /**
     * 判断名称是否重复
     *
     * @param entity
     * @return
     */
    boolean isExistByFullName(DataInterfaceVariateEntity entity);

    /**
     * 添加
     *
     * @param entity
     * @return
     */
    boolean create(DataInterfaceVariateEntity entity);

    /**
     * 修改
     *
     * @param entity
     * @return
     */
    boolean update(DataInterfaceVariateEntity entity);

    /**
     * 删除
     *
     * @param entity
     * @return
     */
    boolean delete(DataInterfaceVariateEntity entity);

    /**
     * 通过id获取列表
     *
     * @param ids
     * @return
     */
    List<DataInterfaceVariateEntity> getListByIds(List<String> ids);

    void update(Map<String, String> map, List<DataInterfaceVariateEntity> variateEntities);

    /**
     * 通过名称获取变量
     *
     * @param fullName
     */
    DataInterfaceVariateEntity getInfoByFullName(String fullName);
}
