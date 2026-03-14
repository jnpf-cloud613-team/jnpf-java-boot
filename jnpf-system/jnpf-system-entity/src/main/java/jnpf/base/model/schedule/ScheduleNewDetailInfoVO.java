package jnpf.base.model.schedule;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


/**
 * @author JNPF开发平台组
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 */
@Data
public class ScheduleNewDetailInfoVO extends ScheduleNewListVO {

    @Schema(description ="参与人")
    private String toUserIds;
    @Schema(description ="附件")
    private String files;

}
