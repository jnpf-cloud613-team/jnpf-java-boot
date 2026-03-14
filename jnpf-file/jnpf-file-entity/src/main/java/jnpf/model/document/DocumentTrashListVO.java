package jnpf.model.document;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class DocumentTrashListVO {
    @Schema(description = "主键id")
    private String id;
    @Schema(description = "文件名称")
    private String fullName;
    @Schema(description = "文件id")
    private String documentId;
    @Schema(description = "删除日期")
    private String deleteTime;
    @Schema(description = "大小")
    private String fileSize;
    @Schema(description = "类型：0-文件夹，1-文件")
    private Integer type;
    @Schema(description ="后缀名")
    private String fileExtension;
}
