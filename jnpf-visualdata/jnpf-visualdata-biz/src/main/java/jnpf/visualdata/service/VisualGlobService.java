package jnpf.visualdata.service;

import jnpf.base.service.SuperService;
import jnpf.visualdata.entity.VisualGlobEntity;
import jnpf.visualdata.model.visual.VisualPaginationModel;

import java.util.List;

/**
 * 大屏组件库
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021年6月15日
 */
public interface VisualGlobService extends SuperService<VisualGlobEntity> {

    /**
     * 列表
     *
     * @param pagination 条件
     * @return
     */
    List<VisualGlobEntity> getList(VisualPaginationModel pagination);

    /**
     * 列表
     *
     * @return
     */
    List<VisualGlobEntity> getList();

    /**
     * 信息
     *
     * @param id 主键值
     * @return
     */
    VisualGlobEntity getInfo(String id);

    /**
     * 创建
     *
     * @param entity 实体对象
     */
    void create(VisualGlobEntity entity);

    /**
     * 更新
     *
     * @param id     主键值
     * @param entity 实体对象
     * @return
     */
    boolean update(String id, VisualGlobEntity entity);

    /**
     * 删除
     *
     * @param entity 实体对象
     */
    void delete(VisualGlobEntity entity);
}
