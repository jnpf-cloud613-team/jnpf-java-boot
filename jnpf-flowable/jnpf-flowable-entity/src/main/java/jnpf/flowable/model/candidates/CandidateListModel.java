package jnpf.flowable.model.candidates;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/4/19 10:22
 */
@Data
public class CandidateListModel implements Serializable {
    @Schema(description = "节点编码")
    private String nodeCode;
    @Schema(description = "节点名称")
    private String nodeName;
    @Schema(description = "是否候选人")
    private Boolean isCandidates = false;
    @Schema(description = "是否有候选人")
    private Boolean hasCandidates = false;
    @Schema(description = "已经选择的候选人")
    private String selected;
    @Schema(description = "全部的候选人")
    private List<String> selectIdList = new ArrayList<>();
    @Schema(description = "是否选择分支")
    private Boolean isBranchFlow = false;
}
