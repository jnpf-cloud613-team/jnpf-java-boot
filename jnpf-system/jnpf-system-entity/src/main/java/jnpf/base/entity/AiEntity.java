package jnpf.base.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 个人签名
 *
 * @author JNPF开发平台组
 * @copyright 引迈信息技术有限公司
 * @date 2022年9月2日 上午9:18
 */
@Data
@TableName("base_ai")
public class AiEntity extends SuperExtendEntity.SuperExtendDEEntity<String> {

    /**
     * 名称
     */
    @TableField("f_full_name")
    private String fullName;

    /**
     * 模型
     */
    @TableField("f_model")
    private String model;

    /**
     * 路径
     */
    @TableField("f_baseUrl")
    private String baseUrl;

    /**
     * 凭证
     */
    @TableField("f_credential")
    private String credential;

}

