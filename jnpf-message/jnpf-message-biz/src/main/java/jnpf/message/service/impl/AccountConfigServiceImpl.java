package jnpf.message.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.ActionResult;
import jnpf.base.service.SuperServiceImpl;
import jnpf.constant.MsgCode;
import jnpf.exception.DataException;
import jnpf.message.entity.AccountConfigEntity;
import jnpf.message.mapper.AccountConfigMapper;
import jnpf.message.model.accountconfig.AccountConfigForm;
import jnpf.message.model.accountconfig.AccountConfigPagination;
import jnpf.message.service.AccountConfigService;
import jnpf.util.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 账号配置功能
 * 版本： V3.2.0
 * 版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * 作者： JNPF开发平台组
 * 日期： 2022-08-18
 */
@Service
public class AccountConfigServiceImpl extends SuperServiceImpl<AccountConfigMapper, AccountConfigEntity> implements AccountConfigService {

    @Override
    public List<AccountConfigEntity> getList(AccountConfigPagination accountConfigPagination) {
        return this.getBaseMapper().getList(accountConfigPagination);
    }

    @Override
    public List<AccountConfigEntity> getTypeList(AccountConfigPagination accountConfigPagination, String dataType) {
        return this.getBaseMapper().getTypeList(accountConfigPagination, dataType);
    }


    @Override
    public AccountConfigEntity getInfo(String id) {
        return this.getBaseMapper().getInfo(id);
    }

    @Override
    public void create(AccountConfigEntity entity) {
        this.save(entity);
    }

    @Override
    public boolean update(String id, AccountConfigEntity entity) {
        entity.setId(id);
        return this.updateById(entity);
    }

    @Override
    public void delete(AccountConfigEntity entity) {
        if (entity != null) {
            this.removeById(entity.getId());
        }
    }

    @Override
    public boolean checkForm(AccountConfigForm form, int i, String type, String id) {
        int total = 0;
        if (ObjectUtil.isNotEmpty(form.getEnCode())) {
            QueryWrapper<AccountConfigEntity> codeWrapper = new QueryWrapper<>();
            codeWrapper.lambda().eq(AccountConfigEntity::getEnCode, form.getEnCode());
            codeWrapper.lambda().eq(AccountConfigEntity::getType, type);
            if (StringUtils.isNotBlank(id) && !"null".equals(id)) {
                codeWrapper.lambda().ne(AccountConfigEntity::getId, id);
            }
            total += (int) this.count(codeWrapper);
        }
        int c = 0;
        return total > i + c;
    }

    @Override
    public boolean checkGzhId(String gzhId, int i, String type, String id) {
        int total = 0;
        if (StringUtil.isNotEmpty(gzhId) && !"null".equals(gzhId)) {
            QueryWrapper<AccountConfigEntity> codeWrapper = new QueryWrapper<>();
            codeWrapper.lambda().eq(AccountConfigEntity::getAppId, gzhId);
            codeWrapper.lambda().eq(AccountConfigEntity::getType, type);
            if (StringUtils.isNotBlank(id) && !"null".equals(id)) {
                codeWrapper.lambda().ne(AccountConfigEntity::getId, id);
            }
            total += (int) this.count(codeWrapper);
        }
        int c = 0;
        return total > i + c;
    }

    @Override
    public AccountConfigEntity getInfoByType(String appKey, String type) {
        return this.getBaseMapper().getInfoByType(appKey, type);
    }

    @Override
    public AccountConfigEntity getInfoByEnCode(String enCode, String type) {
        return this.getBaseMapper().getInfoByEnCode(enCode, type);
    }

    @Override
    public List<AccountConfigEntity> getListByType(String type) {
        return this.getBaseMapper().getListByType(type);
    }

    @Override
    public boolean isExistByFullName(String fullName, String id) {
        return this.getBaseMapper().isExistByFullName(fullName, id);
    }

    @Override
    public boolean isExistByEnCode(String enCode, String id, String type) {
        return this.getBaseMapper().isExistByEnCode(enCode, id, type);
    }

    @Override
    public ActionResult<Object> importData(AccountConfigEntity entity) throws DataException {
        if (entity != null) {
            if (isExistByEnCode(entity.getEnCode(), entity.getId(), entity.getType())) {
                return ActionResult.fail(MsgCode.EXIST002.get());
            }
            try {
                this.save(entity);
            } catch (Exception e) {
                throw new DataException(MsgCode.IMP003.get());
            }
            return ActionResult.success(MsgCode.IMP001.get());
        }
        return ActionResult.fail(MsgCode.IMP006.get());
    }
}
