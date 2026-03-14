package jnpf.base.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;


/**
 * 日程安排
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2019年9月26日 上午9:18
 */
@Data
@TableName("base_schedule_user")
public class ScheduleNewUserEntity extends SuperExtendEntity.SuperExtendDEEntity<String> {
    /**
     * 日程id
     */
    @TableField("F_SCHEDULE_ID")
    private String scheduleId;

    /**
     * 用户id
     */
    @TableField("F_TO_USER_ID")
    private String toUserId;

    /**
     * 类型(1-系统添加 2-用户添加)
     */
    @TableField("F_TYPE")
    private Integer type;

}
