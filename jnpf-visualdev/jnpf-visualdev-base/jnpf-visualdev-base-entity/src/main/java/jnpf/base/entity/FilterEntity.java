package jnpf.base.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;


@Data
@TableName("base_visual_filter")
public class FilterEntity extends SuperExtendEntity<String> {
    /**
     * 在线和代码生成记录主键
     */
    @TableField("F_MODULE_ID")
    private String moduleId;

    /**
     * 过滤配置
     */
    @TableField("F_CONFIG")
    private String config;

    /**
     * 过滤配置app
     */
    @TableField("F_CONFIG_APP")
    private String configApp;
}
