package jnpf.base.model.print;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jnpf.base.model.dataset.DataSetForm;
import lombok.Data;

import java.util.List;

/**
 * 打印模板-数据传输对象
 *
 * @author JNPF开发平台组 YY
 * @version V3.2.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月30日
 */
@Data
@Schema(description = "打印表单基础信息")
public class PrintDevFormDTO {

    @Schema(description = "主键id")
    private String id;

    @NotBlank(message = "打印名称必填")
    @Schema(description = "打印名称", required = true)
    private String fullName;

    @Schema(description = "打印编码", required = true)
    private String enCode;

    @NotBlank(message = "打印分类必填")
    @Schema(description = "打印分类", required = true)
    private String category;

    @Schema(description = "打印排序")
    private Long sortCode;

    @Schema(description = "打印说明")
    private String description;

    @Schema(description = "模板内容")
    private String printTemplate;

    @Schema(description = "模板内容")
    private List<DataSetForm> dataSetList;

    @Schema(description = "转换配置")
    private String convertConfig;

    @Schema(description = "全局配置")
    private String globalConfig;

    @Schema(description = "通用-将该模板设为通用(0-表单用，1-业务打印模板用)")
    private Integer commonUse;

    @Schema(description = "发布范围：1-公开，2-权限设置")
    private Integer visibleType;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "图标颜色")
    private String iconBackground;

    @Schema(description = "系统id")
    private String systemId;
}
