package jnpf.base.model.moduledataauthorizescheme;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class DataAuthorizeSchemeInfoVO {

    @Schema(description = "主键")
    private String id;

    @Schema(description = "名称")
    private String fullName;

    @Schema(description = "条件文本")
    private String conditionText;

    @Schema(description = "条件json")
    private String conditionJson;

    @Schema(description = "功能主键")
    private String moduleId;

    @Schema(description = "编码")
    private String enCode;

    /**
     * ȫ�����ݱ�ʶ
     */
    @Schema(description = "是否全部数据")
    private Integer allData;

    @Schema(description = "分组匹配逻辑")
    private String matchLogic;
}
