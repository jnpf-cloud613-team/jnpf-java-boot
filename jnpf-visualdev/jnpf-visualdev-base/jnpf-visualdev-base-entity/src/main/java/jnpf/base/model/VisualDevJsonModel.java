package jnpf.base.model;


import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.database.model.query.SuperJsonModel;
import jnpf.model.visualjson.FieLdsModel;
import jnpf.model.visualjson.FormDataModel;
import jnpf.model.visualjson.TableModel;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * json格式化对象（在线开发对象）
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2022/6/14
 */
@Data
@Schema(description = "功能设计json模型")
public class VisualDevJsonModel {
    @Schema(description = "主键")
    private String id;
    @Schema(description = "名称")
    private String fullName;
    @Schema(description = "类型(1-应用开发,2-移动开发,3-流程表单,4-Web表单,5-App表单)")
    private Integer type;
    @Schema(description = "关联的表对象")
    private List<TableModel> visualTables;
    @Schema(description = "表单配置对象")
    private FormDataModel formData;
    @Schema(description = "字段配置对象")
    private ColumnDataModel columnData;
    @Schema(description = "app字段配置对象")
    private ColumnDataModel appColumnData;
    @Schema(description = "关联数据连接id")
    private String dbLinkId;
    @Schema(description = "页面类型（1、纯表单，2、表单加列表，3、表单列表工作流、4、数据视图）")
    private Integer webType;
    @Schema(description = "表单字段列表")
    private List<FieLdsModel> formListModels;
    @Schema(description = "树形子列表查询")
    private boolean isChildSearch = false;//树形子列表查询
    @Schema(description = "树形子列表查询值")
    private String childValue;//树形子列表查询值
    @Schema(description = "流程引擎id")
    private String flowId;

    @Schema(description = "主表主键")
    private String pkeyId;//切库回传

    @Schema(description = "数据过滤")
    private SuperJsonModel ruleQuery = new SuperJsonModel();
    @Schema(description = "高级搜索")
    private SuperJsonModel superQuery = new SuperJsonModel();
    @Schema(description = "列表搜索")
    private SuperJsonModel query = new SuperJsonModel();
    @Schema(description = "页签查询")
    private SuperJsonModel extraQuery = new SuperJsonModel();

    @Schema(description = "流程状态查询")
    private Integer jnpfFlowState = null;

    @Schema(description = "列表搜索")
    private SuperJsonModel keyQuery = new SuperJsonModel();

    @Schema(description = "数据权限")
    private List<SuperJsonModel> authorize = new ArrayList<>();
    private List<String> dataIdList = new ArrayList<>();

    @Schema(description = "是否流程菜单")
    private boolean enableFlow;
    @Schema(description = "流程版本id列表")
    private List<String> flowVersionIds = new ArrayList<>();

    @Schema(description = "是否外链:true -是")
    private Boolean isLinkList = false;

    @Schema(description = "需要判断权限")
    private Boolean needPermission = false;
    @Schema(description = "权限列表")
    private List<String> permissionList;
}
