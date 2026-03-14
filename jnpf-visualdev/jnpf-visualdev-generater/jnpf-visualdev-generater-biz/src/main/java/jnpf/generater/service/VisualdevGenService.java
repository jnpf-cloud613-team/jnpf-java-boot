package jnpf.generater.service;


import jnpf.base.entity.VisualdevEntity;
import jnpf.base.model.DownloadCodeForm;
import jnpf.base.service.SuperService;

/**
 * 可视化开发功能表
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021-04-02
 */
public interface VisualdevGenService extends SuperService<VisualdevEntity> {

    /**
     * 代码生成v3
     *
     * @param visualdevEntity  可视化开发功能
     * @param downloadCodeForm 下载相关信息
     * @return 下载文件名
     * @throws Exception ignore
     */
    String codeGengerateV3(VisualdevEntity visualdevEntity, DownloadCodeForm downloadCodeForm);
}

