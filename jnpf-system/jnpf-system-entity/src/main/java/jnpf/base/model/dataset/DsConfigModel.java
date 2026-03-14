package jnpf.base.model.dataset;

import jnpf.database.model.query.SuperQueryJsonModel;
import lombok.Data;

import java.util.List;

/**
 * 数据集配置式属性
 *
 * @author JNPF开发平台组
 * @version v5.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/9/9 17:23:57
 */
@Data
public class DsConfigModel {
    /**
     * 父级表名
     */
    private String parentTable;
    /**
     * 表名
     */
    private String table;
    /**
     * 表备注
     */
    private String tableName;
    /**
     * 表别名
     */
    private String tableAlias;
    /**
     * 字段列表
     */
    private List<DsConfigFields> fieldList;

    /**
     * 条件筛选
     */
    private String matchLogic;

    /**
     * 条件组
     */
    private List<SuperQueryJsonModel> ruleList;

    /**
     * 关联关系
     */
    private DsRelationConfig relationConfig;

    /**
     * 上级表名
     */
    private List<DsConfigModel> children;
}
