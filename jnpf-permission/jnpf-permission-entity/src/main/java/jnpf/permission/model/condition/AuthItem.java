package jnpf.permission.model.condition;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthItem {
    private String field;
    private String fieldId;
    private String fieldName;
    private String id;
    private String fullName;
    private String jnpfKey;
    private String symbol;
    private String symbolName;
    private Integer fieldValueType;
    private String fieldValue;
    private String fieldValueJnpfKey;
    private String cellKey;
    private String tableName;
    private Boolean required;
    private Boolean multiple;
    private Boolean disabled;
    @JSONField(name = "__config__")
    private AuthItemConfig config;
}
