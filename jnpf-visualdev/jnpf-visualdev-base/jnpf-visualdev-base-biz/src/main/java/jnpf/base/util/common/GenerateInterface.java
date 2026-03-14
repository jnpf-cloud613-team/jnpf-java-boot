package jnpf.base.util.common;

import java.util.List;

/**
 * @author JNPF开发平台组
 * @version V3.5.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021/5/31
 */
public interface GenerateInterface {

    /**
     * 获取 前端 及 后端模板
     *
     * @param templatePath
     * @param type
     * @param hasImport
     * @return
     */
    List<String> getTemplates(String templatePath, int type, boolean hasImport);

    /**
     * 获取副子表model、list模板
     *
     * @param isChild
     * @return
     */
    List<String> getChildTemps(boolean isChild);
}
