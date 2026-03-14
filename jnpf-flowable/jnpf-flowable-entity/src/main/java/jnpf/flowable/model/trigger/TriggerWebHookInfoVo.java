package jnpf.flowable.model.trigger;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/9/21 15:43
 */
@Data
public class TriggerWebHookInfoVo {
    @Schema(description = "系统生成数据接收接口")
    private String webhookUrl;
    @Schema(description = "系统生成参数接收接口")
    private String requestUrl;
    @Schema(description = "base64未转换16进制字符串")
    private String enCodeStr;
    @Schema(description = "随机字符")
    private String randomStr;
}
