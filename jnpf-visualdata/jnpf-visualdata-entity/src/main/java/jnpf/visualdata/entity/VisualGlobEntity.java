package jnpf.visualdata.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 全局变量
 *
 * @author JNPF开发平台组
 * @version V3.5.0
 * @copyright 引迈信息技术有限公司
 * @date 2023年12月28日
 */
@Data
@TableName("blade_visual_glob")
public class VisualGlobEntity  {

    /** 主键 */
    @TableId("ID")
    private String id;

    /** 变量名称 */
    @TableField("globalName")
    private String globalName;

    /** 变量Key */
    @TableField("globalKey")
    private String globalKey;

    /** 组变量值 */
    @TableField("globalValue")
    private String globalValue;
    @TableField("SYSTEM_ID")
    private String systemId;

    /**
     * 租户id
     */
    @TableField(value = "f_tenant_id" , fill = FieldFill.INSERT)
    private String tenantId;


}
