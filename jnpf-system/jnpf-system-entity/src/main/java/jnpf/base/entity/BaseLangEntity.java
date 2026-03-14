package jnpf.base.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * @author JNPF开发平台组
 * @version v5.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/6/20 10:02:18
 */
@Data
@TableName("base_language")
public class BaseLangEntity extends SuperExtendEntity.SuperExtendDEEntity<String> implements Serializable {

    /**
     * 翻译标记id
     */
    @TableField("f_group_id")
    private String groupId;

    /**
     * 翻译标记
     */
    @TableField("f_en_code")
    private String enCode;

    /**
     * 语种翻译内容
     */
    @TableField("f_full_name")
    private String fullName;


    /**
     * 语种
     */
    @TableField("f_language")
    private String language;

    /**
     * 类别:0-客户端，1-java服务端，2-net服务端，
     */
    @TableField("f_type")
    private Integer type;
}
