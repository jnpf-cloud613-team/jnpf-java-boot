package jnpf.flowable.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jnpf.base.entity.SuperExtendEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * 经办记录
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/4/23 9:13
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("workflow_record")
public class RecordEntity extends SuperExtendEntity<String> implements Serializable {
    /**
     * 节点名称
     */
    @TableField("f_node_name")
    private String nodeName;
    /**
     * 节点编码
     */
    @TableField("f_node_code")
    private String nodeCode;
    /**
     * 节点id
     */
    @TableField("f_node_id")
    private String nodeId;
    /**
     * 经办类型
     */
    @TableField("f_handle_type")
    private Integer handleType;
    /**
     * 经办人员
     */
    @TableField("f_handle_id")
    private String handleId;
    /**
     * 经办时间
     */
    @TableField("f_handle_time")
    private Date handleTime;
    /**
     * 经办理由
     */
    @TableField("f_handle_opinion")
    private String handleOpinion;
    /**
     * 经办主键
     */
    @TableField("f_operator_id")
    private String operatorId;
    /**
     * 任务id
     */
    @TableField("f_task_id")
    private String taskId;
    /**
     * 签名图片
     */
    @TableField("f_sign_img")
    private String signImg;
    /**
     * 状态，0.进行数据 -1.作废
     */
    @TableField("f_status")
    private Integer status;
    /**
     * 流转操作人
     */
    @TableField("f_handle_user_id")
    private String handleUserId;
    /**
     * 经办文件
     */
    @TableField("f_file_list")
    private String fileList;
    /**
     * 拓展字段
     */
    @TableField("f_expand_field")
    private String expandField;
}
