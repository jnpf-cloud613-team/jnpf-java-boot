package jnpf.base.service;

import jnpf.base.entity.VisualKitEntity;
import jnpf.base.model.visualkit.KitPagination;
import jnpf.base.model.visualkit.KitTreeVo;
import jnpf.base.model.visualkit.VisualKitForm;

import java.util.List;

/**
 * 表单套件
 *
 * @author JNPF开发平台组
 * @version v5.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/8/22 11:10:30
 */
public interface VisualKitService extends SuperService<VisualKitEntity> {
    /**
     * 列表查询
     *
     * @param page
     * @return
     */
    List<VisualKitEntity> getList(KitPagination page);

    /**
     * 名称编码是否重复
     *
     * @param visualKitEntity
     * @param fullNameCheck
     * @param encodeCheck
     */
    void saveCheck(VisualKitEntity visualKitEntity, Boolean fullNameCheck, Boolean encodeCheck);

    Boolean isExistByEnCode(String enCode, String id);

    /**
     * 创建
     *
     * @param form
     */
    void create(VisualKitForm form);

    /**
     * 修改
     *
     * @param id
     * @param form
     */
    boolean update(String id, VisualKitForm form);

    List<KitTreeVo> selectorList();

    /**
     * 复制
     *
     * @param id
     */
    void actionsCopy(String id);

    /**
     * 导入套件
     *
     * @param entity
     * @param type
     */
    String importData(VisualKitEntity entity, Integer type);
}
