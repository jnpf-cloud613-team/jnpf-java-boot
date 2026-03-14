package jnpf.permission.model.authorize;

import jnpf.base.entity.SystemEntity;
import jnpf.base.model.base.SystemBaeModel;
import jnpf.base.model.button.ButtonModel;
import jnpf.base.model.column.ColumnModel;
import jnpf.base.model.form.ModuleFormModel;
import jnpf.base.model.module.ModuleModel;
import jnpf.base.model.resource.ResourceModel;
import jnpf.model.login.UserSystemVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/12 15:29
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthorizeVO implements Serializable {

    /**
     * 功能
     */
    private List<ModuleModel> moduleList = new ArrayList<>();

    /**
     * 按钮
     */
    private List<ButtonModel> buttonList = new ArrayList<>();

    /**
     * 视图
     */
    private List<ColumnModel> columnList = new ArrayList<>();

    /**
     * 资源
     */
    private List<ResourceModel> resourceList = new ArrayList<>();

    /**
     * 表单
     */
    private List<ModuleFormModel> formsList = new ArrayList<>();

    /**
     * 系统
     */
    private List<SystemBaeModel> systemList = new ArrayList<>();
    /**
     * 身份
     */
    private List<UserSystemVO> standingList = new ArrayList<>();
    /**
     * 授权的流程id
     */
    private List<String> flowIdList = new ArrayList<>();

    /**
     * 当前系统
     */
    private SystemEntity currentSystem;

    /**
     * 其他属性
     */
    private OtherModel otherModel;
}
