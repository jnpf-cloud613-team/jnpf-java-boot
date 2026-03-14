package jnpf.base.service;

import jnpf.base.model.ai.VisualAiModel;

/**
 * 在线开发ai接口
 *
 * @author JNPF开发平台组
 * @version v5.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/12/2 10:10:10
 */
public interface VisualAiService {

    /**
     * ai生成表单模板
     *
     * @param keyword
     */
    VisualAiModel form(String keyword);
}
