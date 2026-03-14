package jnpf.service.impl;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.service.BillRuleService;
import jnpf.base.service.SuperServiceImpl;
import jnpf.entity.SalesOrderEntity;
import jnpf.entity.SalesOrderEntryEntity;
import jnpf.mapper.SalesOrderEntryMapper;
import jnpf.mapper.SalesOrderMapper;
import jnpf.model.salesorder.SalesOrderEntryEntityInfoModel;
import jnpf.model.salesorder.SalesOrderForm;
import jnpf.service.SalesOrderService;
import jnpf.util.JsonUtil;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 销售订单
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月29日 上午9:18
 */
@Service
@RequiredArgsConstructor
public class SalesOrderServiceImpl extends SuperServiceImpl<SalesOrderMapper, SalesOrderEntity> implements SalesOrderService {


    private final BillRuleService billRuleService;

    private final SalesOrderEntryMapper salesOrderEntryMapper;



    @Override
    public List<SalesOrderEntryEntity> getSalesEntryList(String id) {
        QueryWrapper<SalesOrderEntryEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(SalesOrderEntryEntity::getSalesOrderId, id).orderByDesc(SalesOrderEntryEntity::getSortCode);
        return salesOrderEntryMapper.selectList(queryWrapper);
    }

    @Override
    public SalesOrderEntity getInfo(String id) {
        return this.baseMapper.getInfo(id);
    }

    @Override
    @DSTransactional
    public void save(String id, SalesOrderEntity entity, List<SalesOrderEntryEntity> salesOrderEntryEntityList, SalesOrderForm form) {
        //表单信息
        if (StringUtil.isEmpty(entity.getId())) {
            entity.setId(id);
            for (int i = 0; i < salesOrderEntryEntityList.size(); i++) {
                salesOrderEntryEntityList.get(i).setId(RandomUtil.uuId());
                salesOrderEntryEntityList.get(i).setSalesOrderId(entity.getId());
                salesOrderEntryEntityList.get(i).setSortCode(Long.parseLong(i + ""));
                salesOrderEntryMapper.insert(salesOrderEntryEntityList.get(i));
            }
            //创建
            save(entity);
            billRuleService.useBillNumber("WF_SalesOrderNo");
        } else {
            entity.setId(id);
            QueryWrapper<SalesOrderEntryEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().eq(SalesOrderEntryEntity::getSalesOrderId, entity.getId());
            salesOrderEntryMapper.delete(queryWrapper);
            for (int i = 0; i < salesOrderEntryEntityList.size(); i++) {
                salesOrderEntryEntityList.get(i).setId(RandomUtil.uuId());
                salesOrderEntryEntityList.get(i).setSalesOrderId(entity.getId());
                salesOrderEntryEntityList.get(i).setSortCode(Long.parseLong(i + ""));
                salesOrderEntryMapper.insert(salesOrderEntryEntityList.get(i));
            }
            //编辑
            updateById(entity);
        }
    }

    @Override
    @DSTransactional
    public void submit(String id, SalesOrderEntity entity, List<SalesOrderEntryEntity> salesOrderEntryEntityList, SalesOrderForm form) {
        //表单信息
        if (StringUtil.isEmpty(entity.getId())) {
            entity.setId(id);
            for (int i = 0; i < salesOrderEntryEntityList.size(); i++) {
                salesOrderEntryEntityList.get(i).setId(RandomUtil.uuId());
                salesOrderEntryEntityList.get(i).setSalesOrderId(entity.getId());
                salesOrderEntryEntityList.get(i).setSortCode(Long.parseLong(i + ""));
                salesOrderEntryMapper.insert(salesOrderEntryEntityList.get(i));
            }
            //创建
            save(entity);
            billRuleService.useBillNumber("WF_SalesOrderNo");
        } else {
            entity.setId(id);
            QueryWrapper<SalesOrderEntryEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().eq(SalesOrderEntryEntity::getSalesOrderId, entity.getId());
            salesOrderEntryMapper.delete(queryWrapper);
            for (int i = 0; i < salesOrderEntryEntityList.size(); i++) {
                salesOrderEntryEntityList.get(i).setId(RandomUtil.uuId());
                salesOrderEntryEntityList.get(i).setSalesOrderId(entity.getId());
                salesOrderEntryEntityList.get(i).setSortCode(Long.parseLong(i + ""));
                salesOrderEntryMapper.insert(salesOrderEntryEntityList.get(i));
            }
            //编辑
            updateById(entity);
        }
    }

    @Override
    public void data(String id, String data) {
        SalesOrderForm salesOrderForm = JsonUtil.getJsonToBean(data, SalesOrderForm.class);
        SalesOrderEntity entity = JsonUtil.getJsonToBean(salesOrderForm, SalesOrderEntity.class);
        List<SalesOrderEntryEntityInfoModel> entryList = salesOrderForm.getEntryList() != null ? salesOrderForm.getEntryList() : new ArrayList<>();
        List<SalesOrderEntryEntity> salesOrderEntryEntityList = JsonUtil.getJsonToList(entryList, SalesOrderEntryEntity.class);
        entity.setId(id);
        QueryWrapper<SalesOrderEntryEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(SalesOrderEntryEntity::getSalesOrderId, entity.getId());
        salesOrderEntryMapper.delete(queryWrapper);
        for (int i = 0; i < salesOrderEntryEntityList.size(); i++) {
            salesOrderEntryEntityList.get(i).setId(RandomUtil.uuId());
            salesOrderEntryEntityList.get(i).setSalesOrderId(entity.getId());
            salesOrderEntryEntityList.get(i).setSortCode(Long.parseLong(i + ""));
            salesOrderEntryMapper.insert(salesOrderEntryEntityList.get(i));
        }
        //编辑
        saveOrUpdate(entity);
    }

    @Override
    public void delete(SalesOrderEntity entity) {
        this.baseMapper.delete(entity);
    }

}
