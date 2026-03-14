package jnpf.visualdata.service;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import jnpf.base.service.SuperService;
import jnpf.visualdata.entity.VisualMapEntity;
import jnpf.visualdata.model.visual.VisualPaginationModel;

import java.util.List;

/**
 * 大屏地图配置
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021年6月15日
 */
public interface VisualMapService extends SuperService<VisualMapEntity> {

    /**
     * 列表
     *
     * @param pagination 条件
     * @return
     */
    List<VisualMapEntity> getList(VisualPaginationModel pagination);

    /**
     * 列表
     * @param pagination 条件
     * @param columns 筛选字段
     * @return
     */
    List<VisualMapEntity> getListWithColnums(VisualPaginationModel pagination, SFunction<VisualMapEntity, ?>... columns);

    /**
     * 信息
     *
     * @param id 主键值
     * @return
     */
    VisualMapEntity getInfo(String id);

    /**
     * 创建
     *
     * @param entity 实体对象
     */
    void create(VisualMapEntity entity);

    /**
     * 更新
     *
     * @param id     主键值
     * @param entity 实体对象
     * @return
     */
    boolean update(String id, VisualMapEntity entity);

    /**
     * 删除
     *
     * @param entity 实体对象
     */
    void delete(VisualMapEntity entity);



    /**
     * 是否存在夏季
     */
    boolean hasChild(String id);
}
