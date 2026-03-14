package jnpf.flowable.model.templatenode.nodejson;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.flowable.enums.ExtraRuleEnum;
import jnpf.flowable.enums.OperatorEnum;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TimeConfig {

    //----------------------限时------------------------
    /**
     * 开始时间 0-接收时间，1-发起时间，2-表单变量
     */
    @Schema(description = "开始类型")
    private Integer nodeLimit = 0;
    /**
     * 表单字段key
     */
    @Schema(description = "表单字段key")
    private String formField = "";
    /**
     * 限定时长 默认24 （小时）
     */
    @Schema(description = "限定时长")
    private Integer duringDeal = 24;

    //--------------------超时-------------------------
    /**
     * 超时自动审批
     */
    @Schema(description = "超时自动审批")
    private Boolean overAutoApprove = false;
    /**
     * 超时次数
     */
    @Schema(description = "超时次数")
    private Integer overAutoApproveTime = 5;

    /**
     * 超时自动转审
     */
    @Schema(description = "超时自动转审")
    private Boolean overAutoTransfer = false;
    /**
     * 转审超时次数
     */
    @Schema(description = "转审超时次数")
    private Integer overAutoTransferTime = 5;
    /**
     * 转审人类型
     */
    @Schema(description = "转审人类型")
    private Integer overTimeType = OperatorEnum.NOMINATOR.getCode();
    /**
     * 接口主键
     */
    @Schema(description = "接口主键")
    private String interfaceId;
    /**
     * 模块json
     */
    @Schema(description = "模块json")
    private List<TemplateJsonModel> templateJson = new ArrayList<>();
    /**
     * 转审人
     */
    @Schema(description = "转审人")
    private List<String> reApprovers = new ArrayList<>();
    /**
     * 超时审批人，2.同一部门 7.同一角色 3.同一岗位 8.同一分组
     */
    @Schema(description = "超时审批人")
    private Integer overTimeExtraRule = ExtraRuleEnum.ORGANIZE.getCode();

    /**
     * 超时自动结束
     */
    @Schema(description = "超时自动结束")
    private Boolean overAutoFree = false;

    /**
     * 自流超时次数
     */
    @Schema(description = "自流超时次数")
    private Integer overAutoFreeTime = 5;

    //---------------------公共----------------------------------
    /**
     * 超时设置 0.关闭  1.自定义  2.同步发起配置
     */
    @Schema(description = "超时设置")
    private Integer on = 0;
    /**
     * 第一次时间
     * （小时）第一次超时时间默认值0=第一次触发超时事件时间=节点限定时长起始值+节点处理限定时长+设定的第一次超时时间
     */
    @Schema(description = "第一次时间")
    private Integer firstOver = 0;
    /**
     * 时间间隔(提醒、超时)
     */
    @Schema(description = "时间间隔")
    private Integer overTimeDuring = 2;
}
