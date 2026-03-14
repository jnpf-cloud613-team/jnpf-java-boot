package jnpf.base.model;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "下载代码表单")
public class DownloadCodeForm {

    @Schema(description = "所属模块")
    private String module;

    @Schema(description = "模块包名")
    private String modulePackageName;

    @Schema(description = "主功能备注")
    private String description;

    @Schema(description = "数据源id")
    private String dataSourceId;

    @Schema(description = "主表表名：用于文件夹创建")
    private String mainClassName;

    @Schema(description = "是否流程：0否，1-是")
    private Integer enableFlow;

    @Schema(description = "是否对比：-true,false")
    private boolean contrast = false;
}


