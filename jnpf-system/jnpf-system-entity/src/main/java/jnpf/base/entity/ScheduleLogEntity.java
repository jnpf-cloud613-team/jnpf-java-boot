package jnpf.base.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 日程安排
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2019年9月26日 上午9:18
 */
@Data
@TableName("base_schedule_log")
public class ScheduleLogEntity extends SuperExtendEntity.SuperExtendDEEntity<String> {
    /**
     * 分类
     */
    @TableField("F_CATEGORY")
    private String category;

    /**
     * 紧急程度
     */
    @TableField("F_URGENT")
    private Integer urgent;

    /**
     * 标题
     */
    @TableField("F_TITLE")
    private String title;

    /**
     * 内容
     */
    @TableField("F_CONTENT")
    private String content;

    /**
     * 全天
     */
    @TableField("F_ALL_DAY")
    private Integer allDay;

    /**
     * 开始时间
     */
    @TableField("F_START_DAY")
    private Date startDay;

    /**
     * 开始日期
     */
    @TableField("F_START_TIME")
    private String startTime;

    /**
     * 结束时间
     */
    @TableField("F_END_DAY")
    private Date endDay;

    /**
     * 结束日期
     */
    @TableField("F_END_TIME")
    private String endTime;

    /**
     * 时长
     */
    @TableField("F_DURATION")
    private Integer duration;

    /**
     * 颜色
     */
    @TableField("F_COLOR")
    private String color;

    /**
     * 提醒时长 -2不提醒 -1开始 其他是分钟
     */
    @TableField("F_REMINDER_TIME")
    private Integer reminderTime;

    /**
     * 提醒方式(1-默认 2-自定义)
     */
    @TableField("F_REMINDER_TYPE")
    private Integer reminderType;

    /**
     * 发送配置id
     */
    @TableField("F_SEND_CONFIG_ID")
    private String send;

    /**
     * 发送配置名称
     */
    @TableField("F_SEND_CONFIG_NAME")
    private String sendConfigName;

    /**
     * 重复提醒 1.不重复 2.每天重复 3.每周重复 4.每月重复 5.每年重复
     */
    @TableField("F_REPETITION")
    private Integer repetition;

    /**
     * 结束重复
     */
    @TableField("F_REPEAT_TIME")
    private Date repeatTime;

    /**
     * 推送时间
     */
    @TableField("F_PUSH_TIME")
    private Date pushTime;

    /**
     * 分组id
     */
    @TableField("F_GROUP_ID")
    private String groupId;

    /**
     * 参与用户
     */
    @TableField("F_USER_ID")
    private String userId;

    /**
     * 日程id
     */
    @TableField("F_SCHEDULE_ID")
    private String scheduleId;

    /**
     * 操作类型 1.新增 2.修改 3.删除 4.参与人删除
     */
    @TableField("F_OPERATION_TYPE")
    private String operationType;
}
