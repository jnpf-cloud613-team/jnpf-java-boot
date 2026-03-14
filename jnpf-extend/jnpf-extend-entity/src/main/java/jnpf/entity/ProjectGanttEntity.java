package jnpf.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jnpf.base.entity.SuperExtendEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 项目计划
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2019年9月26日 上午9:18
 */
@Data
@TableName("ext_project_gantt")
public class ProjectGanttEntity extends SuperExtendEntity.SuperExtendDEEntity<String> {

    /**
     * 项目上级
     */
    @TableField("F_PARENT_ID")
    private String parentId;

    /**
     * 项目主键
     */
    @TableField("F_PROJECT_ID")
    private String projectId;

    /**
     * 项目类型
     */
    @TableField("F_TYPE")
    private Integer type;

    /**
     * 项目编码
     */
    @TableField("F_EN_CODE")
    private String enCode;

    /**
     * 项目名称
     */
    @TableField("F_FULL_NAME")
    private String fullName;

    /**
     * 项目工期
     */
    @TableField("F_TIME_LIMIT")
    private BigDecimal timeLimit;

    /**
     * 项目标记
     */
    @TableField("F_SIGN")
    private String sign;

    /**
     * 标记颜色
     */
    @TableField("F_SIGN_COLOR")
    private String signColor;

    /**
     * 开始时间
     */
    @TableField("F_START_TIME")
    private Date startTime;

    /**
     * 结束时间
     */
    @TableField("F_END_TIME")
    private Date endTime;

    /**
     * 当前进度
     */
    @TableField("F_SCHEDULE")
    private Integer schedule;

    /**
     * 负责人
     */
    @TableField("F_MANAGER_IDS")
    private String managerIds;

}
