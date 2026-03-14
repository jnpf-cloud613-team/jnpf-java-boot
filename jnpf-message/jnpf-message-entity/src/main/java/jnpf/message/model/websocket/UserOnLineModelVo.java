package jnpf.message.model.websocket;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.message.model.UserOnlineModel;
import lombok.Data;

@Data
public class UserOnLineModelVo extends UserOnlineModel {
    @Schema(description = "是否当前设备")
    private Boolean isCurrent;
}
