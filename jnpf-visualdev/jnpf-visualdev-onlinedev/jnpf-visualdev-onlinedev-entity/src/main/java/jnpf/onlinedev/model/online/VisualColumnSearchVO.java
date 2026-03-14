package jnpf.onlinedev.model.online;

import jnpf.model.visualjson.TemplateJsonModel;
import jnpf.model.visualjson.config.ConfigModel;
import jnpf.model.visualjson.props.PropsModel;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author JNPF开发平台组
 * @version V3.2.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021/8/9
 */
@Data
public class VisualColumnSearchVO {
    /**
     * 查询字段全key：如tableField113-datePickerField117
     */
    private String id;
    /**
     * 查询字段全名：如设计子表-子表年月日
     */
    private String fullName;
    /**
     * 查询条件类型 1.等于 2.模糊 3.范围
     */
    private String searchType;
    private String vModel;
    /**
     * 查询值
     */
    private Object value;
    /**
     * 是否多选
     */
    private Boolean multiple;

    private Boolean searchMultiple;

    private ConfigModel config;
    /**
     * 省市区
     */
    private Integer level;
    /**
     * 时间类型格式
     */
    private String format;
    private String type;

    /**
     * 数据库字段
     */
    private String field;
    private String table;

    private PropsModel props;
    private SlotModel slot;
    private String options;

    private List<TemplateJsonModel> templateJson = new ArrayList<>();
    private String interfaceId;

    private String selectType;
    private String ableDepIds;
    private String ableIds;
    private String ablePosIds;
    private String ableUserIds;
    private String ableRoleIds;
    private String ableGroupIds;

    /**
     * 列表字段是否关键词
     */
    private Boolean isKeyword = false;

    /**
     * 是否选中数据及子信息(只针对视图)
     */
    private Boolean isIncludeSubordinate = false;
}
