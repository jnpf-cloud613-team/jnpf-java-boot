package jnpf.onlinedev.model;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 *
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @author JNPF开发平台组
 * @date 2021/3/16
 */
@Data
@Schema(description="功能数据创建表单")
public class VisualdevModelDataCrForm {
    @Schema(description = "判断新增")
    private String id;
    @Schema(description = "引擎主键")
    private String flowId;
    @Schema(description = "数据内容")
    private String data;
    @Schema(description = "状态")
    private String status;
    @Schema(description = "流程候选人列表")
    private Map<String, List<String>> candidateList;
    @Schema(description = "流程紧急度")
    private Integer flowUrgent = 1;
    @Schema(description = "是否外链")
    private Boolean isLink = false;

    @Schema(description = "菜单id")
    private String menuId;
}
