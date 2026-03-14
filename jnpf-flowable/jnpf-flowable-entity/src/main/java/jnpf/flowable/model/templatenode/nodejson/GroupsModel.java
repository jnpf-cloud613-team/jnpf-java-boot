package jnpf.flowable.model.templatenode.nodejson;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.flowable.enums.FieldEnum;
import lombok.Data;

@Data
public class GroupsModel {
    //1.字段 2.公式
    @Schema(description = "类型")
    private int fieldType = FieldEnum.FIELD.getCode();
    @Schema(description = "类型")
    //1.数据里面获取 //2.解析表达式
    private String field;
    @Schema(description = "类型")
    //1.字段 2.自定义 3.系统参数 4.流程参数
    private int fieldValueType = FieldEnum.CUSTOM.getCode();
    @Schema(description = "类型")
    //1.数据里面获取 2.直接获取
    private Object fieldValue;
    @Schema(description = "属性")
    private String symbol;
    @Schema(description = "类型")
    private String jnpfKey;
    @Schema(description = "类型")
    private String fieldValueJnpfKey;
}
