package jnpf.flowable.model.operator;

import jnpf.flowable.model.util.FlowNature;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 加签参数
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/4/30 10:33
 */
@Data
public class AddSignModel implements Serializable {
    /**
     * 加签人
     */
    private List<String> addSignUserIdList = new ArrayList<>();
    /**
     * 加签类型 1.前 2 后
     */
    private Integer addSignType = FlowNature.BEFORE;
    /**
     * 审批类型（0：或签 1：会签  2：依次审批）
     */
    private Integer counterSign = FlowNature.FIXED_APPROVER;
    /**
     * 会签比例
     */
    private Integer auditRatio = 100;
    /**
     * 加签层级
     */
    private Integer level = 1;
}
