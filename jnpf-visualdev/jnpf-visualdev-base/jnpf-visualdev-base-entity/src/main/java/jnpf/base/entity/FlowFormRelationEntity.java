package jnpf.base.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 流程表单关联表
 *
 * @author JNPF开发平台组
 * @version V3.4.2
 * @copyright 引迈信息技术有限公司
 * @date 2022/10/26 15:58:02
 */
@Data
@TableName("flow_form_relation")
public class FlowFormRelationEntity extends SuperExtendEntity<String> {

    /**
     * 流程版本id
     */
    @TableField("F_FLOW_ID")
    private String flowId;
    /**
     * 表单id
     */
    @TableField("F_FORM_ID")
    private String formId;

}