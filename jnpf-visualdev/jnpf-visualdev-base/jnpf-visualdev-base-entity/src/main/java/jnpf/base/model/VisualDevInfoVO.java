package jnpf.base.model;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021/3/16
 */
@Data
@Schema(description = "功能设计详情模型")
@AllArgsConstructor
@NoArgsConstructor
public class VisualDevInfoVO {
    @Schema(description = "主键")
    private String id;
    @Schema(description = "名称")
    private String fullName;
    @Schema(description = "编码")
    private String enCode;
    @Schema(description = "分类(数据字典维护)")
    private String category;
    @Schema(description = "类型(1-表单设计,2-系统表单)")
    private Integer type;
    @Schema(description = "描述")
    private String description;
    @Schema(description = "表单配置JSON")
    private String formData;
    @Schema(description = "列表配置JSON")
    private String columnData;
    @Schema(description = "app列表配置JSON")
    private String appColumnData;
    @Schema(description = "关联的表")
    private String tables;
    @Schema(description = "状态")
    private Integer state;
    @Schema(description = "关联数据连接id")
    private String dbLinkId;
    @Schema(description = "页面类型（1、纯表单，2、表单加列表，4、数据视图）")
    private String webType;
    @Schema(description = "排序")
    private Long sortCode;
    @Schema(description = "启用流程")
    private Integer enableFlow;
    @Schema(description = "流程引擎json")
    private String flowTemplateJson;
    @Schema(description = "接口id")
    private String interfaceId;
    @Schema(description = "接口名称")
    private String interfaceName;
    @Schema(description = "接口参数")
    private String interfaceParam;

    //以下系统表单属性
    @Schema(description = "Web地址")
    private String urlAddress;

    @Schema(description = "APP地址")
    private String appUrlAddress;

    @Schema(description = "接口路径")
    private String interfaceUrl;

    @Schema(description = "web页面地址")
    private String webAddress;

    @Schema(description = "app页面地址")
    private String appAddress;

    @Schema(description = "按钮设置json")
    private String buttonData;

    @Schema(description = "app按钮设置json")
    private String appButtonData;

    public VisualDevInfoVO(String id, String enCode) {
        this.id = id;
        this.enCode = enCode;
    }
}
