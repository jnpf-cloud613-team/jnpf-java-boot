package jnpf.flowable.model.templatenode;

import lombok.Data;

import java.io.Serializable;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/5/16 16:56
 */
@Data
public class ButtonModel implements Serializable {
    /**
     * 通过按钮开关
     */
    private Boolean hasAuditBtn = false;

    /**
     * 拒绝按钮开关
     */
    private Boolean hasRejectBtn = false;

    /**
     * 退回按钮开关
     */
    private Boolean hasBackBtn = false;

    /**
     * 加签按钮开关
     */
    private Boolean hasFreeApproverBtn = false;

    /**
     * 减签按钮开关
     */
    private Boolean hasReduceApproverBtn = false;

    /**
     * 转审按钮开关
     */
    private Boolean hasTransferBtn = false;

    /**
     * 协办按钮开关
     */
    private Boolean hasAssistBtn = false;

    /**
     * 审批暂存按钮开关
     */
    private Boolean hasSaveAuditBtn = false;

    /**
     * 暂存
     */
    private Boolean hasSaveBtn = false;

    /**
     * 提交按钮开关
     */
    private Boolean hasSubmitBtn = false;

    /**
     * 发起撤回按钮开关
     */
    private Boolean hasRecallLaunchBtn = false;

    /**
     * 催办按钮开关
     */
    private Boolean hasPressBtn = false;

    /**
     * 撤销按钮开关
     */
    private Boolean hasRevokeBtn = false;

    /**
     * 审批撤回按钮开关
     */
    private Boolean hasRecallAuditBtn = false;

    /**
     * 签收按钮开关
     */
    private Boolean hasSignBtn = false;

    /**
     * 办理按钮开关
     */
    private Boolean hasTransactBtn = false;

    /**
     * 退签按钮开关
     */
    private Boolean hasReduceSignBtn = false;

    /**
     * 终止按钮开关
     */
    private Boolean hasCancelBtn = false;

    /**
     * 协办保存
     */
    private Boolean hasAssistSaveBtn = false;

    /**
     * 指派
     */
    private Boolean hasAssignBtn = false;

    /**
     * 复活
     */
    private Boolean hasActivateBtn = false;

    /**
     * 暂停
     */
    private Boolean hasPauseBtn = false;

    /**
     * 恢复
     */
    private Boolean hasRebootBtn = false;

    /**
     * 打印
     */
    private Boolean hasPrintBtn = false;

    /**
     * 归档按钮
     */
    private Boolean hasFileBtn = false;

    /**
     * 查看发起表单
     */
    private Boolean hasViewStartFormBtn = false;

    /**
     * 委托发起按钮
     */
    private Boolean hasDelegateSubmitBtn = false;

    /**
     * 代理标识
     */
    private Boolean proxyMark = false;
}
