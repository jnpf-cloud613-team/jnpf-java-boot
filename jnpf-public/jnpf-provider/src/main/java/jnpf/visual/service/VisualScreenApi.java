package jnpf.visual.service;

import jnpf.base.model.export.VisualScreenExportVo;

/**
 * 大屏设计api
 *
 * @author JNPF开发平台组
 * @version v6.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2025/8/11 15:59:38
 */
public interface VisualScreenApi {

    VisualScreenExportVo getExportList(String systemId);

    boolean importCopy(VisualScreenExportVo vo, String systemId);

    void deleteBySystemId(String systemId);
}
