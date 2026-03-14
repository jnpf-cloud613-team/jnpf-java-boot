package jnpf.visualdata.service;

import jnpf.base.service.SuperService;
import jnpf.visualdata.entity.VisualComponentEntity;
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
public interface VisualComponentService extends SuperService<VisualComponentEntity> {

    /**
     * 列表
     *
     * @param pagination 条件
     * @return
     */
    List<VisualComponentEntity> getList(VisualPaginationModel pagination);

    /**
     * 列表
     *
     * @return
     */
    List<VisualComponentEntity> getList();

    /**
     * 信息
     *
     * @param id 主键值
     * @return
     */
    VisualComponentEntity getInfo(String id);

    /**
     * 创建
     *
     * @param entity 实体对象
     */
    void create(VisualComponentEntity entity);

    /**
     * 更新
     *
     * @param id     主键值
     * @param entity 实体对象
     * @return
     */
    boolean update(String id, VisualComponentEntity entity);

    /**
     * 删除
     *
     * @param entity 实体对象
     */
    void delete(VisualComponentEntity entity);
}
