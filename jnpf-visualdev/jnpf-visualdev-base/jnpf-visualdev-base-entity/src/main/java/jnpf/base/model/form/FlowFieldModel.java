package jnpf.base.model.form;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 流程设计
 *
 * @author JNPF开发平台组
 * @version V3.4.2
 * @copyright 引迈信息技术有限公司
 * @date 2022/9/23 15:10:27
 */
@Data
@Accessors(chain = true)
@Schema(description="字段模型")
public class FlowFieldModel {
    /**
     *__vModel__
     */
    @Schema(description = "字段id")
    String filedId;
    /**
     *__config__.label
     */
    @Schema(description = "字段名称")
    String filedName;
    /**
     *__config__.jnpfKey
     */
    @Schema(description = "字段jnpfkey")
    String jnpfKey;
    /**
     *__config__.required
     */
    @Schema(description = "字段是否必填")
    String required;
}
