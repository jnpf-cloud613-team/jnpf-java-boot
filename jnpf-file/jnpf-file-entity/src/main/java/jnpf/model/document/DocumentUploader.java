package jnpf.model.document;
import com.alibaba.fastjson.annotation.JSONField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.io.Serializable;

@Data
public class DocumentUploader implements Serializable {
    @Schema(description ="父级id")
    private String parentId;
    @JSONField(serialize = false)
    @Schema(description ="文件")
    private MultipartFile file;

    @Schema(description ="文件")
    private String fileName;

    @Schema(description ="流程任务id")
    private String taskId;

    public DocumentUploader(MultipartFile file, String parentId, String taskId) {
        this.file = file;
        this.parentId = parentId;
        this.taskId = taskId;
    }

    public DocumentUploader(MultipartFile file, String parentId) {
        this.file = file;
        this.parentId = parentId;
    }

    public DocumentUploader() {
    }

}
