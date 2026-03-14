package jnpf.message.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.ActionResult;
import jnpf.base.service.SuperServiceImpl;
import jnpf.constant.MsgCode;
import jnpf.exception.DataException;
import jnpf.message.entity.MessageTemplateConfigEntity;
import jnpf.message.entity.SmsFieldEntity;
import jnpf.message.entity.TemplateParamEntity;
import jnpf.message.mapper.MessageTemplateConfigMapper;
import jnpf.message.mapper.SmsFieldMapper;
import jnpf.message.mapper.TemplateParamMapper;
import jnpf.message.model.messagetemplateconfig.MessageTemplateConfigForm;
import jnpf.message.model.messagetemplateconfig.MessageTemplateConfigPagination;
import jnpf.message.model.messagetemplateconfig.TemplateParamModel;
import jnpf.message.service.MessageTemplateConfigService;
import jnpf.util.StringUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 消息模板（新）
 * 版本： V3.2.0
 * 版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * 作者： JNPF开发平台组
 * 日期： 2022-08-18
 */
@Service
@RequiredArgsConstructor
public class MessageTemplateConfigServiceImpl extends SuperServiceImpl<MessageTemplateConfigMapper, MessageTemplateConfigEntity> implements MessageTemplateConfigService {

    private final TemplateParamMapper templateParamMapper;
    private final SmsFieldMapper smsFieldMapper;

    @Override
    public List<MessageTemplateConfigEntity> getList(MessageTemplateConfigPagination pagination) {
        return this.baseMapper.getList(pagination);
    }

    @Override
    public List<MessageTemplateConfigEntity> getTypeList(MessageTemplateConfigPagination pagination, String dataType) {
        return this.baseMapper.getTypeList(pagination, dataType);
    }

    @Override
    public MessageTemplateConfigEntity getInfo(String id) {
        return this.baseMapper.getInfo(id);
    }

    @Override
    public MessageTemplateConfigEntity getInfoByEnCode(String enCode, String messageType) {
        return this.baseMapper.getInfoByEnCode(enCode, messageType);
    }

    @Override
    public void create(MessageTemplateConfigEntity entity) {
        this.save(entity);
    }

    @Override
    public boolean update(String id, MessageTemplateConfigEntity entity) {
        entity.setId(id);
        return this.updateById(entity);
    }

    @Override
    public void delete(MessageTemplateConfigEntity entity) {
        if (entity != null) {
            this.removeById(entity.getId());
        }
    }

    @Override
    public List<TemplateParamEntity> getTemplateParamList(String id) {
        QueryWrapper<TemplateParamEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(TemplateParamEntity::getTemplateId, id);
        return templateParamMapper.selectList(queryWrapper);
    }

    @Override
    public List<SmsFieldEntity> getSmsFieldList(String id) {
        QueryWrapper<SmsFieldEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(SmsFieldEntity::getTemplateId, id);
        return smsFieldMapper.selectList(queryWrapper);
    }


    //验证表单唯一字段
    @Override
    public boolean checkForm(MessageTemplateConfigForm form, int i, String id) {
        int total = 0;
        if (ObjectUtil.isNotEmpty(form.getEnCode())) {
            QueryWrapper<MessageTemplateConfigEntity> codeWrapper = new QueryWrapper<>();
            codeWrapper.lambda().eq(MessageTemplateConfigEntity::getEnCode, form.getEnCode());
            if (StringUtils.isNotBlank(id) && !"null".equals(id)) {
                codeWrapper.lambda().ne(MessageTemplateConfigEntity::getId, id);
            }
            if ((int) this.count(codeWrapper) > i) {
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
    public ActionResult<Object> importData(MessageTemplateConfigEntity entity) throws DataException {
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
    public List<TemplateParamModel> getParamJson(String id) {
        MessageTemplateConfigEntity entity = getInfo(id);
        List<TemplateParamModel> paramModelList = new ArrayList<>();
        List<String> smsField = new ArrayList<>();
        if (entity != null) {
            if ("3".equals(entity.getMessageType()) || "7".equals(entity.getMessageType())) {
                List<SmsFieldEntity> smsFieldList = smsFieldMapper.getDetailListByParentId(id);
                for (SmsFieldEntity entity1 : smsFieldList) {
                    if (!"@flowLink".equals(entity1.getField())) {
                        smsField.add(entity1.getField());
                    }
                }
                List<TemplateParamEntity> paramFieldList = templateParamMapper.getDetailListByParentId(id);
                for (TemplateParamEntity entity1 : paramFieldList) {
                    if (smsField.contains(entity1.getField())) {
                        TemplateParamModel paramModel = new TemplateParamModel();
                        paramModel.setTemplateId(entity.getId());
                        paramModel.setTemplateCode(entity.getEnCode());
                        paramModel.setTemplateType(entity.getTemplateType());
                        paramModel.setField(entity1.getField());
                        paramModel.setFieldName(entity1.getFieldName());
                        paramModel.setId(entity1.getId());
                        paramModel.setTemplateName(entity.getFullName());
                        paramModelList.add(paramModel);
                    }
                }
            } else {
                String content = StringUtil.isNotEmpty(entity.getContent()) ? entity.getContent() : "";
                if ("5".equals(entity.getMessageSource()) && "1".equals(entity.getMessageType())) {
                    content = "{@FormDataId}{@FormTemplateId}";
                }
                String title = StringUtil.isNotEmpty(entity.getTitle()) ? entity.getTitle() : "";
                Set<String> list = new HashSet<>();
                list.addAll(regexContent(content));
                list.addAll(regexContent(title));
                List<TemplateParamEntity> paramFieldList = templateParamMapper.getDetailListByParentId(id);
                for (TemplateParamEntity entity1 : paramFieldList) {
                    TemplateParamModel paramModel = new TemplateParamModel();
                    paramModel.setTemplateId(entity.getId());
                    paramModel.setTemplateCode(entity.getEnCode());
                    paramModel.setTemplateType(entity.getTemplateType());
                    paramModel.setField(entity1.getField());
                    paramModel.setFieldName(entity1.getFieldName());
                    paramModel.setId(entity1.getId());
                    paramModel.setTemplateName(entity.getFullName());
                    if (list.contains(entity1.getField()) && !"@FlowLink".equals(entity1.getField())
                            && !"@CreatorUserName".equals(entity1.getField())
                            && !"@SendTime".equals(entity1.getField())
                            && !"@Mandator".equals(entity1.getField())
                            && !"@Mandatary".equals(entity1.getField())
                    ) {
                        paramModelList.add(paramModel);
                    }
                }
            }
        }
        return paramModelList;
    }

    //获取消息内容参数
    public List<String> regexContent(String content) {
        List<String> list = new ArrayList<>();
        String pattern = "\\{([^}]+)\\}";
        Pattern patternList = Pattern.compile(pattern);
        Matcher m = patternList.matcher(content);
        while (m.find()) {
            String group = m.group().replace("{", "").replace("}", "");
            list.add(group);
        }
        return list;
    }
}
