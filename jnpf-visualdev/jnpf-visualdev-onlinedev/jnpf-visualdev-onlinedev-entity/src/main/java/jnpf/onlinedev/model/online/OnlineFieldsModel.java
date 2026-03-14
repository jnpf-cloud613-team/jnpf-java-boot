package jnpf.onlinedev.model.online;


import lombok.Data;

import java.util.List;


/**
 *在线开发formData
 *
 * @author JNPF开发平台组
 * @version V3.2.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date  2021/8/2
 */
@Data
public class OnlineFieldsModel {
	private StringBuilder sql;
	private List<OnlineColumnFieldModel> mastTableList;
}
