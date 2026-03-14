package jnpf.base.model.schedule;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * @author JNPF开发平台组
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 */
@Data
public class ScheduleNewInfoVO extends ScheduleNewListVO {

    @Schema(description ="参与人")
    private List<String> toUserIds;
    @Schema(description ="附件")
    private String files;

}
