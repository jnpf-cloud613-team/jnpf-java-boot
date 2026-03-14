package jnpf.flowable.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jnpf.base.entity.SuperExtendEntity;
import lombok.Data;

import java.io.Serializable;

/**
 * 流程引擎
 *
 * @author JNPF开发平台组
 * @version V3.4.2
 * @copyright 引迈信息技术有限公司
 * @date 2022年7月11日 上午9:18
 */
@Data
@TableName("workflow_node")
public class TemplateNodeEntity extends SuperExtendEntity<String> implements Serializable {

    /**
     * 流程版本主键
     */
    @TableField("F_FLOW_ID")
    private String flowId;
    /**
     * 表单主键
     */
    @TableField("f_form_id")
    private String formId;
    /**
     * 节点类型
     */
    @TableField("f_node_type")
    private String nodeType;
    /**
     * 节点编码
     */
    @TableField("f_node_code")
    private String nodeCode;
    /**
     * 节点属性
     */
    @TableField("f_node_json")
    private String nodeJson;

}
