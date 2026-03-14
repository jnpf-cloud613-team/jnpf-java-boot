package jnpf.visualdata.service;

import jnpf.base.service.SuperService;
import jnpf.visualdata.entity.VisualAssetsEntity;
import jnpf.visualdata.model.visual.VisualPaginationModel;

import java.util.List;

/**
 * 静态资源
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021年6月15日
 */
public interface VisualAssetsService extends SuperService<VisualAssetsEntity> {

    /**
     * 列表
     *
     * @param pagination 条件
     * @return
     */
    List<VisualAssetsEntity> getList(VisualPaginationModel pagination);

    /**
     * 列表
     *
     * @return
     */
    List<VisualAssetsEntity> getList();

    /**
     * 信息
     *
     * @param id 主键值
     * @return
     */
    VisualAssetsEntity getInfo(String id);

    /**
     * 创建
     *
     * @param entity 实体对象
     */
    void create(VisualAssetsEntity entity);

    /**
     * 更新
     *
     * @param id     主键值
     * @param entity 实体对象
     * @return
     */
    boolean update(String id, VisualAssetsEntity entity);

    /**
     * 删除
     */
    boolean delete(String id);
}
