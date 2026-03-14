package jnpf.base.service;

import jnpf.base.Pagination;
import jnpf.base.entity.SmsTemplateEntity;
import jnpf.base.SmsModel;

import java.util.List;

/**
 * @author Administrator
 * @description 针对表【base_sms_template】的数据库操作Service
 * @createDate 2021-12-09 10:12:52
 */
public interface SmsTemplateService extends SuperService<SmsTemplateEntity> {

    /**
     * 列表（不分页）
     *
     * @return
     */
    List<SmsTemplateEntity> getList(String keyword);

    /**
     * 列表
     *
     * @param pagination 条件
     * @return 单据规则列表
     */
    List<SmsTemplateEntity> getList(Pagination pagination);

    /**
     * 信息
     *
     * @param id 主键值
     * @return 单据规则
     */
    SmsTemplateEntity getInfo(String id);

    /**
     * 创建
     *
     * @param entity 实体
     */
    void create(SmsTemplateEntity entity);

    /**
     * 更新
     *
     * @param id     主键值
     * @param entity 实体对象
     * @return ignore
     */
    boolean update(String id, SmsTemplateEntity entity);

    /**
     * 删除
     *
     * @param entity 实体
     */
    void delete(SmsTemplateEntity entity);

    /**
     * 判断模板编号是否重复
     *
     * @param templateName
     * @param id
     * @return
     */
    boolean isExistByTemplateName(String templateName, String id);

    /**
     * 判断模板编号是否重复
     *
     * @param enCode
     * @param id
     * @return
     */
    boolean isExistByEnCode(String enCode, String id);

    /**
     * 获取短信配置
     * @return
     */
    SmsModel getSmsConfig();
}
