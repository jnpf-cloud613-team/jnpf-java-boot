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
@TableName("workflow_version")
public class TemplateJsonEntity extends SuperExtendEntity<String> implements Serializable {

    /**
     * 流程模板id
     */
    @TableField("F_TEMPLATE_ID")
    private String templateId;

    /**
     * 流程版本
     */
    @TableField("F_VERSION")
    private String version;

    /**
     * 状态(0.设计,1.启用,2.历史)
     */
    @TableField("F_STATUS")
    private Integer state;

    /**
     * 流程模板
     */
    @TableField("F_XML")
    private String flowXml;

    /**
     * flowable部署ID
     */
    @TableField("f_flowable_id")
    private String flowableId;
    /**
     * activiti部署ID
     */
    @TableField("f_activiti_id")
    private String activitiId;
    /**
     * camunda部署ID
     */
    @TableField("f_camunda_id")
    private String camundaId;
    /**
     * 消息配置id
     */
    @TableField("f_send_config_ids")
    private String sendConfigIds;

}
