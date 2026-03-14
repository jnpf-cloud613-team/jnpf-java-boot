package jnpf.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jnpf.base.entity.SuperExtendEntity;
import lombok.Data;

import java.util.Date;

/**
 *
 * 工作日志分享
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2019年9月26日 上午9:18
 */
@Data
@TableName("ext_work_log_share")
public class WorkLogShareEntity extends SuperExtendEntity<String> {

    /**
     * 日志主键
     */
    @TableField("F_WORK_LOG_ID")
    private String workLogId;

    /**
     * 共享人员
     */
    @TableField("F_SHARE_USER_ID")
    private String shareUserId;

    /**
     * 共享时间
     */
    @TableField("F_SHARE_TIME")
    private Date shareTime;

}
