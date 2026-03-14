package jnpf.portal.model;

import lombok.Data;

/**
 * 可视化列表模型
 *
 * @author JNPF开发平台组
 * @version V3.2.8
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date  2021/11/20
 */
@Data
public class PortalListModel {
	private String category;
	private Long creatorTime;
	private String creatorUser;
	private String enCode;
	private Integer enabledMark;
	private String fullName;
	private String id;
	private Integer type;
	private Long lastModifyTime;
	private String lastModifyUser;
	private Long sortCode;
}
