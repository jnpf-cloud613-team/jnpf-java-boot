package jnpf.base.service.impl;


import cn.hutool.core.util.ObjectUtil;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.ActionResult;
import jnpf.base.Pagination;
import jnpf.base.entity.BillRuleEntity;
import jnpf.base.mapper.BillRuleMapper;
import jnpf.base.model.billrule.BillRulePagination;
import jnpf.base.service.BillRuleService;
import jnpf.base.service.SuperServiceImpl;
import jnpf.constant.MsgCode;
import jnpf.exception.DataException;
import jnpf.util.RandomUtil;
import jnpf.util.RedisUtil;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 单据规则
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
@Service
@RequiredArgsConstructor
public class BillRuleServiceImpl extends SuperServiceImpl<BillRuleMapper, BillRuleEntity> implements BillRuleService {


    private final RedisUtil redisUtil;

    @Override
    public List<BillRuleEntity> getList(BillRulePagination pagination) {
        return this.baseMapper.getList(pagination);
    }

    @Override
    public List<BillRuleEntity> getList() {
        return this.baseMapper.getList();
    }

    @Override
    public BillRuleEntity getInfo(String id) {
        return this.baseMapper.getInfo(id);
    }

    @Override
    public boolean isExistByFullName(String fullName, String id) {
        return this.baseMapper.isExistByFullName(fullName, id);
    }

    @Override
    public boolean isExistByEnCode(String enCode, String id) {
        return this.baseMapper.isExistByEnCode(enCode, id);
    }

    @Override
    @DSTransactional
    public String getNumber(String enCode) throws DataException {
        return this.baseMapper.getNumber(enCode);
    }

    @Override
    public void create(BillRuleEntity entity) {
        this.baseMapper.create(entity);
    }

    @Override
    public boolean update(String id, BillRuleEntity entity) {
        entity.setId(id);
        entity.setLastModifyTime(new Date());
        entity.setLastModifyUserId(UserProvider.getUser().getUserId());
        return this.updateById(entity);
    }

    @Override
    public void delete(BillRuleEntity entity) {
        this.removeById(entity.getId());
    }

    @Override
    public String getBillNumber(String enCode, boolean isCache) throws DataException {
        String strNumber;
        String tenantId = !StringUtil.isEmpty(UserProvider.getUser().getTenantId()) ? UserProvider.getUser().getTenantId() : "";
        if (isCache) {
            String cacheKey = tenantId + UserProvider.getUser().getUserId() + enCode;
            if (!redisUtil.exists(cacheKey)) {
                strNumber = this.getNumber(enCode);
                redisUtil.insert(cacheKey, strNumber);
            } else {
                strNumber = String.valueOf(redisUtil.getString(cacheKey));
            }
        } else {
            strNumber = this.getNumber(enCode);
        }
        return strNumber;
    }

    @Override
    public void useBillNumber(String enCode) {
        String cacheKey = UserProvider.getUser().getTenantId() + UserProvider.getUser().getUserId() + enCode;
        redisUtil.remove(cacheKey);
    }

    @Override
    @DSTransactional
    public ActionResult<Object> importData(BillRuleEntity entity, Integer type) throws DataException {
        if (entity != null) {
            StringJoiner stringJoiner = new StringJoiner("、");
            QueryWrapper<BillRuleEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().eq(BillRuleEntity::getId, entity.getId());
            if (this.count(queryWrapper) > 0) {
                if (Objects.equals(type, 0)) {
                    stringJoiner.add("ID");
                } else {
                    entity.setId(RandomUtil.uuId());
                }
            }
            queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().eq(BillRuleEntity::getEnCode, entity.getEnCode());
            if (this.count(queryWrapper) > 0) {
                stringJoiner.add("编码");
            }
            queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().eq(BillRuleEntity::getFullName, entity.getFullName());
            if (this.count(queryWrapper) > 0) {
                stringJoiner.add("名称");
            }
            if (stringJoiner.length() > 0 && ObjectUtil.equal(type, 1)) {
                String copyNum = UUID.randomUUID().toString().substring(0, 5);
                entity.setFullName(entity.getFullName() + ".副本" + copyNum);
                entity.setEnCode(entity.getEnCode() + copyNum);
            } else if (ObjectUtil.equal(type, 0) && stringJoiner.length() > 0) {
                return ActionResult.fail(stringJoiner.toString() + "重复");
            }
            entity.setCreatorTime(new Date());
            entity.setCreatorUserId(UserProvider.getLoginUserId());
            entity.setLastModifyTime(null);
            entity.setLastModifyUserId(null);
            try {
                this.setIgnoreLogicDelete().removeById(entity);
                entity.setEnabledMark(0);
                entity.setThisNumber(null);
                entity.setOutputNumber(null);
                this.setIgnoreLogicDelete().saveOrUpdate(entity);
            } catch (Exception e) {
                throw new DataException(MsgCode.IMP003.get());
            } finally {
                this.clearIgnoreLogicDelete();
            }
            return ActionResult.success(MsgCode.IMP001.get());
        }
        return ActionResult.fail(MsgCode.IMP004.get());
    }

    @Override
    public List<BillRuleEntity> getListByCategory(String id, Pagination pagination) {
        return this.baseMapper.getListByCategory(id, pagination);
    }
}
