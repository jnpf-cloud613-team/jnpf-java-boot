package jnpf.base.model.dbsync;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

/**
 * 类功能
 *
 * @author JNPF开发平台组 YanYu
 * @version v3.4.5
 * @copyrignt 引迈信息技术有限公司
 * @date 2023-01-05
 */
@Data
@Accessors(chain = true)
public class DbSyncPrintForm {

    @NotBlank
    @Schema(description = "被同步库连接")
    private String dbLinkFrom;
    @Schema(description = "同步至库类型")
    private String dbTypeTo;
    @Schema(description = "批量同步表名集合")
    private List<String> dbTableList;
    @Schema(description = "转换规则")
    private Map<String, String> convertRuleMap;
    @Schema(description = "输出路径")
    private String outPath;
    @Schema(description = "输出路径")
    private String outFileName;
    @Schema(description = "打印类型")
    private String printType;
    @Schema(description = "多表开关")
    private Boolean multiTabFlag;


}
