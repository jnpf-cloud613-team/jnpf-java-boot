package jnpf.base.model.export;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.model.dataset.DataSetInfo;
import jnpf.base.model.dataset.TableTreeModel;
import jnpf.util.treeutil.SumTree;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "打印模板详情")
public class PrintExportVo {

    @Schema(description = "模板id")
    private String id;

    @Schema(description = "版本id")
    private String versionId;

    @Schema(description = "名称")
    private String fullName;

    @Schema(description = "编码")
    private String enCode;

    @Schema(description = "分类")
    private String category;

    @Schema(description = "状态：0-未发布，1-已发布，2-已修改")
    private Integer state;

    @Schema(description = "模板内容")
    private String printTemplate;

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

    @Schema(description = "排序")
    private Long sortCode;

    @Schema(description = "说明")
    private String description;

    @Schema(description = "应用id")
    private String systemId;

    @Schema(description = "模板内容")
    private List<DataSetInfo> dataSetList;

    @Schema(description = "表字段列表")
    private List<SumTree<TableTreeModel>> fieldList;
}
