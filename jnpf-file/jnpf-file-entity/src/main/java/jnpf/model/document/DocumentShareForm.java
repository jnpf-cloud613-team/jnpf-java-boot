package jnpf.model.document;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class DocumentShareForm {

    @Schema(description = "用户id列表")
    private List<String> userIds;

    @Schema(description = "共享文件id列表")
    private List<String> ids;

    private String creatorUserId;
}
