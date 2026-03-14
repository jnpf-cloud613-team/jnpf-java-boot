package jnpf.base.model.template;
import lombok.Data;

/**
 * 权限控制字段
 * @author JNPF开发平台组
 * @version V3.2
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date  2021/10/6
 */
@Data
public class AuthorityModel {
		/**
		 * 列表权限
		 */
		private Boolean useColumnPermission;
		/**
		 * 表单权限
		 */
		private Boolean useFormPermission;
		/**
		 * 按钮权限
		 */
		private Boolean useBtnPermission;
		/**
		 * 数据权限
		 */
		private Boolean useDataPermission;
}
