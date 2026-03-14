package jnpf.portal.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.entity.SuperEntity;
import lombok.Data;

import java.io.Serializable;


/**
 * <p>
 *
 * </p>
 *
 * @author YanYu
 * @since 2023-04-19
 */
@Data
@TableName("base_portal_data")
@Schema(description = "PortalData对象")
public class PortalDataEntity extends SuperEntity<String> implements Serializable {

    @Schema(description = "门户ID")
    @TableField("F_PORTAL_ID")
    private String portalId;

    @Schema(description = "PC:网页端 APP:手机端")
    @TableField("F_PLATFORM")
    private String platform;

    @Schema(description = "表单配置JSON")
    @TableField("F_FORM_DATA")
    private String formData;

    @Schema(description = "系统ID")
    @TableField("F_SYSTEM_ID")
    private String systemId;

    @Schema(description = "类型（mod：模型、custom：自定义）")
    @TableField("F_TYPE")
    private String type;

}
