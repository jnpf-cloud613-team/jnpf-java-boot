package jnpf.base.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 系统配置
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2019年9月27日 上午9:18
 */
@Data
@TableName("base_sys_config")
public class SysConfigEntity extends SuperExtendEntity<String> implements Serializable {

    /**
     * 名称
     */
    @TableField("F_FULL_NAME")
    private String fullName;

    /**
     * 键
     */
    @TableField("F_KEY")
    private String fkey;

    /**
     * 值
     */
    @TableField("F_VALUE")
    private String value;

    /**
     * 分类
     */
    @TableField("F_CATEGORY")
    private String category;

}
