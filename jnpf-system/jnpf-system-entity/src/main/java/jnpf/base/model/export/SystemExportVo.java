package jnpf.base.model.export;

import jnpf.base.entity.SystemEntity;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 系统菜单导出模型
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021-06-17
 */
@Data
public class SystemExportVo {
    private SystemEntity systemEntity;
    private List<VisualExportVo> visualList = new ArrayList<>();
    private List<PortalExportDataVo> portalList = new ArrayList<>();
    private List<Object> reportList = new ArrayList<>();
    private List<Object> oldReportList = new ArrayList<>();
    private List<PrintExportVo> printList = new ArrayList<>();
    private List<TemplateExportVo> templateList = new ArrayList<>();
    private VisualScreenExportVo visualScreen;
}
