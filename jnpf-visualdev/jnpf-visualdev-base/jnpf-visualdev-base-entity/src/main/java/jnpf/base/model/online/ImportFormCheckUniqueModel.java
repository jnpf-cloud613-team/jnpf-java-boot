package jnpf.base.model.online;

import jnpf.database.model.entity.DbLinkEntity;
import jnpf.model.visualjson.TableModel;
import lombok.Data;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 导入验证 表单验证
 *
 * @author JNPF开发平台组
 * @version V3.4.5
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date  2022/11/12
 */
@Data
public class ImportFormCheckUniqueModel {
	private boolean isUpdate;
	private boolean isMain;
	private String id;
	private String dbLinkId;
	private DbLinkEntity linkEntity;
	private String flowId;
	private List<String> flowIdList = new ArrayList<>();
	/**
	 * 主键
	 */
	private Connection connection;
	private Integer primaryKeyPolicy;
	private Boolean logicalDelete = false;
	private List<ImportDataModel> importDataModel = new ArrayList<>();
	private List<TableModel> tableModelList;
	private List<Map<String, Object>> childMap;
	private Integer childIndex;
}
