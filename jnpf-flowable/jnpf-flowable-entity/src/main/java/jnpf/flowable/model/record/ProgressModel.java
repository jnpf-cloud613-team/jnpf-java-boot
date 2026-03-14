package jnpf.flowable.model.record;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/6/18 17:09
 */
@Data
public class ProgressModel implements Serializable {
    /**
     * 主键
     */
    private String id;
    /**
     * 开始时间
     */
    private Long startTime;
    /**
     * 节点id
     */
    private String nodeId;
    /**
     * 节点编码
     */
    private String nodeCode;
    /**
     * 节点名称
     */
    private String nodeName;
    /**
     * 节点类型
     */
    private String nodeType;
    /**
     * 节点状态， 1-已提交 2-已通过 3-已拒绝 4-审批中.  7等待中、8待办理
     */
    private Integer nodeStatus;
    /**
     * 审批类型（0：或签 1：会签 2：依次审批）
     */
    private Integer counterSign;
    /**
     * 审批人
     */
    private List<UserItem> approver = new ArrayList<>();
    /**
     * 审批人数
     */
    private Integer approverCount = 0;
    /**
     * 显示任务流程按钮
     */
    private Boolean showTaskFlow = false;
    /**
     * 外部节点请求结果
     */
    private Boolean outSideStatus = true;
    /**
     * 外部节点错误提示
     */
    private String errorTip;
    /**
     * 外部节点错误数据
     */
    private String errorData;
    /**
     * 外部节点是否重试
     */
    private Boolean isRetry = false;
    /**
     * 是否有抄送
     */
    private Boolean isCirculate = false;

    /**
     * 审批类型
     */
    private Integer assigneeType;

}
