
package jnpf.message.service;

import jnpf.base.service.SuperService;


import java.util.*;

import jnpf.base.ActionResult;
import jnpf.exception.DataException;
import jnpf.message.entity.AccountConfigEntity;
import jnpf.message.model.accountconfig.*;

/**
 * 账号配置功能
 * 版本： V3.2.0
 * 版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * 作者： JNPF开发平台组
 * 日期： 2022-08-18
 */
public interface AccountConfigService extends SuperService<AccountConfigEntity> {


    List<AccountConfigEntity> getList(AccountConfigPagination accountConfigPagination);

    List<AccountConfigEntity> getTypeList(AccountConfigPagination accountConfigPagination, String dataType);


    AccountConfigEntity getInfo(String id);

    void delete(AccountConfigEntity entity);

    void create(AccountConfigEntity entity);

    boolean update(String id, AccountConfigEntity entity);

    /**
     *
     * @param type 配置类型 1：站内信，2：邮件，3：短信，4：钉钉，5：企业微信，6：webhook
     * @return
     */
    List<AccountConfigEntity> getListByType(String type);

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
    boolean isExistByEnCode(String enCode, String id,String type);

    /**
     * 账号配置导入
     *
     * @param entity 实体对象
     * @return ignore
     * @throws DataException ignore
     */
    ActionResult<Object> importData(AccountConfigEntity entity) throws DataException;

//  子表方法

    //列表子表数据方法

    //验证表单
    boolean checkForm(AccountConfigForm form, int i,String type,String id);

    /**
     * 验证微信公众号原始id唯一性
     * @param gzhId 微信公众号原始id
     * @param i
     * @param type
     * @param id
     * @return
     */
    boolean checkGzhId(String gzhId, int i,String type,String id);

    AccountConfigEntity getInfoByType(String appKey, String type);

    AccountConfigEntity getInfoByEnCode(String enCode,String type);
}
