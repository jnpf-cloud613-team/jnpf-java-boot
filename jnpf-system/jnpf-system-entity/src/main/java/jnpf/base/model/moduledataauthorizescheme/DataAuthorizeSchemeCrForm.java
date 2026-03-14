package jnpf.base.model.moduledataauthorizescheme;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/12 15:31
 */
@Data
public class DataAuthorizeSchemeCrForm {
    @NotBlank(message = "方案名称不能为空")
    private String fullName;

    private Object conditionJson;

    private String conditionText;

    private String moduleId;

    @NotBlank(message = "方案编码不能为空")
    private String enCode;

    /**
     * 全部数据标识
     */
    private Integer allData;
    /**
     * 分组匹配逻辑
     */
    private String matchLogic;
}
