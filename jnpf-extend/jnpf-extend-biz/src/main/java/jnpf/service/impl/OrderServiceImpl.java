package jnpf.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.ImmutableMap;
import jnpf.base.service.BillRuleService;
import jnpf.base.service.SuperServiceImpl;
import jnpf.entity.OrderEntity;
import jnpf.entity.OrderEntryEntity;
import jnpf.entity.OrderReceivableEntity;
import jnpf.exception.DataException;
import jnpf.mapper.OrderEntryMapper;
import jnpf.mapper.OrderMapper;
import jnpf.mapper.OrderReceivableMapper;
import jnpf.model.order.*;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.service.UserService;
import jnpf.service.OrderService;
import jnpf.util.*;
import jnpf.workflow.service.WorkFlowApi;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 订单信息
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/15 9:19
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends SuperServiceImpl<OrderMapper, OrderEntity> implements OrderService {

    

    private final OrderReceivableMapper orderReceivableMapper;

    private final OrderEntryMapper orderEntryMapper;

    private final BillRuleService billRuleService;

    private final UserService userService;

    private final WorkFlowApi workFlowApi;


    /**
     * 前单
     **/
    private  static final String PREV = "prev";
    /**
     * 后单
     **/
    private  static final String NEXT = "next";

    @Override
    public List<OrderEntity> getList(PaginationOrder paginationOrder) {
        Map<String, String> fileMap = ImmutableMap.of("orderCode", "f_order_code", "orderDate", "f_order_date",
                "customerName", "f_customer_name", "salesmanName", "f_salesman_name",
                "receivableMoney", "f_receivable_money", "currentState", "f_current_state");
        paginationOrder.setSidx(fileMap.get(paginationOrder.getSidx()));

        QueryWrapper<OrderEntity> queryWrapper = new QueryWrapper<>();
        //关键字（订单编码、客户名称、业务人员）
        String keyWord = paginationOrder.getKeyword() != null ? paginationOrder.getKeyword() : null;
        if (!StringUtils.isEmpty(keyWord)) {
            queryWrapper.lambda().and(
                    t -> t.like(OrderEntity::getOrderCode, keyWord)
                            .or().like(OrderEntity::getCustomerName, keyWord)
                            .or().like(OrderEntity::getSalesmanName, keyWord)
            );
        }
        //起始日期-结束日期
        if (ObjectUtil.isNotEmpty(paginationOrder.getStartTime()) && ObjectUtil.isNotEmpty(paginationOrder.getEndTime())) {
            queryWrapper.lambda().between(OrderEntity::getOrderDate, new Date(paginationOrder.getStartTime()), new Date(paginationOrder.getEndTime()));
        }
        //订单状态
        String mark = paginationOrder.getEnabledMark();
        if (ObjectUtil.isNotEmpty(mark)) {
            queryWrapper.lambda().eq(OrderEntity::getEnabledMark, mark);
        }

        //流程数据查询
        if (ObjectUtil.isNotEmpty(paginationOrder.getTemplateId())) {
            List<String> flowIdsByTemplateId = workFlowApi.getFlowIdsByTemplateId(paginationOrder.getTemplateId());
            queryWrapper.lambda().in(OrderEntity::getFlowId, flowIdsByTemplateId);
        }
        //排序
        if (StringUtils.isEmpty(paginationOrder.getSidx())) {
            queryWrapper.lambda().orderByDesc(OrderEntity::getCreatorTime);
        } else {
            queryWrapper = "asc".equalsIgnoreCase(paginationOrder.getSort()) ? queryWrapper.orderByAsc(paginationOrder.getSidx()) : queryWrapper.orderByDesc(paginationOrder.getSidx());
        }
        Page<OrderEntity> page = new Page<>(paginationOrder.getCurrentPage(), paginationOrder.getPageSize());
        IPage<OrderEntity> orderEntityPage = this.page(page, queryWrapper);
        List<OrderEntity> data = orderEntityPage.getRecords();
        return paginationOrder.setData(data, page.getTotal());
    }

    @Override
    public List<OrderEntryEntity> getOrderEntryList(String id) {
        QueryWrapper<OrderEntryEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(OrderEntryEntity::getOrderId, id).orderByDesc(OrderEntryEntity::getSortCode);
        return orderEntryMapper.selectList(queryWrapper);
    }

    @Override
    public List<OrderReceivableEntity> getOrderReceivableList(String id) {
        QueryWrapper<OrderReceivableEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(OrderReceivableEntity::getOrderId, id).orderByDesc(OrderReceivableEntity::getSortCode);
        return orderReceivableMapper.selectList(queryWrapper);
    }

    @Override
    public OrderEntity getPrevOrNextInfo(String id, String method) {
        QueryWrapper<OrderEntity> result = new QueryWrapper<>();
        OrderEntity orderEntity = getInfo(id);
        String orderBy = "desc";
        if (PREV.equals(method)) {
            result.lambda().gt(OrderEntity::getCreatorTime, orderEntity.getCreatorTime());
            orderBy = "";
        } else if (NEXT.equals(method)) {
            result.lambda().lt(OrderEntity::getCreatorTime, orderEntity.getCreatorTime());
        }
        result.lambda().notIn(OrderEntity::getId, orderEntity.getId());
        if (StringUtil.isNotEmpty(orderBy)) {
            result.lambda().orderByDesc(OrderEntity::getCreatorTime);
        }
        List<OrderEntity> data = this.list(result);
        if (!data.isEmpty()) {
            return data.get(0);
        }
        return null;
    }

    @Override
    public OrderInfoVO getInfoVo(String id, String method) throws DataException {
        OrderInfoVO infoModel = null;
        OrderEntity orderEntity = this.getPrevOrNextInfo(id, method);
        if (orderEntity != null) {
            List<OrderEntryEntity> orderEntryList = this.getOrderEntryList(orderEntity.getId());
            List<OrderReceivableEntity> orderReceivableList = this.getOrderReceivableList(orderEntity.getId());
            infoModel = JsonUtilEx.getJsonToBeanEx(orderEntity, OrderInfoVO.class);
            UserEntity createUser = null;
            if (StringUtil.isNotEmpty(infoModel.getCreatorUserId())) {
                createUser = userService.getInfo(infoModel.getCreatorUserId());
            }
            infoModel.setCreatorUserId(createUser != null ? createUser.getRealName() + "/" + createUser.getAccount() : "");
            UserEntity lastUser = null;
            if (StringUtil.isNotEmpty(infoModel.getLastModifyUserId())) {
                lastUser = userService.getInfo(infoModel.getLastModifyUserId());
            }
            infoModel.setLastModifyUserId(lastUser != null ? lastUser.getRealName() + "/" + lastUser.getAccount() : "");
            List<OrderInfoOrderEntryModel> orderEntryModels = JsonUtil.getJsonToList(orderEntryList, OrderInfoOrderEntryModel.class);
            infoModel.setGoodsList(orderEntryModels);
            List<OrderInfoOrderReceivableModel> orderReceivableModels = JsonUtil.getJsonToList(orderReceivableList, OrderInfoOrderReceivableModel.class);
            infoModel.setCollectionPlanList(orderReceivableModels);
        }
        return infoModel;
    }

    @Override
    public OrderEntity getInfo(String id) {
        QueryWrapper<OrderEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(OrderEntity::getId, id);
        return this.getOne(queryWrapper);
    }

    @Override
    public void delete(OrderEntity entity) {
        QueryWrapper<OrderEntity> orderWrapper = new QueryWrapper<>();
        orderWrapper.lambda().eq(OrderEntity::getId, entity.getId());
        this.remove(orderWrapper);
        QueryWrapper<OrderEntryEntity> entryWrapper = new QueryWrapper<>();
        entryWrapper.lambda().eq(OrderEntryEntity::getOrderId, entity.getId());
        orderEntryMapper.delete(entryWrapper);
        QueryWrapper<OrderReceivableEntity> receivableWrapper = new QueryWrapper<>();
        receivableWrapper.lambda().eq(OrderReceivableEntity::getOrderId, entity.getId());
        orderReceivableMapper.delete(receivableWrapper);
    }

    @Override
    @DSTransactional
    public void create(OrderEntity entity, List<OrderEntryEntity> orderEntryList, List<OrderReceivableEntity> orderReceivableList) {
        entity.setCreatorUserId(UserProvider.getUser().getUserId());
        entity.setEnabledMark(1);
        for (int i = 0; i < orderEntryList.size(); i++) {
            orderEntryList.get(i).setId(RandomUtil.uuId());
            orderEntryList.get(i).setOrderId(entity.getId());
            orderEntryList.get(i).setSortCode(Long.parseLong(i + ""));
            orderEntryMapper.insert(orderEntryList.get(i));
        }
        for (int i = 0; i < orderReceivableList.size(); i++) {
            orderReceivableList.get(i).setId(RandomUtil.uuId());
            orderReceivableList.get(i).setOrderId(entity.getId());
            orderReceivableList.get(i).setSortCode(Long.parseLong(i + ""));
            orderReceivableMapper.insert(orderReceivableList.get(i));
        }
        billRuleService.useBillNumber("OrderNumber");
        this.save(entity);
    }

    @Override
    @DSTransactional
    public boolean update(String id, OrderEntity entity, List<OrderEntryEntity> orderEntryList, List<OrderReceivableEntity> orderReceivableList) {
        entity.setId(id);
        entity.setLastModifyTime(new Date());
        entity.setLastModifyUserId(UserProvider.getUser().getUserId());
        QueryWrapper<OrderEntryEntity> entryWrapper = new QueryWrapper<>();
        entryWrapper.lambda().eq(OrderEntryEntity::getOrderId, entity.getId());
        orderEntryMapper.delete(entryWrapper);
        QueryWrapper<OrderReceivableEntity> receivableWrapper = new QueryWrapper<>();
        receivableWrapper.lambda().eq(OrderReceivableEntity::getOrderId, entity.getId());
        orderReceivableMapper.delete(receivableWrapper);
        for (int i = 0; i < orderEntryList.size(); i++) {
            orderEntryList.get(i).setId(RandomUtil.uuId());
            orderEntryList.get(i).setOrderId(entity.getId());
            orderEntryList.get(i).setSortCode(Long.parseLong(i + ""));
            orderEntryMapper.insert(orderEntryList.get(i));
        }
        for (int i = 0; i < orderReceivableList.size(); i++) {
            orderReceivableList.get(i).setId(RandomUtil.uuId());
            orderReceivableList.get(i).setOrderId(entity.getId());
            orderReceivableList.get(i).setSortCode(Long.parseLong(i + ""));
            orderReceivableMapper.insert(orderReceivableList.get(i));
        }
        return this.updateById(entity);
    }

    @Override
    public void data(String id, String data) {
        OrderForm orderForm = JsonUtil.getJsonToBean(data, OrderForm.class);
        OrderEntity entity = JsonUtil.getJsonToBean(orderForm, OrderEntity.class);
        List<OrderEntryModel> goodsList = orderForm.getGoodsList() != null ? orderForm.getGoodsList() : new ArrayList<>();
        List<OrderEntryEntity> orderEntryList = JsonUtil.getJsonToList(goodsList, OrderEntryEntity.class);
        List<OrderReceivableModel> collectionPlanList = orderForm.getCollectionPlanList() != null ? orderForm.getCollectionPlanList() : new ArrayList<>();
        List<OrderReceivableEntity> orderReceivableList = JsonUtil.getJsonToList(collectionPlanList, OrderReceivableEntity.class);
        entity.setId(id);
        entity.setLastModifyTime(new Date());
        entity.setLastModifyUserId(UserProvider.getUser().getUserId());
        QueryWrapper<OrderEntryEntity> entryWrapper = new QueryWrapper<>();
        entryWrapper.lambda().eq(OrderEntryEntity::getOrderId, entity.getId());
        orderEntryMapper.delete(entryWrapper);
        QueryWrapper<OrderReceivableEntity> receivableWrapper = new QueryWrapper<>();
        receivableWrapper.lambda().eq(OrderReceivableEntity::getOrderId, entity.getId());
        orderReceivableMapper.delete(receivableWrapper);
        for (int i = 0; i < orderEntryList.size(); i++) {
            orderEntryList.get(i).setId(RandomUtil.uuId());
            orderEntryList.get(i).setOrderId(entity.getId());
            orderEntryList.get(i).setSortCode(Long.parseLong(i + ""));
            orderEntryMapper.insert(orderEntryList.get(i));
        }
        for (int i = 0; i < orderReceivableList.size(); i++) {
            orderReceivableList.get(i).setId(RandomUtil.uuId());
            orderReceivableList.get(i).setOrderId(entity.getId());
            orderReceivableList.get(i).setSortCode(Long.parseLong(i + ""));
            orderReceivableMapper.insert(orderReceivableList.get(i));
        }
        this.saveOrUpdate(entity);
    }


}
