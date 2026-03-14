package jnpf.service;

import jnpf.base.service.SuperService;
import jnpf.entity.OrderEntity;
import jnpf.entity.OrderEntryEntity;
import jnpf.entity.OrderReceivableEntity;
import jnpf.exception.DataException;
import jnpf.exception.WorkFlowException;
import jnpf.model.order.OrderInfoVO;
import jnpf.model.order.PaginationOrder;

import java.util.List;

/**
 * 订单信息
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月26日 上午9:18
 */
public interface OrderService extends SuperService<OrderEntity> {

    /**
     * 列表
     *
     * @param paginationOrder 分页
     * @return
     */
    List<OrderEntity> getList(PaginationOrder paginationOrder);

    /**
     * 子列表（订单明细）
     *
     * @param id 主表Id
     * @return
     */
    List<OrderEntryEntity> getOrderEntryList(String id);

    /**
     * 子列表（订单收款）
     *
     * @param id 主表Id
     * @return
     */
    List<OrderReceivableEntity> getOrderReceivableList(String id);

    /**
     * 信息（前单、后单）
     *
     * @param id     主键值
     * @param method 方法:prev、next
     * @return
     */
    OrderEntity getPrevOrNextInfo(String id, String method);

    /**
     * 信息（前单、后单）
     *
     * @param id     主键值
     * @param method 方法:prev、next
     * @return
     * @throws DataException 异常
     */
    OrderInfoVO getInfoVo(String id, String method) throws DataException;

    /**
     * 信息
     *
     * @param id 主键值
     * @return
     */
    OrderEntity getInfo(String id);

    /**
     * 删除
     *
     * @param entity 订单信息
     */
    void delete(OrderEntity entity);

    /**
     * 新增
     *
     * @param entity              订单信息
     * @param orderEntryList      订单明细
     * @param orderReceivableList 订单收款
     * @throws WorkFlowException 异常
     */
    void create(OrderEntity entity, List<OrderEntryEntity> orderEntryList, List<OrderReceivableEntity> orderReceivableList);

    /**
     * 更新
     *
     * @param id                  主键值
     * @param entity              订单信息
     * @param orderEntryList      订单明细
     * @param orderReceivableList 订单收款
     * @return
     * @throws WorkFlowException 异常
     */
    boolean update(String id, OrderEntity entity, List<OrderEntryEntity> orderEntryList, List<OrderReceivableEntity> orderReceivableList);

    /**
     * 更改数据
     *
     * @param id   主键值
     * @param data 实体对象
     */
    void data(String id, String data);

}
