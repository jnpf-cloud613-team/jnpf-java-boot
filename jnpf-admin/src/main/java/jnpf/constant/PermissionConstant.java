package jnpf.constant;

/**
 * 分级管理常量
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021-11-01
 */
public class PermissionConstant {


    private PermissionConstant() {
    }

    /**
     * 创建方法
     */
    public static final String METHOD_CREATE = "create";

    /**
     * 编辑方法
     */
    public static final String METHOD_UPDATE = "update";

    /**
     * 删除方法
     */
    public static final String METHOD_DELETE = "delete";

    /**
     * 更新状态
     */
    public static final String METHOD_DISABLE = "disable";

    /**
     * 创建方法
     */
    public static final String METHOD_CREATE_DEPARTMENT = "createDepartment";

    /**
     * 编辑方法
     */
    public static final String METHOD_UPDATE_DEPARTMENT = "updateDepartment";

    /**
     * 删除方法
     */
    public static final String METHOD_DELETE_DEPARTMENT = "deleteDepartment";

    /**
     * 保存方法
     */
    public static final String METHOD_SAVE = "save";
    public static final String METHOD_SAVE_BATCH = "saveBatch";
    /**
     * 修改用户密码
     */
    public static final String METHOD_MODIFY_PW = "modifyPassword";

    /**
     * 拼接方法名
     */
    public static final String GET_METHOD_CREATE = "Add";
    public static final String GET_METHOD_UPDATE = "Edit";
    public static final String GET_METHOD_DELETE = "Delete";
    public static final String GET_METHOD_SELECT = "Select";
    public static final String GET_METHOD_THIS = "getThisLayer";
    public static final String GET_METHOD_SUB = "getSubLayer";

    /**
     * 解除绑定方法
     */
    public static final String METHOD_DELETE_SOCIALS = "deleteSocials";

}
