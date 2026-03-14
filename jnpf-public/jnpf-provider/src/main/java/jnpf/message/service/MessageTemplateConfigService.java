
package jnpf.message.service;


import jnpf.base.service.SuperService;

import java.util.*;

import jnpf.base.ActionResult;
import jnpf.exception.DataException;
import jnpf.message.entity.MessageTemplateConfigEntity;
import jnpf.message.entity.SmsFieldEntity;
import jnpf.message.entity.TemplateParamEntity;
import jnpf.message.model.messagetemplateconfig.*;

/**
 * 消息模板（新）
 * 版本： V3.2.0
 * 版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * 作者： JNPF开发平台组
 * 日期： 2022-08-18
 */
public interface MessageTemplateConfigService extends SuperService<MessageTemplateConfigEntity> {

    List<MessageTemplateConfigEntity> getList(MessageTemplateConfigPagination pagination);

    List<MessageTemplateConfigEntity> getTypeList(MessageTemplateConfigPagination pagination, String dataType);

    MessageTemplateConfigEntity getInfo(String id);

    MessageTemplateConfigEntity getInfoByEnCode(String enCode,String messageType);

    void delete(MessageTemplateConfigEntity entity);

    void create(MessageTemplateConfigEntity entity);

    boolean update(String id, MessageTemplateConfigEntity entity);

    List<TemplateParamEntity> getTemplateParamList(String id);

    List<SmsFieldEntity> getSmsFieldList(String id);

    boolean checkForm(MessageTemplateConfigForm form, int i,String id);

    /**
     * 验证名称
     *
     * @param fullName 名称
     * @param id       主键值
     * @return ignore
     */
    boolean isExistByFullName(String fullName, String id);

    /**
     * 验证编码
     *
     * @param enCode 编码
     * @param id     主键值
     * @return ignore
     */
    boolean isExistByEnCode(String enCode, String id);

    /**
     * 消息模板导入
     *
     * @param entity 实体对象
     * @return ignore
     * @throws DataException ignore
     */
    ActionResult<Object> importData(MessageTemplateConfigEntity entity) throws DataException;

    /**
     * 获取模板被引用的参数（消息模板参数数据用子表保存）
     * @param id 模板id
     * @return
     */
    List<TemplateParamModel> getParamJson(String id);
}
