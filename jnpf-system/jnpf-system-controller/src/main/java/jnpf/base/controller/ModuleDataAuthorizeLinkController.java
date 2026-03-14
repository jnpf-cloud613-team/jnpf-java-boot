package jnpf.base.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import cn.hutool.core.util.ObjectUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jnpf.base.ActionResult;
import jnpf.base.Pagination;
import jnpf.base.entity.ModuleDataAuthorizeLinkEntity;
import jnpf.base.entity.ModuleEntity;
import jnpf.base.model.dbtable.vo.DbFieldVO;
import jnpf.base.model.module.PropertyJsonModel;
import jnpf.base.model.moduledataauthorize.DataAuthorizeLinkForm;
import jnpf.base.model.moduledataauthorize.DataAuthorizeTableNameVO;
import jnpf.base.service.DbTableService;
import jnpf.base.service.ModuleDataAuthorizeLinkDataService;
import jnpf.base.service.ModuleService;
import jnpf.base.vo.PageListVO;
import jnpf.base.vo.PaginationVO;
import jnpf.constant.MsgCode;
import jnpf.database.model.dbfield.DbFieldModel;
import jnpf.model.visualjson.TableModel;
import jnpf.util.*;
import jnpf.util.context.SpringContext;
import jnpf.workflow.service.TemplateApi;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据权限字段管理 数据连接
 *
 * @author JNPF开发平台组
 * @version V3.4.2
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2022/6/7
 */
@Tag(name = "数据权限字段管理数据连接", description = "ModuleDataAuthorizeLink")
@RestController
@RequestMapping("/api/system/ModuleDataAuthorizeLink")
@RequiredArgsConstructor
public class ModuleDataAuthorizeLinkController {

   
    private final  ModuleDataAuthorizeLinkDataService linkDataService;
  
    private final  ModuleService moduleService;
  
    private final  DbTableService dbTableService;
 
    private final TemplateApi templateApi;

    /**
     * 页面参数
     *
     * @param linkForm 页面参数
     * @return
     */
    @Operation(summary = "保存编辑数据连接")
    @Parameter(name = "linkForm", description = "页面参数", required = true)
    @SaCheckPermission(value = {"permission.resource", "appConfig.appResource"}, mode = SaMode.OR)
    @PostMapping("/saveLinkData")
    public ActionResult<Object>saveLinkData(@RequestBody @Valid DataAuthorizeLinkForm linkForm) {
        ModuleDataAuthorizeLinkEntity linkDataEntity = JsonUtil.getJsonToBean(linkForm, ModuleDataAuthorizeLinkEntity.class);
        if (StringUtil.isEmpty(linkDataEntity.getId())) {
            linkDataEntity.setId(RandomUtil.uuId());
            linkDataService.save(linkDataEntity);
            return ActionResult.success(MsgCode.SU002.get());
        } else {
            linkDataService.updateById(linkDataEntity);
            return ActionResult.success(MsgCode.SU004.get());
        }
    }

    /**
     * 获取表名
     *
     * @param menuId 菜单id
     * @param type   分类
     * @return
     */
    @Operation(summary = "获取表名")
    @Parameter(name = "menuId", description = "菜单id", required = true)
    @Parameter(name = "type", description = "分类", required = true)
    @SaCheckPermission(value = {"permission.resource", "appConfig.appResource"}, mode = SaMode.OR)
    @GetMapping("/getVisualTables/{menuId}/{type}")
    public ActionResult<DataAuthorizeTableNameVO> getVisualTables(@PathVariable("menuId") String menuId, @PathVariable("type") Integer type) {
        ModuleEntity info = moduleService.getInfo(menuId);
        DataAuthorizeTableNameVO vo = null;
        if (ObjectUtil.isNotNull(info)) {
            PropertyJsonModel model = JsonUtil.getJsonToBean(info.getPropertyJson(), PropertyJsonModel.class);
            if (model == null) {
                model = new PropertyJsonModel();
            }
            //功能
            if (info.getType() == 3 || info.getType() == 9 ||  info.getType() == 11) {
                String formId = model.getModuleId();
                if (info.getType() == 9) {
                    formId = templateApi.getFormByFlowId(model.getModuleId());
                }
                // 得到bean
                Object bean = SpringContext.getBean("visualdevServiceImpl");
                Object method = ReflectionUtil.invokeMethod(bean, "getInfo", new Class[]{String.class}, new Object[]{formId});
                Map<String, Object> map = JsonUtil.entityToMap(method);
                if (map != null) {
                    List<TableModel> tables = JsonUtil.getJsonToList(String.valueOf(map.get("tables")), TableModel.class);
                    List<String> collect = tables.stream().map(t -> t.getTable()).collect(Collectors.toList());
                    vo = DataAuthorizeTableNameVO.builder().linkTables(collect).linkId(String.valueOf(map.get("dbLinkId"))).build();
                }
            } else {
                ModuleDataAuthorizeLinkEntity linkDataEntity = linkDataService.getLinkDataEntityByMenuId(menuId, type);
                String linkTables = linkDataEntity.getLinkTables();
                List<String> tables = StringUtil.isNotEmpty(linkTables) ? Arrays.asList(linkTables.split(",")) : new ArrayList<>();
                vo = DataAuthorizeTableNameVO.builder().linkTables(tables).linkId(linkDataEntity.getLinkId()).build();
            }
        }
        return ActionResult.success(vo);
    }

    /**
     * 数据连接信息
     *
     * @param menudId 菜单id
     * @param type    分类
     * @return
     */
    @Operation(summary = "数据连接信息")
    @Parameter(name = "menudId", description = "菜单id", required = true)
    @Parameter(name = "type", description = "分类", required = true)
    @SaCheckPermission(value = {"permission.resource", "appConfig.appResource"}, mode = SaMode.OR)
    @GetMapping("/getInfo/{menudId}/{type}")
    public ActionResult<Object>getInfo(@PathVariable("menudId") String menudId, @PathVariable("type") Integer type) {
        ModuleDataAuthorizeLinkEntity linkDataEntity = linkDataService.getLinkDataEntityByMenuId(menudId, type);
        DataAuthorizeLinkForm linkForm = JsonUtil.getJsonToBean(linkDataEntity, DataAuthorizeLinkForm.class);
        return ActionResult.success(linkForm);
    }

    /**
     * 表名获取数据表字段
     *
     * @param linkId     连接id
     * @param tableName  表名
     * @param menuType   菜单类型
     * @param dataType   数据类型
     * @param pagination 分页模型
     * @return
     * @throws Exception
     */
    @Operation(summary = "表名获取数据表字段")
    @Parameter(name = "linkId", description = "连接id", required = true)
    @Parameter(name = "tableName", description = "表名", required = true)
    @Parameter(name = "menuType", description = "菜单类型", required = true)
    @Parameter(name = "dataType", description = "数据类型", required = true)
    @SaCheckPermission(value = {"permission.resource", "appConfig.appResource"}, mode = SaMode.OR)
    @GetMapping("/{linkId}/Tables/{tableName}/Fields/{menuType}/{dataType}")
    public ActionResult<PageListVO<DbFieldVO>>getTableInfoByTableName(@PathVariable("linkId") String linkId, @PathVariable("tableName") String tableName, @PathVariable("menuType") Integer menuType, @PathVariable("dataType") Integer dataType, Pagination pagination) throws SQLException {
        List<DbFieldModel> data = dbTableService.getFieldList(linkId, tableName);
        List<DbFieldVO> vos = JsonUtil.getJsonToList(data, DbFieldVO.class);
        if (StringUtil.isNotEmpty(pagination.getKeyword())) {
            vos = vos.stream().filter(vo -> {
                boolean ensure;
                String fieldName = vo.getFieldName();
                fieldName = Optional.ofNullable(fieldName).orElse("");
                ensure = fieldName.toLowerCase().contains(pagination.getKeyword().toLowerCase()) || vo.getField().toLowerCase().contains(pagination.getKeyword().toLowerCase());
                return ensure;
            }).collect(Collectors.toList());
        }
        List<DbFieldVO> listPage = PageUtil.getListPage((int) pagination.getCurrentPage(), (int) pagination.getPageSize(), vos);
        PaginationVO paginationVO = JsonUtil.getJsonToBean(pagination, PaginationVO.class);
        paginationVO.setTotal(vos.size());
        return ActionResult.page(listPage, paginationVO);
    }
}

