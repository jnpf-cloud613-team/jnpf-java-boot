package jnpf.flowable.model.templatenode.nodejson;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 解析引擎
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 */
@Data
public class MsgConfig {
    /**
     * 0.关闭  1.自定义  2.同步发起配置  3.默认
     */
    @Schema(description = "类型")
    private Integer on = 0;
    @Schema(description = "消息主键")
    private String msgId;
    @Schema(description = "接口主键")
    private String interfaceId;
    @Schema(description = "数据")
    private List<SendConfigJson> templateJson = new ArrayList<>();
}
