package jnpf.message.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.ActionResult;
import jnpf.base.service.SuperServiceImpl;
import jnpf.constant.MsgCode;
import jnpf.exception.DataException;
import jnpf.message.entity.SendConfigTemplateEntity;
import jnpf.message.entity.SendMessageConfigEntity;
import jnpf.message.mapper.SendConfigTemplateMapper;
import jnpf.message.mapper.SendMessageConfigMapper;
import jnpf.message.model.sendmessageconfig.SendMessageConfigForm;
import jnpf.message.model.sendmessageconfig.SendMessageConfigPagination;
import jnpf.message.service.SendMessageConfigService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 消息发送配置
 * 版本： V3.2.0
 * 版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * 作者： JNPF开发平台组
 * 日期： 2022-08-19
 */
@Service
@RequiredArgsConstructor
public class SendMessageConfigServiceImpl extends SuperServiceImpl<SendMessageConfigMapper, SendMessageConfigEntity> implements SendMessageConfigService {

    private final SendConfigTemplateMapper sendConfigTemplateMapper;

    @Override
    public List<SendMessageConfigEntity> getList(SendMessageConfigPagination sendMessageConfigPagination, String dataType) {
        return this.baseMapper.getList(sendMessageConfigPagination, dataType);
    }

    @Override
    public List<SendMessageConfigEntity> getSelectorList(SendMessageConfigPagination sendMessageConfigPagination) {
        return this.baseMapper.getSelectorList(sendMessageConfigPagination);
    }


    @Override
    public SendMessageConfigEntity getInfo(String id) {
        return this.baseMapper.getInfo(id);
    }

    @Override
    public SendMessageConfigEntity getInfoByEnCode(String enCode) {
        return this.baseMapper.getInfoByEnCode(enCode);
    }

    @Override
    public void create(SendMessageConfigEntity entity) {
        this.save(entity);
    }

    @Override
    public boolean update(String id, SendMessageConfigEntity entity) {
        entity.setId(id);
        return this.updateById(entity);
    }

    @Override
    public void delete(SendMessageConfigEntity entity) {
        if (entity != null) {
            this.removeById(entity.getId());
        }
    }

    @Override
    public List<SendConfigTemplateEntity> getSendConfigTemplateList(String id) {
        QueryWrapper<SendConfigTemplateEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(SendConfigTemplateEntity::getSendConfigId, id);
        queryWrapper.lambda().orderByDesc(SendConfigTemplateEntity::getSortCode);
        return sendConfigTemplateMapper.selectList(queryWrapper);
    }

    //列表子表数据方法


    //验证表单唯一字段
    @Override
    public boolean checkForm(SendMessageConfigForm form, int i, String id) {
        int total = 0;
        if (ObjectUtil.isNotEmpty(form.getEnCode())) {
            QueryWrapper<SendMessageConfigEntity> enCodeWrapper = new QueryWrapper<>();
            enCodeWrapper.lambda().eq(SendMessageConfigEntity::getEnCode, form.getEnCode());
            if (StringUtils.isNotBlank(id) && !"null".equals(id)) {
                enCodeWrapper.lambda().ne(SendMessageConfigEntity::getId, id);
            }
            if ((int) this.count(enCodeWrapper) > i) {
                total++;
            }
        }
        return total > 0;
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
    public ActionResult<Object> importData(SendMessageConfigEntity entity) throws DataException {
        if (entity != null) {
            if (isExistByEnCode(entity.getEnCode(), entity.getId())) {
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

    @Override
    public List<SendMessageConfigEntity> getList(List<String> idList) {
        return this.baseMapper.getList(idList);
    }


    public List<String> getIdList(String usedId) {
        return this.baseMapper.getIdList(usedId);
    }

    @Override
    public boolean idUsed(String id) {
        return this.baseMapper.idUsed(id);
    }

}
