package jnpf.model.document;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
public class DocumentSuserListVO {
    @Schema(description ="主键")
    private String id;
    @Schema(description ="文档主键")
    private String documentId;
    @Schema(description ="用户")
    private String shareUserId;
    @Schema(description ="时间")
    private Date shareTime;
}
