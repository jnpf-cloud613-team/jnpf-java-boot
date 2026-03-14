package jnpf.portal.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.entity.SuperExtendEntity;
import lombok.Data;

/**
 *  门户
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date  2021/3/16
 */
@Data
@TableName("base_portal")
public class PortalEntity extends SuperExtendEntity.SuperExtendDEEntity<String> {

	@Schema(description = "名称")
	@TableField("F_FULL_NAME")
	private String fullName;

	@Schema(description = "编码")
	@TableField("F_EN_CODE")
	private String enCode;

	@Schema(description = "分类(数据字典维护)")
	@TableField("F_CATEGORY")
	private String category;

	@Schema(description = "类型(0-页面设计,1-自定义路径)")
	@TableField("F_TYPE")
	private Integer type;

	@Schema(description = "静态页面路径")
	@TableField("F_CUSTOM_URL")
	private String customUrl;

	@Schema(description = "类型(0-页面,1-外链)")
	@TableField("F_LINK_TYPE")
	private Integer linkType;

	@TableField("F_STATE")
	private Integer state;

	@Schema(description = "移动锁定(0-未锁定,1-锁定)")
	@TableField("F_ENABLED_LOCK")
	private Integer enabledLock;

	/**
	 * 发布时勾选平台类型
	 */
	@TableField("F_PLATFORM_RELEASE" )
	private String platformRelease;

	@TableField("F_SYSTEM_ID")
	private String systemId;
}
