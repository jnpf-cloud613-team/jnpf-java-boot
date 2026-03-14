package jnpf.base.entity;

import lombok.Data;

import java.util.Date;

/**
 * 打印模板-流程经办记录
 *
 * @author JNPF开发平台组 YY
 * @version V3.2.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月30日
 */
@Data
public class OperatorRecordEntity {
    /**
     * 节点名称
     */
    private String nodeName;
    /**
     * 节点编码
     */
    private String nodeCode;
    /**
     * 节点id
     */
    private String nodeId;
    /**
     * 经办类型
     */
    private Integer handleType;
    /**
     * 经办人员
     */
    private String handleId;
    /**
     * 经办时间
     */
    private Date handleTimeOrigin;
    /**
     * 经办理由
     */
    private String handleOpinion;
    /**
     * 经办主键
     */
    private String operatorId;
    /**
     * 任务id
     */
    private String taskId;
    /**
     * 签名图片
     */
    private String signImg;
    /**
     * 状态，0.进行数据 1.加签数据 3.已办不显示数据 -1.作废
     */
    private Integer status;
    /**
     * 流转操作人
     */
    private String handleUserId;
    /**
     * 经办文件
     */
    private String fileList;

    /**
     * 经办人员
     */
    private String userName;
    /**
     * 执行动作
     */
    private Integer handleStatus;
    /**
     * 经办时间（时间戳）
     */
    private Long handleTime;
}
