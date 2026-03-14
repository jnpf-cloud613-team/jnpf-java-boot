
package jnpf.message.service;

import jnpf.base.service.SuperService;

import java.util.*;

import jnpf.base.ActionResult;
import jnpf.exception.DataException;
import jnpf.message.entity.SendConfigTemplateEntity;
import jnpf.message.entity.SendMessageConfigEntity;
import jnpf.message.model.sendmessageconfig.*;

/**
 * 消息发送配置
 * 版本： V3.2.0
 * 版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * 作者： JNPF开发平台组
 * 日期： 2022-08-19
 */
public interface SendMessageConfigService extends SuperService<SendMessageConfigEntity> {

    List<SendMessageConfigEntity> getList(SendMessageConfigPagination sendMessageConfigPagination, String dataType);

    List<SendMessageConfigEntity> getSelectorList(SendMessageConfigPagination sendMessageConfigPagination);


    SendMessageConfigEntity getInfo(String id);

    SendMessageConfigEntity getInfoByEnCode(String enCode);

    void delete(SendMessageConfigEntity entity);

    void create(SendMessageConfigEntity entity);

    boolean update(String id, SendMessageConfigEntity entity);

    List<SendConfigTemplateEntity> getSendConfigTemplateList(String id);

    boolean checkForm(SendMessageConfigForm form, int i,String id);

    boolean isExistByFullName(String fullName, String id);

    boolean isExistByEnCode(String enCode, String id);

    /**
     * 消息发送配置导入
     *
     * @param entity 实体对象
     * @return ignore
     * @throws DataException ignore
     */
    ActionResult<Object> importData(SendMessageConfigEntity entity) throws DataException;

    List<SendMessageConfigEntity> getList(List<String> idList);

    boolean idUsed(String id);

}
