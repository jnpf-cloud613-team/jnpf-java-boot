package jnpf.base.service;

import jnpf.base.entity.PrintVersionEntity;
import jnpf.base.model.print.PrintDevFormDTO;

import java.util.List;

/**
 * @author JNPF开发平台组
 * @version v5.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/5/6 14:06:40
 */
public interface PrintVersionService extends SuperService<PrintVersionEntity> {

    /**
     * 创建版本
     *
     * @param dto
     */
    void create(PrintDevFormDTO dto);

    /**
     * 获取版本列表
     *
     * @param templateId
     * @return
     */
    List<PrintVersionEntity> getList(String templateId);

    /**
     * 复制版本（点击新增打印版本）
     *
     * @param versionId
     */
    String copyVersion(String versionId);

    /**
     * 根据打印id删除版本
     *
     * @param templateId
     * @return
     */
    void removeByTemplateId(String templateId);
}
