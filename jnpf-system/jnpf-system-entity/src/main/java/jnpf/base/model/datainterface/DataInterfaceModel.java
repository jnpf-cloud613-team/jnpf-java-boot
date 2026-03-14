package jnpf.base.model.datainterface;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 自定义参数模型
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021-12-13
 */
@Data
public class DataInterfaceModel extends ParamModel implements Serializable {

    /**
     * 是否为空（0允许，1不允许）
     */
    @Schema(description = "是否为空（0允许，1不允许）")
    private Integer required;

    /**
     * 参数名称
     */
    @Schema(description = "参数名称")
    private String parameter;
    /**
     * 参数来源
     */
    @Schema(description = "参数来源（1-字段，2-自定义，3-为空，4-系统变量）")
    private Integer sourceType = 1;

    /**
     * 表单字段
     */
    @Schema(description = "表单字段")
    private String relationField;
}
