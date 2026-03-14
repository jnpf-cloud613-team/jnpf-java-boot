package jnpf.message.mapper;


import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jnpf.base.mapper.SuperMapper;
import jnpf.message.entity.AccountConfigEntity;
import jnpf.message.model.accountconfig.AccountConfigPagination;
import jnpf.util.StringUtil;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.util.List;

/**
 * 账号配置功能
 * 版本： V3.2.0
 * 版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * 作者： JNPF开发平台组
 * 日期： 2022-08-18
 */
public interface AccountConfigMapper extends SuperMapper<AccountConfigEntity> {

    default List<AccountConfigEntity> getList(AccountConfigPagination accountConfigPagination) {
        return getTypeList(accountConfigPagination, accountConfigPagination.getDataType());
    }

    default List<AccountConfigEntity> getTypeList(AccountConfigPagination accountConfigPagination, String dataType) {
        QueryWrapper<AccountConfigEntity> accountConfigQueryWrapper = new QueryWrapper<>();

        //关键字
        if (StringUtils.isNotBlank(accountConfigPagination.getKeyword()) && !"null".equals(accountConfigPagination.getKeyword())) {
            accountConfigQueryWrapper.lambda().and(t -> t.like(AccountConfigEntity::getEnCode, accountConfigPagination.getKeyword())
                    .or().like(AccountConfigEntity::getFullName, accountConfigPagination.getKeyword()).or().like(AccountConfigEntity::getAddressorName, accountConfigPagination.getKeyword())
                    .or().like(AccountConfigEntity::getSmtpUser, accountConfigPagination.getKeyword()).or().like(AccountConfigEntity::getSmsSignature, accountConfigPagination.getKeyword()));
        }
        //webhook类型
        if (ObjectUtil.isNotEmpty(accountConfigPagination.getWebhookType())) {
            accountConfigQueryWrapper.lambda().eq(AccountConfigEntity::getWebhookType, accountConfigPagination.getWebhookType());
        }
        //渠道
        if (ObjectUtil.isNotEmpty(accountConfigPagination.getChannel())) {
            accountConfigQueryWrapper.lambda().eq(AccountConfigEntity::getChannel, accountConfigPagination.getChannel());
        }
        //状态
        if (ObjectUtil.isNotEmpty(accountConfigPagination.getEnabledMark())) {
            int enabledMark = Integer.parseInt(accountConfigPagination.getEnabledMark());
            accountConfigQueryWrapper.lambda().eq(AccountConfigEntity::getEnabledMark, enabledMark);
        }
        //配置类型
        if (ObjectUtil.isNotEmpty(accountConfigPagination.getType())) {
            accountConfigQueryWrapper.lambda().eq(AccountConfigEntity::getType, accountConfigPagination.getType());
        }

        //排序
        if (StringUtil.isEmpty(accountConfigPagination.getSidx())) {
            accountConfigQueryWrapper.lambda().orderByAsc(AccountConfigEntity::getSortCode).orderByDesc(AccountConfigEntity::getCreatorTime).orderByDesc(AccountConfigEntity::getLastModifyTime);
        } else {
            try {
                String sidx = accountConfigPagination.getSidx();
                String sortOrder = accountConfigPagination.getSort().toLowerCase();
                Field declaredField = AccountConfigEntity.class.getDeclaredField(sidx);
                TableField tableField = declaredField.getAnnotation(TableField.class);
                if (tableField != null && !tableField.value().isEmpty()) {
                    String columnName = tableField.value();
                    if ("asc".equals(sortOrder)) {
                        accountConfigQueryWrapper.orderByAsc(columnName);
                    } else {
                        accountConfigQueryWrapper.orderByDesc(columnName);
                    }
                }
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }
        if (!"1".equals(dataType)) {
            Page<AccountConfigEntity> page = new Page<>(accountConfigPagination.getCurrentPage(), accountConfigPagination.getPageSize());
            IPage<AccountConfigEntity> userIPage = this.selectPage(page, accountConfigQueryWrapper);
            return accountConfigPagination.setData(userIPage.getRecords(), userIPage.getTotal());
        } else {
            return this.selectList(accountConfigQueryWrapper);
        }
    }


    default AccountConfigEntity getInfo(String id) {
        QueryWrapper<AccountConfigEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(AccountConfigEntity::getId, id);
        return this.selectOne(queryWrapper);
    }


    default AccountConfigEntity getInfoByType(String appKey, String type) {
        QueryWrapper<AccountConfigEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(AccountConfigEntity::getType, type);
        queryWrapper.lambda().eq(AccountConfigEntity::getAppKey, appKey);
        return this.selectOne(queryWrapper);
    }

    default AccountConfigEntity getInfoByEnCode(String enCode, String type) {
        QueryWrapper<AccountConfigEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(AccountConfigEntity::getType, type);
        queryWrapper.lambda().eq(AccountConfigEntity::getEnCode, enCode);
        return this.selectOne(queryWrapper);
    }

    default List<AccountConfigEntity> getListByType(String type) {
        QueryWrapper<AccountConfigEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(AccountConfigEntity::getType, type);
        queryWrapper.lambda().eq(AccountConfigEntity::getEnabledMark, 1);
        return this.selectList(queryWrapper);
    }

    default boolean isExistByFullName(String fullName, String id) {
        QueryWrapper<AccountConfigEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(AccountConfigEntity::getFullName, fullName);
        if (!StringUtil.isEmpty(id)) {
            queryWrapper.lambda().ne(AccountConfigEntity::getId, id);
        }
        return this.selectCount(queryWrapper) > 0;
    }

    default boolean isExistByEnCode(String enCode, String id, String type) {
        QueryWrapper<AccountConfigEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(AccountConfigEntity::getEnCode, enCode);
        queryWrapper.lambda().eq(AccountConfigEntity::getType, type);
        if (!StringUtil.isEmpty(id)) {
            queryWrapper.lambda().ne(AccountConfigEntity::getId, id);
        }
        return this.selectCount(queryWrapper) > 0;
    }
}
