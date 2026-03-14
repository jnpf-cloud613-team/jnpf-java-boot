package jnpf.flowable.model.candidates;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/4/20 11:06
 */
@Data
public class CandidateUserVo implements Serializable {
    @Schema(description = "主键")
    private String id;
    @Schema(description = "名称")
    private String fullName;
    @Schema(description = "头像")
    private String headIcon;
    @Schema(description = "组织")
    private String organize;
}
