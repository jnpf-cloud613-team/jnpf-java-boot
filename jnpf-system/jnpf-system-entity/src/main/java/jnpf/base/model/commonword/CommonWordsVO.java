package jnpf.base.model.commonword;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * 审批常用语 Entity
 *
 * @author JNPF开发平台组 YanYu
 * @version v3.4.6
 * @copyrignt 引迈信息技术有限公司
 * @date 2023-01-06
 */
@Data
@Schema(description = "CommonWords对象", name = "审批常用语")
public class CommonWordsVO implements Serializable {

    @Schema(description = "自然主键")
    private String id;
    @Schema(description = "应用id")
    private List<String> systemIds;
    @Schema(description = "应用名称")
    private String systemNames;
    @Schema(description = "常用语")
    private String commonWordsText;
    @Schema(description = "常用语类型(0:系统,1:个人)")
    private Integer commonWordsType;
    @Schema(description = "排序")
    private Long sortCode;
    @Schema(description = "有效标志")
    private Integer enabledMark;
    @Schema(description = "使用次数")
    private Long usesNum;

    public void setSystemIds(String systemIds) {
        if(systemIds != null){
            this.systemIds = Arrays.asList(systemIds.split(","));
        }
    }

}
