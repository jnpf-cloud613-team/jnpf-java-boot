package jnpf.flowable.model.templatenode.nodejson;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.flowable.enums.FieldEnum;
import lombok.Data;

import java.io.Serializable;

/**
 * 解析引擎
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 */
@Data
public class TemplateJsonModel implements Serializable {

    @Schema(description = "接口字段")
    public String field;
    @Schema(description = "目标字段")
    private String targetField;

    @Schema(description = "名称")
    public String fieldName;
    @Schema(description = "名称")
    private String targetFieldLabel;

    @Schema(description = "数据源字段")
    private String sourceValue;
    @Schema(description = "数据源字段")
    public String relationField;


    @Schema(description = "消息主键")
    private String msgTemplateId;
    @Schema(description = "参数来源")
    private Integer sourceType = FieldEnum.FIELD.getCode();

}
