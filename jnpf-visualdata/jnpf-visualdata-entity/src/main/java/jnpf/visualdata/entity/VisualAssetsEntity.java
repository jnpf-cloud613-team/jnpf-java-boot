package jnpf.visualdata.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

import java.util.Date;

/**
 * 静态资源
 *
 * @author JNPF开发平台组
 * @version V3.5.0
 * @copyright 引迈信息技术有限公司
 * @date 2023年12月28日
 */
@Data
@TableName("blade_visual_assets")
public class VisualAssetsEntity {

    /** 主键 */
    @TableId("id")
    private String id;

    /** 资源名称 */
    @TableField("assetsName")
    private String assetsName;
    /** 资源大小 1M */
    @TableField("assetsSize")
    private String assetsSize;
    /** 资源上传时间 */
    @TableField("assetsTime")
    private Date assetsTime;
    /** 资源后缀名 */
    @TableField("assetsType")
    private String assetsType;
    /** 资源地址 */
    @TableField("assetsUrl")
    private String assetsUrl;
    @TableField("SYSTEM_ID")
    private String systemId;

    /**
     * 租户id
     */
    @TableField(value = "f_tenant_id" , fill = FieldFill.INSERT)
    private String tenantId;

}
