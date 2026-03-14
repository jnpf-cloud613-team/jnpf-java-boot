package jnpf.model.document;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class DocumentInfoVO {
    @Schema(description ="文件名称")
    private String fullName;
    @Schema(description ="主键id")
    private String id;
    @Schema(description ="文档分类")
    private Integer type;
    @Schema(description ="文档父级")
    private String parentId;

    @Schema(description ="文档下载地址")
    private String uploaderUrl;
    @Schema(description ="文件路径")
    private String filePath;

}
