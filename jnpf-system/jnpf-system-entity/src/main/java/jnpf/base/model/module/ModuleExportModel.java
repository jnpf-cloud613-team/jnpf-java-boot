package jnpf.base.model.module;

import jnpf.base.entity.*;
import lombok.Data;

import java.util.List;

/**
 * 系统菜单导出模型
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021-06-17
 */
@Data
public class ModuleExportModel {
    private ModuleEntity moduleEntity;
    private List<ModuleButtonEntity> buttonEntityList;
    private List<ModuleColumnEntity> columnEntityList;
    private List<ModuleFormEntity> formEntityList;
    private List<ModuleDataAuthorizeSchemeEntity> schemeEntityList;
    private List<ModuleDataAuthorizeEntity> authorizeEntityList;
}
