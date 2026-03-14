package jnpf.base.model.online;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * 在线开发导入数据结果集
 * @author JNPF开发平台组
 * @version V3.4.3
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date  2022/9/6
 */
@Data
public class ExcelImportModel {
	/**
	 * 导入成功条数
	 */
	private int snum;
	/**
	 * 导入失败条数
	 */
	private int fnum;
	/**
	 * 导入结果状态(0,成功  1，失败)
	 */
	private int resultType;

	/**
	 * 失败结果
	 */
	private List<Map<String, Object>> failResult;

	/**
	 * 数据字段
	 */
	private List<Map<String, Object>> headerRow;

	/**
	 * 集成调用哦个
	 */
	private List<VisualdevModelDataInfoVO> dataInfoList = new ArrayList<>();
}
