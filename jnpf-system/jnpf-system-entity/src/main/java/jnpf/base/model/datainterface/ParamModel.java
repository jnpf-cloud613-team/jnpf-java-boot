package jnpf.base.model.datainterface;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
public class ParamModel extends FieldModel implements Serializable {

    @Schema(description = "列说明")
    private String fieldName;

    /**
     * 参数类型
     * 字符串
     * 整型
     * 日期时间
     * 浮点
     * 长整型
     * 文本
     */
    @Schema(description = "参数类型")
    private String dataType;

}
