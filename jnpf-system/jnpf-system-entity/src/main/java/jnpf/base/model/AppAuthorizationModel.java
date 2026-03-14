package jnpf.base.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class AppAuthorizationModel {

    @Schema(description = "应用id")
    private String systemId;
    @Schema(description = "创建者id")
    private String createUserId;
    @Schema(description = "开发者id")
    private List<String> devUsers;
    @Schema(description = "开发者类型")
    private Integer isAllDevUser;
}
