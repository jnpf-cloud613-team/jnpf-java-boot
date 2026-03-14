package jnpf.message.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.message.entity.SynThirdInfoEntity;
import lombok.Data;

@Data
public class SynThirdInfoVo extends SynThirdInfoEntity {
    @Schema(description = "本系统对象名称")
    private String systemObjectName;
}
