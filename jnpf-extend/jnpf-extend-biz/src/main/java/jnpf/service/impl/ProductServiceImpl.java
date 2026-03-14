package jnpf.service.impl;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.UserInfo;
import jnpf.base.service.BillRuleService;
import jnpf.base.service.SuperServiceImpl;
import jnpf.entity.ProductEntity;
import jnpf.entity.ProductEntryEntity;
import jnpf.exception.DataException;
import jnpf.mapper.ProductEntryMapper;
import jnpf.mapper.ProductMapper;
import jnpf.model.product.ProductPagination;
import jnpf.service.ProductService;
import jnpf.util.RandomUtil;
import jnpf.util.UserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 销售订单
 * 版本： V3.1.0
 * 版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * 作者： JNPF开发平台组
 * 日期： 2021-07-10 10:40:59
 */
@Service
@RequiredArgsConstructor
public class ProductServiceImpl extends SuperServiceImpl<ProductMapper, ProductEntity> implements ProductService {


    private final BillRuleService billRuleService;

    private final ProductEntryMapper productEntryMapper;



    @Override
    public List<ProductEntity> getList(ProductPagination productPagination) {
        return this.baseMapper.getList(productPagination);
    }

    @Override
    public ProductEntity getInfo(String id) {
        return this.baseMapper.getInfo(id);
    }

    @Override
    @DSTransactional(rollbackFor = Exception.class)
    public void create(ProductEntity entity, List<ProductEntryEntity> productEntryList) throws DataException {
        UserInfo userInfo = UserProvider.getUser();
        String code = billRuleService.getBillNumber("OrderNumber", false);
        entity.setEnCode(code);
        //类型
        entity.setType("市场活动");
        //制单
        entity.setSalesmanId(userInfo.getUserId());
        entity.setSalesmanName(userInfo.getUserName());
        entity.setSalesmanDate(new Date());
        //状态
        entity.setAuditState(1);
        entity.setGoodsState(1);
        entity.setCloseState(1);
        entity.setCreatorUserId(userInfo.getUserId());
        entity.setCreatorTime(new Date());
        entity.setId(RandomUtil.uuId());
        this.save(entity);
        for (ProductEntryEntity product : productEntryList) {
            product.setId(RandomUtil.uuId());
            product.setActivity("市场部全国香风健康奢护");
            product.setType("市场活动");
            product.setUtil("支");
            product.setCommandType("唯一码");
            product.setProductId(entity.getId());
            productEntryMapper.insert(product);
        }
    }

    @Override
    @DSTransactional(rollbackFor = Exception.class)
    public boolean update(String id, ProductEntity entity, List<ProductEntryEntity> productEntryList) {
        entity.setId(id);
        entity.setLastModifyUserId(UserProvider.getUser().getUserId());
        entity.setLastModifyTime(new Date());
        QueryWrapper<ProductEntryEntity> entryWrapper = new QueryWrapper<>();
        entryWrapper.lambda().eq(ProductEntryEntity::getProductId, entity.getId());
        productEntryMapper.delete(entryWrapper);
        for (ProductEntryEntity product : productEntryList) {
            product.setId(RandomUtil.uuId());
            product.setProductId(entity.getId());
            productEntryMapper.insert(product);
        }
        return this.updateById(entity);
    }

    @Override
    @DSTransactional(rollbackFor = Exception.class)
    public void delete(ProductEntity entity) {
        if (entity != null) {
            QueryWrapper<ProductEntryEntity> entryWrapper = new QueryWrapper<>();
            entryWrapper.lambda().eq(ProductEntryEntity::getProductId, entity.getId());
            productEntryMapper.delete(entryWrapper);
            this.removeById(entity.getId());
        }
    }


}
