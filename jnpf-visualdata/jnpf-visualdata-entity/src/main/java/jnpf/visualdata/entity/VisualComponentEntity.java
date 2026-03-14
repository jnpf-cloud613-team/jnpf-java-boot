package jnpf.visualdata.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 大屏组件库
 *
 * @author JNPF开发平台组
 * @version V3.5.0
 * @copyright 引迈信息技术有限公司
 * @date 2023年7月7日
 */
@Data
@TableName("blade_visual_component")
public class VisualComponentEntity {

    /** 主键 */
    @TableId("ID")
    private String id;

    /** 组件名称 */
    @TableField("name")
    private String name;

    /** 组件内容 */
    @TableField("content")
    private String content;

    /** 组件类型 */
    @TableField("type")
    private Integer type;

    /** 组件图片 */
    @TableField("img")
    private String img;
    @TableField("SYSTEM_ID")
    private String systemId;

    /**
     * 租户id
     */
    @TableField(value = "f_tenant_id" , fill = FieldFill.INSERT)
    private String tenantId;


}
