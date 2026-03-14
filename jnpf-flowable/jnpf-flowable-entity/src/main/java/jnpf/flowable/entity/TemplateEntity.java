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
@TableName("workflow_template")
public class TemplateEntity extends SuperExtendEntity.SuperExtendDEEntity<String> implements Serializable {

    /**
     * 流程主版本
     */
    @TableField("F_FLOW_ID")
    private String flowId;
    /**
     * 流程主版本
     */
    @TableField("F_VERSION")
    private String version;

    /**
     * 流程编码
     */
    @TableField("F_EN_CODE")
    private String enCode;

    /**
     * 流程名称
     */
    @TableField("F_FULL_NAME")
    private String fullName;

    /**
     * 流程类型，0.标准  1.简流  2.任务 3.自由流
     */
    @TableField("F_TYPE")
    private Integer type;

    /**
     * 流程分类
     */
    @TableField("F_CATEGORY")
    private String category;

    /**
     * 图标
     */
    @TableField("F_ICON")
    private String icon;

    /**
     * 图标背景色
     */
    @TableField("F_ICON_BACKGROUND")
    private String iconBackground;

    /**
     * 流程设置
     */
    @TableField("F_FLOW_CONFIG")
    private String flowConfig;

    /**
     * 流程权限（1全局  2权限）
     */
    @TableField("F_VISIBLE_TYPE")
    private Integer visibleType;

    @TableField("F_FLOWABLE_ID")
    private String flowableId;
    @TableField("F_ACTIVITI_ID")
    private String activitiId;
    @TableField("F_CAMUNDA_ID")
    private String camundaId;

    /**
     * 流程显示类型（0-全局 1-流程 2-菜单）
     */
    @TableField("F_SHOW_TYPE")
    private Integer showType;
    /**
     * 状态(0.未上架,1.上架,2.下架-继续审批，3.下架-隐藏审批)
     */
    @TableField("F_STATUS")
    private Integer status;

    /**
     * 应用主建
     */
    @TableField("F_SYSTEM_ID")
    private String systemId;
}
