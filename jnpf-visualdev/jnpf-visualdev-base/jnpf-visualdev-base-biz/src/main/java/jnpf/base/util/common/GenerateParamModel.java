package jnpf.base.util.common;

import jnpf.base.UserInfo;
import jnpf.base.entity.VisualdevEntity;
import jnpf.base.model.DownloadCodeForm;
import jnpf.base.model.template.Template7Model;
import jnpf.config.ConfigValueUtil;
import jnpf.database.model.entity.DbLinkEntity;
import jnpf.database.util.DataSourceUtil;
import jnpf.model.visualjson.TableModel;
import jnpf.model.visualjson.analysis.FormAllModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author JNPF开发平台组
 * @version V3.5.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021/5/31
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GenerateParamModel {
    private DataSourceUtil dataSourceUtil;
    private String path;
    private String fileName;
    private String templatesPath;
    private DownloadCodeForm downloadCodeForm;
    private VisualdevEntity entity;
    private UserInfo userInfo;
    private ConfigValueUtil configValueUtil;
    private DbLinkEntity linkEntity;
    /**
     * 当前表名
     */
    private String table;
    /**
     * 主表主键：映射字段
     */
    private String pKeyName;
    /**
     * 主表主键:原字段
     */
    private String pKeyNameOriginal;
    /**
     * 当前表类名
     */
    private String className;
    /**
     * 代码生成基础信息
     */
    private Template7Model template7Model;

    /**
     * 乐观锁
     */
    private boolean concurrencyLock;
    /**
     * 是否自增
     */
    private boolean autoIncrement;
    /**
     * 是否主表
     */
    private boolean isMainTable;
    /**
     * 命名规范映射
     */
    private Map<String,AliasModel> tableAliseMap = new HashMap<>();

    /**
     * 表信息，只解析一遍传参通用
     */
    private List<TableModel> tableModelList;
    /**
     * 所有控件，只解析一遍传参通用
     */
    private List<FormAllModel> formAllModel;
}
