package jnpf.flowable.model.templatenode.nodejson;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.flowable.enums.PrintEnum;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PrintConfig {
    /**
     * 开启打印
     */
    @Schema(description = "开启打印")
    private Boolean on = false;
    /**
     * 模板
     */
    @Schema(description = "模板")
    private List<String> printIds = new ArrayList<>();
    /**
     * 1：不限制  2：节点结束  3：流程结束  4：条件设置
     */
    @Schema(description = "打印配置")
    private Integer conditionType = PrintEnum.NONE.getCode();
    /**
     * 条件设置
     */
    @Schema(description = "条件设置")
    private List<ProperCond> conditions = new ArrayList<>();
    /**
     * 逻辑
     */
    @Schema(description = "逻辑")
    private String matchLogic = "and";
}
