

package jnpf.message.model.sendmessageconfig;


import com.alibaba.fastjson.annotation.JSONField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @版本： V3.2.0
 * @版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @作者： JNPF开发平台组
 * @日期： 2022-08-19
 */
@Data
public class SendConfigListVO {
    @Schema(description = "主键")
    private String id;

    /**
     * 名称
     **/
    @Schema(description = "名称")
    @JSONField(name = "fullName")
    private String fullName;

    /**
     * 编码
     **/
    @Schema(description = "编码")
    @JSONField(name = "enCode")
    private String enCode;

    @Schema(description = "类型")
    @JSONField(name = "messageSource")
    private String messageSource;

    @Schema(description = "模板json")
    private Object templateJson;

}
