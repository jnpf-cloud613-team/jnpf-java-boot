package jnpf.base.util.app;

import jnpf.base.UserInfo;
import jnpf.base.entity.VisualdevEntity;
import jnpf.base.model.DownloadCodeForm;
import jnpf.base.model.template.Template7Model;
import jnpf.base.util.common.AliasModel;
import jnpf.database.model.entity.DbLinkEntity;
import jnpf.database.util.DataSourceUtil;
import jnpf.model.visualjson.FormDataModel;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021/10/25
 */
@Data
public class AppGenModel {
    /**
     * 文件夹名字
     */
    private String fileName;
    /**
     * 实体对象
     */
    private VisualdevEntity entity;
    /**
     * 下载对象
     */
    private DownloadCodeForm downloadCodeForm;
    /**
     * 表单对象
     */
    private FormDataModel model;
    /**
     * 模板文件
     */
    private String templatePath;
    /**
     * 主键
     */
    private String pKeyName;
    /**
     * 本地数据源
     */
    private DataSourceUtil dataSourceUtil;
    /**
     * 数据连接
     */
    private DbLinkEntity linkEntity;
    /**
     * 个人信息
     */
    private UserInfo userInfo;
    /**
     * 生成文件名字
     */
    private String className;
    /**
     * 数据库表
     */
    private String table;
    /**
     * 生成路径
     */
    private String serviceDirectory;
    /**
     * 模板路径
     */
    private String templateCodePath;

    private Boolean groupTable;

    private String type;

    /**
     * 代码生成基础信息
     */
    private Template7Model template7Model;

    /**
     * 命名规范映射
     */
    private Map<String, AliasModel> tableAliseMap = new HashMap<>();
}
