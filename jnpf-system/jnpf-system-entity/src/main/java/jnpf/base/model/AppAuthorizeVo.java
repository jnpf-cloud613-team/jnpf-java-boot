package jnpf.base.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class AppAuthorizeVo {
    @Schema(description = "1-全部开发者，0-自定义")
    private Integer isAllDevUser;
    @Schema(description = "创建者名称")
    private String fullName;
    @Schema(description = "创建者id")
    private String createUserId;
    @Schema(description = "用户数据")
    private List<String> devUsers;
}
