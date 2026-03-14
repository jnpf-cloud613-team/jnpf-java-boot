package jnpf.permission.model.authorize;

import jnpf.constant.DataInterfaceVarConst;
import jnpf.util.visiual.JnpfKeyConsts;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据权限过滤条件字段
 *
 * @author JNPF开发平台组
 * @version V3.2
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021/10/9
 */
public enum AuthorizeConditionEnum {
    /**
     * 表单主键
     */
    FORMID(DataInterfaceVarConst.FORM_ID, "表单主键"),

    /**
     * 用户
     */
    USER(DataInterfaceVarConst.USER, "当前用户"),
    USERANDUNDER(DataInterfaceVarConst.USERANDSUB, "当前用户及下属"),
    USERANDPROGENY(DataInterfaceVarConst.USERANDPROGENY, "当前用户及子孙下属"),
    /**
     * 组织
     */
    ORGANIZE(DataInterfaceVarConst.ORG, "当前组织"),
    ORGANDSUB(DataInterfaceVarConst.ORGANDSUB, "当前组织及子组织"),
    ORGANIZEANDPROGENY(DataInterfaceVarConst.ORGANIZEANDPROGENY, "当前组织及子组织"),
    /**
     * 岗位
     */
    POSITIONID(DataInterfaceVarConst.POSITIONID, "当前岗位"),
    POSITIONANDSUB(DataInterfaceVarConst.POSITIONANDSUB, "当前岗位及子岗位"),
    POSITIONANDPROGENY(DataInterfaceVarConst.POSITIONANDPROGENY, "当前岗位及子岗位"),

    /**
     * 当前时间
     */
    CURRENTTIME(DataInterfaceVarConst.CURRENTTIME, "当前时间"),
    /**
     * 任意文本
     */
    TEXT(JnpfKeyConsts.COM_INPUT, "任意文本"),
    DATATIME(JnpfKeyConsts.DATE, "日期选择"),
    INPUTNUMBER(JnpfKeyConsts.NUM_INPUT, "数字输入"),
    COMSELECT(JnpfKeyConsts.COMSELECT, "组织选择"),
    DEPSELECT(JnpfKeyConsts.DEPSELECT, "部门选择"),
    POSSELECT(JnpfKeyConsts.POSSELECT, "岗位选择"),
    ROLESELECT(JnpfKeyConsts.ROLESELECT, "角色选择"),
    GROUPSELECT(JnpfKeyConsts.GROUPSELECT, "分组选择"),
    USERSELECT(JnpfKeyConsts.USERSELECT, "用户选择"),


    ;
    private String condition;
    private String message;

    AuthorizeConditionEnum(String condition, String message) {
        this.condition = condition;
        this.message = message;
    }

    public String getCondition() {
        return condition;
    }

    public String getMessage() {
        return message;
    }

    public static AuthorizeConditionEnum getByMessage(String condition) {
        for (AuthorizeConditionEnum status : AuthorizeConditionEnum.values()) {
            if (status.getCondition().equals(condition)) {
                return status;
            }
        }
        return null;
    }

    public static List<String> getResListType() {
        List<String> resList = new ArrayList<>();
        resList.add(USERANDUNDER.getCondition());
        resList.add(USERANDPROGENY.getCondition());
        resList.add(ORGANIZE.getCondition());
        resList.add(ORGANDSUB.getCondition());
        resList.add(ORGANIZEANDPROGENY.getCondition());
        resList.add(POSITIONID.getCondition());
        resList.add(POSITIONANDSUB.getCondition());
        resList.add(POSITIONANDPROGENY.getCondition());
        return resList;
    }
}
