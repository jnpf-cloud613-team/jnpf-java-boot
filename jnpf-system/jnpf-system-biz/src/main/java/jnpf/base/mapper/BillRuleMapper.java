package jnpf.base.mapper;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jnpf.base.Pagination;
import jnpf.base.entity.BillRuleEntity;
import jnpf.base.model.billrule.BillRulePagination;
import jnpf.exception.DataException;
import jnpf.util.*;

import java.util.List;


/**
 * 单据规则
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
public interface BillRuleMapper extends SuperMapper<BillRuleEntity> {

    default List<BillRuleEntity> getList(BillRulePagination pagination) {
        // 定义变量判断是否需要使用修改时间倒序
        boolean flag = false;
        QueryWrapper<BillRuleEntity> queryWrapper = new QueryWrapper<>();
        if (!StringUtil.isEmpty(pagination.getKeyword())) {
            flag = true;
            queryWrapper.lambda().and(
                    t -> t.like(BillRuleEntity::getFullName, pagination.getKeyword())
                            .or().like(BillRuleEntity::getEnCode, pagination.getKeyword())
            );
        }
        if (!StringUtil.isEmpty(pagination.getCategoryId())) {
            flag = true;
            queryWrapper.lambda().and(
                    t -> t.like(BillRuleEntity::getCategory, pagination.getCategoryId())
            );
        }
        if (pagination.getEnabledMark() != null) {
            flag = true;
            queryWrapper.lambda().eq(BillRuleEntity::getEnabledMark, pagination.getEnabledMark());
        }
        // 排序
        queryWrapper.lambda().orderByAsc(BillRuleEntity::getSortCode).orderByDesc(BillRuleEntity::getCreatorTime);
        if (flag) {
            queryWrapper.lambda().orderByDesc(BillRuleEntity::getLastModifyTime);
        }
        Page<BillRuleEntity> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
        IPage<BillRuleEntity> userPage = this.selectPage(page, queryWrapper);
        return pagination.setData(userPage.getRecords(), page.getTotal());
    }

    default List<BillRuleEntity> getList() {
        QueryWrapper<BillRuleEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(BillRuleEntity::getEnabledMark, 1);
        // 排序
        queryWrapper.lambda().orderByAsc(BillRuleEntity::getSortCode).orderByDesc(BillRuleEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }

    default BillRuleEntity getInfo(String id) {
        QueryWrapper<BillRuleEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(BillRuleEntity::getId, id);
        return this.selectOne(queryWrapper);
    }

    default boolean isExistByFullName(String fullName, String id) {
        QueryWrapper<BillRuleEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(BillRuleEntity::getFullName, fullName);
        if (!StringUtil.isEmpty(id)) {
            queryWrapper.lambda().ne(BillRuleEntity::getId, id);
        }
        return this.selectCount(queryWrapper) > 0;
    }

    default boolean isExistByEnCode(String enCode, String id) {
        QueryWrapper<BillRuleEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(BillRuleEntity::getEnCode, enCode);
        if (!StringUtil.isEmpty(id)) {
            queryWrapper.lambda().ne(BillRuleEntity::getId, id);
        }
        return this.selectCount(queryWrapper) > 0;
    }


    default String getNumber(String enCode) throws DataException {
        StringBuilder strNumber = new StringBuilder();
        QueryWrapper<BillRuleEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(BillRuleEntity::getEnCode, enCode);
        BillRuleEntity entity = this.selectOne(queryWrapper);
        if (entity != null) {
            Integer startNumber = StringUtil.isEmpty(entity.getStartNumber()) ? 0 : Integer.parseInt(entity.getStartNumber());
            //拼接单据编码
            strNumber.append(StringUtil.isNotEmpty(entity.getPrefix()) ? entity.getPrefix() : "");
            if (ObjectUtil.equal(entity.getType(), 2)) {
                // 随机数编号
                if (ObjectUtil.equal(entity.getRandomType(), 1)) {
                    strNumber.append(cn.hutool.core.util.RandomUtil.randomNumbers(entity.getRandomDigit()));
                } else {
                    strNumber.append(cn.hutool.core.util.RandomUtil.randomStringUpper(entity.getRandomDigit()));
                }
            } else if (ObjectUtil.equal(entity.getType(), 3)) {
                // UUID
                strNumber.append(IdUtil.randomUUID().toUpperCase());
            } else {
                // 时间格式
                String dateFor = entity.getDateFormat();
                String dateForValue = "no";
                switch (dateFor) {
                    case "YYYY":
                        dateForValue = "yyyy";
                        break;
                    case "YYYYMM":
                        dateForValue = "yyyyMM";
                        break;
                    case "YYYYMMDD":
                        dateForValue = "yyyyMMdd";
                        break;
                    case "YYYYMMDDHH":
                        dateForValue = "yyyyMMddHH";
                        break;
                    case "YYYYMMDDHHmm":
                        dateForValue = "yyyyMMddHHmm";
                        break;
                    case "YYYYMMDDHHmmss":
                        dateForValue = "yyyyMMddHHmmss";
                        break;
                    case "YYYYMMDDHHmmssSSS":
                        dateForValue = "yyyyMMddHHmmssSSS";
                        break;
                    default:
                        break;
                }
                //处理隔天流水号归0
                if (entity.getOutputNumber() != null) {
                    String serialDate = "";
                    entity.setThisNumber(entity.getThisNumber() + 1);
                    if (!"no".equals(dateForValue)) {
                        String thisDate = DateUtil.dateNow(dateForValue);
                        int suffixLength = entity.getSuffix() != null ? entity.getSuffix().length() : 0;
                        try {
                            serialDate = entity.getOutputNumber().substring((entity.getOutputNumber().length() - dateForValue.length() - entity.getDigit() - suffixLength), (entity.getOutputNumber().length() - entity.getDigit() - suffixLength));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (!serialDate.equals(thisDate)) {
                            entity.setThisNumber(0);
                        }
                    }
                } else {
                    entity.setThisNumber(0);
                }
                if (!"no".equals(dateForValue)) {
                    strNumber.append(DateUtil.dateNow(dateForValue));
                }
                strNumber.append(PadUtil.padRight(String.valueOf(startNumber + entity.getThisNumber()), entity.getDigit(), '0'));
            }
            strNumber.append(StringUtil.isNotEmpty(entity.getSuffix()) ? entity.getSuffix() : "");
            //更新流水号
            entity.setOutputNumber(strNumber.toString());
            this.updateById(entity);
        } else {
            throw new DataException("单据规则不存在");
        }
        return strNumber.toString();
    }

    default void create(BillRuleEntity entity) {
        entity.setId(RandomUtil.uuId());
        entity.setCreatorUserId(UserProvider.getUser().getUserId());
        this.insert(entity);
    }

    default List<BillRuleEntity> getListByCategory(String id, Pagination pagination) {
        // 定义变量判断是否需要使用修改时间倒序
        boolean flag = false;
        QueryWrapper<BillRuleEntity> queryWrapper = new QueryWrapper<>();
        if (!StringUtil.isEmpty(pagination.getKeyword())) {
            flag = true;
            queryWrapper.lambda().and(
                    t -> t.like(BillRuleEntity::getFullName, pagination.getKeyword())
                            .or().like(BillRuleEntity::getEnCode, pagination.getKeyword())
            );
        }
        if (!StringUtil.isEmpty(id)) {
            flag = true;
            queryWrapper.lambda().eq(BillRuleEntity::getCategory, id);
        }
        queryWrapper.lambda().eq(BillRuleEntity::getEnabledMark, 1);
        // 排序
        queryWrapper.lambda().orderByAsc(BillRuleEntity::getSortCode).orderByDesc(BillRuleEntity::getCreatorTime);
        if (flag) {
            queryWrapper.lambda().orderByDesc(BillRuleEntity::getLastModifyTime);
        }
        Page<BillRuleEntity> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
        IPage<BillRuleEntity> userPage = this.selectPage(page, queryWrapper);
        return pagination.setData(userPage.getRecords(), page.getTotal());
    }
}
