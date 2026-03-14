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
 * @since 2024/4/19 10:24
 */
@Data
public class CandidateCheckVo implements Serializable {
    @Schema(description = "节点")
    private List<CandidateListModel> list = new ArrayList<>();
    /**
     * 1.有分支 2.没有分支有候选人 3.没有分支也没有候选人 4.自由审批
     */
    private Integer type = 3;
    /**
     * 自行结束
     */
    private Boolean oneSelfEndApproval = true;
}
