package jnpf.onlinedev.entity;


import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jnpf.base.entity.SuperEntity;
import lombok.Data;

/**
 * 在线开发-数据日志实体类
 *
 * @author JNPF开发平台组
 * @version v5.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/8/27 15:50:10
 */
@Data
@TableName("base_visual_log")
public class VisualLogEntity extends SuperEntity<String> {

    /**
     * 模板id
     */
    @TableField("F_MODEL_ID")
    private String modelId;

    /**
     * 日志类型：0-新建，1-编辑
     */
    @TableField("F_TYPE")
    private Integer type;

    /**
     * 数据id
     */
    @TableField("F_DATA_ID")
    private String dataId;

    /**
     * 日志内容
     */
    @TableField("F_DATA_LOG")
    private String dataLog;
}
