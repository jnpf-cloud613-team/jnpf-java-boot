package jnpf.visualdata.service;

import jnpf.base.service.SuperService;
import jnpf.visualdata.entity.VisualRecordEntity;
import jnpf.visualdata.model.visual.VisualPaginationModel;

import java.util.List;

/**
 * 大屏数据集
 *
 * @author JNPF开发平台组
 * @version V3.5.0
 * @copyright 引迈信息技术有限公司
 * @date 2023年7月7日
 */
public interface VisualRecordService extends SuperService<VisualRecordEntity> {

    /**
     * 列表
     *
     * @param pagination 条件
     * @return
     */
    List<VisualRecordEntity> getList(VisualPaginationModel pagination);

    /**
     * 列表
     *
     * @return
     */
    List<VisualRecordEntity> getList();

    /**
     * 信息
     *
     * @param id 主键值
     * @return
     */
    VisualRecordEntity getInfo(String id);

    /**
     * 创建
     *
     * @param entity 实体对象
     */
    void create(VisualRecordEntity entity);

    /**
     * 更新
     *
     * @param id     主键值
     * @param entity 实体对象
     * @return
     */
    boolean update(String id, VisualRecordEntity entity);

    /**
     * 删除
     *
     * @param entity 实体对象
     */
    void delete(VisualRecordEntity entity);
}
