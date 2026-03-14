package jnpf.permission.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import cn.hutool.core.collection.CollUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jnpf.annotation.OrganizeAdminIsTrator;
import jnpf.base.ActionResult;
import jnpf.base.UserInfo;
import jnpf.base.controller.SuperController;
import jnpf.base.entity.PrintDevEntity;
import jnpf.base.entity.SystemEntity;
import jnpf.base.model.portalmanage.PortalListVO;
import jnpf.base.model.portalmanage.PortalVO;
import jnpf.base.model.portalmanage.SavePortalAuthModel;
import jnpf.base.model.print.PaginationPrint;
import jnpf.base.service.PrintDevService;
import jnpf.base.service.SystemService;
import jnpf.constant.AuthorizeConst;
import jnpf.constant.MsgCode;
import jnpf.constant.PermissionConst;
import jnpf.flowable.entity.TemplateEntity;
import jnpf.flowable.model.template.TemplateTreeListVo;
import jnpf.permission.entity.*;
import jnpf.permission.model.authorize.*;
import jnpf.permission.model.columnspurview.ColumnsPurviewUpForm;
import jnpf.permission.service.*;
import jnpf.permission.util.AuthPermUtil;
import jnpf.util.JsonUtil;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;
import jnpf.util.context.RequestContext;
import jnpf.workflow.service.TemplateApi;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 操作权限
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月26日 上午9:18
 */
@Tag(name = "操作权限", description = "Authorize")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/permission/Authority")
public class AuthorizeController extends SuperController<AuthorizeService, AuthorizeEntity> {

    private final AuthorizeService authorizeService;
    private final ColumnsPurviewService columnsPurviewService;
    private final SystemService systemApi;
    private final TemplateApi templateApi;
    private final PrintDevService printDevService;
    private final AuthPermUtil authPermUtil;
    private final OrganizeService organizeService;
    private final PositionService positionService;
    private final RoleRelationService roleRelationService;

    @Operation(summary = "获取岗位/角色/用户权限树形结构")
    @Parameter(name = "objectId", description = "对象主键", required = true)
    @Parameter(name = "dataValuesQuery", description = "权限值", required = true)
    @SaCheckPermission(value = {"permission.auth", "permission.role"}, mode = SaMode.OR)
    @PostMapping("/Data/{objectId}/Values")
    public ActionResult<AuthorizeDataReturnVO> getValuesData(@PathVariable("objectId") String objectId, @RequestBody DataValuesQuery dataValuesQuery) {
        if (!StringUtil.isEmpty(dataValuesQuery.getType())) {
            AuthorizeParam authorizeParam = AuthorizeParam.builder()
                    .appCode(RequestContext.getAppCode())
                    .objectId(objectId)
                    .objectType(dataValuesQuery.getObjectType())
                    .itemType(dataValuesQuery.getType())
                    .moduleIds(dataValuesQuery.getModuleIds())
                    .build();
            AuthorizeDataReturnVO dataRes = authPermUtil.getAuthMenuList(authorizeParam);
            return ActionResult.success(dataRes);
        }
        return ActionResult.fail(MsgCode.PS012.get());
    }

    /**
     * 对象数据
     *
     * @return
     */
    @Operation(summary = "获取功能权限数据")
    @Parameter(name = "itemId", description = "对象主键", required = true)
    @Parameter(name = "objectType", description = "对象类型", required = true)
    @SaCheckPermission(value = {"permission.auth", "permission.role", "onlineDev.visualPortal"}, mode = SaMode.OR)
    @GetMapping("/Model/{itemId}/{objectType}")
    public ActionResult<AuthorizeItemObjIdsVO> getObjectAuth(@PathVariable("itemId") String itemId, @PathVariable("objectType") String objectType) {
        List<AuthorizeEntity> authorizeList = authorizeService.getListByObjectAndItem(itemId, objectType);
        List<String> ids = authorizeList.stream().map(u -> u.getObjectId()).collect(Collectors.toList());
        AuthorizeItemObjIdsVO vo = new AuthorizeItemObjIdsVO();
        vo.setIds(ids);
        return ActionResult.success(vo);
    }

    @Operation(summary = "门户管理授权")
    @Parameter(name = "itemId", description = "对象主键", required = true)
    @Parameter(name = "saveAuthForm", description = "保存权限模型", required = true)
    @PutMapping("/Model/{portalManageId}")
    @SaCheckPermission(value = {"permission.auth", "permission.role"}, mode = SaMode.OR)
    public ActionResult<Object> savePortalManage(@PathVariable("portalManageId") String portalManageId, @RequestBody SavePortalAuthModel model) {
        model.setId(portalManageId);
        model.setType(AuthorizeConst.AUTHORIZE_PORTAL_MANAGE);
        model.setIds(model.getObjectId());
        authorizeService.saveObjectAuth(model);
        return ActionResult.success(MsgCode.SU005.get());
    }

    /**
     * 保存
     *
     * @param objectId            对象主键
     * @param authorizeDataUpForm 修改权限模型
     * @return
     */
    @OrganizeAdminIsTrator
    @Operation(summary = "保存权限")
    @Parameter(name = "objectId", description = "对象主键", required = true)
    @Parameter(name = "authorizeDataUpForm", description = "修改权限模型", required = true)
    @SaCheckPermission(value = {"permission.auth", "permission.role"}, mode = SaMode.OR)
    @PutMapping("/Data/{objectId}")
    public ActionResult<Object> save(@PathVariable("objectId") String objectId, @RequestBody AuthorizeDataUpForm authorizeDataUpForm) {
        authorizeDataUpForm.setObjectId(objectId);
        String err = authorizeService.save(authorizeDataUpForm);
        if (StringUtil.isNotEmpty(err)) {
            return ActionResult.success(err);
        }
        //更新权限集合权限
        authorizeService.setPermissionGroup(objectId, authorizeDataUpForm.getObjectType());
        return ActionResult.success(MsgCode.SU005.get());
    }

    /**
     * 获取模块列表展示字段
     *
     * @param moduleId 菜单Id
     * @return
     */
    @Operation(summary = "获取模块列表展示字段")
    @Parameter(name = "moduleId", description = "菜单id", required = true)
    @GetMapping("/GetColumnsByModuleId/{moduleId}")
    public ActionResult<Object> getColumnsByModuleId(@PathVariable("moduleId") String moduleId) {
        ColumnsPurviewEntity entity = columnsPurviewService.getInfo(moduleId);
        List<Map<String, Object>> jsonToListMap = null;
        if (entity != null) {
            jsonToListMap = JsonUtil.getJsonToListMap(entity.getFieldList());
        }
        return ActionResult.success(jsonToListMap != null ? jsonToListMap : new ArrayList<>(16));
    }

    /**
     * 配置模块列表展示字段
     *
     * @param columnsPurviewUpForm 修改模型
     * @return
     */
    @Operation(summary = "配置模块列表展示字段")
    @Parameter(name = "columnsPurviewUpForm", description = "修改模型", required = true)
    @PutMapping("/SetColumnsByModuleId")
    public ActionResult<Object> setColumnsByModuleId(@RequestBody ColumnsPurviewUpForm columnsPurviewUpForm) {
        ColumnsPurviewEntity entity = JsonUtil.getJsonToBean(columnsPurviewUpForm, ColumnsPurviewEntity.class);
        columnsPurviewService.update(columnsPurviewUpForm.getModuleId(), entity);
        return ActionResult.success(MsgCode.SU005.get());
    }

    /**
     * 保存流程权限
     *
     * @return
     */
    @Operation(summary = "保存流程权限")
    @Parameter(name = "id", description = "对象主键", required = true)
    @PostMapping("/GroupFlow/{id}")
    public ActionResult<Object> groupFlow(@PathVariable("id") String id, @RequestBody SavePortalAuthModel model) {
        model.setId(id);
        model.setType(AuthorizeConst.FLOW);
        authorizeService.saveObjectAuth(model);
        return ActionResult.success(MsgCode.SU005.get());
    }

    /**
     * 保存流程权限
     *
     * @return
     */
    @Operation(summary = "保存流程权限")
    @Parameter(name = "id", description = "对象主键", required = true)
    @PostMapping("/Flow/{id}")
    public ActionResult<Object> saveFlowAuth(@PathVariable("id") String id, @RequestBody SavePortalAuthModel model) {
        model.setId(id);
        model.setType(AuthorizeConst.FLOW);
        authorizeService.saveItemAuth(model);
        //更新权限集合权限
        authorizeService.setPermissionGroup(id, model.getObjectType());
        return ActionResult.success(MsgCode.SU005.get());
    }

    /**
     * 获取流程权限
     *
     * @return
     */
    @Operation(summary = "获取流程权限")
    @Parameter(name = "id", description = "对象主键", required = true)
    @GetMapping("/Flow/{id}")
    public ActionResult<PortalVO> getFlowAuth(@PathVariable("id") String id, @RequestParam("objectType") String objectType) {
        UserInfo userInfo = UserProvider.getUser();
        boolean isAdmin = userInfo.getIsAdministrator();
        boolean isManageRole = userInfo.getIsManageRole();
        boolean isDevRole = userInfo.getIsDevRole();
        PortalVO vo = new PortalVO();
        //全部流程
        List<TemplateTreeListVo> treeList = templateApi.treeListWithPower();
        //上级权限传递（treeList移除无权限的）
        treeList = filterParent(id, objectType, treeList, AuthorizeConst.FLOW);
        // 当前权限组权限
        List<AuthorizeEntity> authorizePortalManage = authorizeService.getListByObjectId(id, AuthorizeConst.FLOW);
        List<String> ids = authorizePortalManage.stream().map(AuthorizeEntity::getItemId).collect(Collectors.toList());

        List<String> idAll = new ArrayList<>();

        //当前用户拥有的权限
        if (isAdmin || isManageRole || isDevRole) {
            idList(treeList, idAll, null, ids);
        } else {
            List<AuthorizeEntity> list = authorizeService.getListByUserId(false, userInfo.getUserId(), true);
            List<String> itemIds = list.stream().filter(t -> AuthorizeConst.FLOW.equals(t.getItemType())).map(AuthorizeEntity::getItemId).collect(Collectors.toList());
            itemIds.addAll(templateApi.getListByCreUser(userInfo.getUserId()).stream().map(TemplateEntity::getId).collect(Collectors.toList()));
            //没有权限的禁用
            idList(treeList, idAll, itemIds, ids);
        }

        vo.setAll(idAll);
        vo.setIds(ids);
        vo.setList(JsonUtil.getJsonToList(treeList, PortalListVO.class));
        return ActionResult.success(vo);
    }

    /**
     * 根据itemIds，禁用以外的选项
     *
     * @param list    树形结构数据
     * @param idAll   提取全部id
     * @param itemIds 非禁用id列表
     */
    private void idList(List<TemplateTreeListVo> list, List<String> idAll, List<String> itemIds, List<String> selectIds) {
        for (TemplateTreeListVo vo : list) {
            vo.setDisabled(false);
            if (vo.getChildren() != null) {
                idList(vo.getChildren(), idAll, itemIds, selectIds);
                //子集有被禁用且被选中的的，那么上级肯定被禁用，如果没有被选中，那么只有全部禁用的情况才禁用
                boolean isDisabled = false;
                int n = 0;
                for (TemplateTreeListVo child : vo.getChildren()) {
                    if (Boolean.TRUE.equals(child.getDisabled())) {
                        if (selectIds.contains(child.getId())) {
                            isDisabled = true;
                            break;
                        } else {
                            n++;
                        }
                    }
                }
                if (n == vo.getChildren().size()) {
                    isDisabled = true;
                }
                vo.setDisabled(isDisabled);
            } else {
                if (itemIds != null && !itemIds.contains(vo.getId())) {
                    vo.setDisabled(true);
                }
            }
            idAll.add(vo.getId());
        }
    }

    //根据List<String> 过滤树形数据
    private List<TemplateTreeListVo> filter(List<TemplateTreeListVo> list, List<String> filterList) {
        List<TemplateTreeListVo> listRes = new ArrayList<>(list);
        for (TemplateTreeListVo vo : list) {
            if (!filterList.contains(vo.getId())) {
                listRes.remove(vo);
            } else {
                if (CollUtil.isNotEmpty(vo.getChildren())) {
                    vo.setChildren(filter(vo.getChildren(), filterList));
                }
            }
        }
        return listRes;
    }

    //传递上级权限--过滤上级没有的权限
    private List<TemplateTreeListVo> filterParent(String id, String objectType, List<TemplateTreeListVo> treeList, String itemType) {
        boolean filter = false;
        List<String> filterList = new ArrayList<>();
        if (PermissionConst.ORGANIZE.equals(objectType)) {
            OrganizeEntity info = organizeService.getInfo(id);
            if (!"-1".equals(info.getParentId())) {
                filter = true;
                List<String> objectIds = new ArrayList<>();
                objectIds.add(info.getParentId());
                List<String> collect = roleRelationService.getListByObjectId(info.getParentId(), PermissionConst.ORGANIZE)
                        .stream().map(RoleRelationEntity::getRoleId).collect(Collectors.toList());
                objectIds.addAll(collect);
                filterList.addAll(authorizeService.getListByRoleIdsAndItemType(objectIds, itemType)
                        .stream().map(AuthorizeEntity::getItemId).collect(Collectors.toList()));
            }
        }
        if (PermissionConst.POSITION.equals(objectType)) {
            filter = true;
            PositionEntity info = positionService.getInfo(id);
            if (StringUtil.isEmpty(info.getParentId()) || "-1".equals(info.getParentId())) {
                //继承组织
                List<String> objectIds = new ArrayList<>();
                objectIds.add(info.getOrganizeId());
                List<String> collect = roleRelationService.getListByObjectId(info.getOrganizeId(), PermissionConst.ORGANIZE)
                        .stream().map(RoleRelationEntity::getRoleId).collect(Collectors.toList());
                objectIds.addAll(collect);
                filterList.addAll(authorizeService.getListByRoleIdsAndItemType(objectIds, itemType)
                        .stream().map(AuthorizeEntity::getItemId).collect(Collectors.toList()));
            } else {
                //继承上级岗位
                List<String> objectIds = new ArrayList<>();
                objectIds.add(info.getParentId());
                List<String> collect = roleRelationService.getListByObjectId(info.getParentId(), PermissionConst.POSITION)
                        .stream().map(RoleRelationEntity::getRoleId).collect(Collectors.toList());
                objectIds.addAll(collect);
                filterList.addAll(authorizeService.getListByRoleIdsAndItemType(objectIds, itemType)
                        .stream().map(AuthorizeEntity::getItemId).collect(Collectors.toList()));
            }
        }
        if (filter) {
            return filter(treeList, filterList);
        }
        return treeList;
    }


    /**
     * 保存打印权限
     *
     * @return
     */
    @Operation(summary = "保存打印权限")
    @Parameter(name = "id", description = "对象主键", required = true)
    @PostMapping("/Print/{id}")
    public ActionResult<Object> savePrintAuth(@PathVariable("id") String id, @RequestBody SavePortalAuthModel model) {
        model.setId(id);
        model.setType(AuthorizeConst.PRINT);
        authorizeService.saveItemAuth(model);
        //更新权限集合权限
        authorizeService.setPermissionGroup(id, model.getObjectType());
        return ActionResult.success(MsgCode.SU005.get());
    }

    /**
     * 获取打印权限
     *
     * @return
     */
    @Operation(summary = "获取打印权限")
    @Parameter(name = "id", description = "对象主键", required = true)
    @GetMapping("/Print/{id}")
    public ActionResult<PortalVO> getPrintAuth(@PathVariable("id") String id, @RequestParam("objectType") String objectType) {
        UserInfo userInfo = UserProvider.getUser();
        boolean isAdmin = userInfo.getIsAdministrator();
        boolean isManageRole = userInfo.getIsManageRole();
        boolean isDevRole = userInfo.getIsDevRole();
        PortalVO vo = new PortalVO();
        PaginationPrint paginationPrint = new PaginationPrint();
        paginationPrint.setDataType(1);
        paginationPrint.setVisibleType(2);
        List<PrintDevEntity> list = printDevService.getWorkSelector(paginationPrint);
        List<String> systemIds = list.stream().map(PrintDevEntity::getSystemId).collect(Collectors.toList());
        List<SystemEntity> systemList = systemApi.getListByIds(systemIds, null);
        List<TemplateTreeListVo> treeList = new ArrayList<>();
        for (SystemEntity dict : systemList) {
            TemplateTreeListVo tree = JsonUtil.getJsonToBean(dict, TemplateTreeListVo.class);
            List<PrintDevEntity> childList = list.stream()
                    .filter(e -> dict.getId().equals(e.getSystemId()))
                    .sorted(Comparator.comparing(PrintDevEntity::getSortCode, Comparator.nullsLast(Comparator.naturalOrder()))
                            .thenComparing(PrintDevEntity::getCreatorTime, Comparator.nullsLast(Comparator.reverseOrder()))).collect(Collectors.toList());
            if (!childList.isEmpty()) {
                List<TemplateTreeListVo> childListAll = new ArrayList<>();
                for (PrintDevEntity entity : childList) {
                    TemplateTreeListVo user = JsonUtil.getJsonToBean(entity, TemplateTreeListVo.class);
                    childListAll.add(user);
                }
                tree.setChildren(childListAll);
                treeList.add(tree);
            }
        }
        //上级权限传递（treeList移除无权限的）
        treeList = filterParent(id, objectType, treeList, AuthorizeConst.PRINT);

        // 当前权限组权限
        List<AuthorizeEntity> authorizePortalManage = authorizeService.getListByObjectId(id, AuthorizeConst.PRINT);
        List<String> ids = authorizePortalManage.stream().map(AuthorizeEntity::getItemId).collect(Collectors.toList());

        List<String> idAll = new ArrayList<>();
        //当前用户拥有的权限--没有权限禁用
        if (isAdmin || isManageRole || isDevRole) {
            idList(treeList, idAll, null, ids);
        } else {
            List<AuthorizeEntity> lista = authorizeService.getListByUserId(false, userInfo.getUserId(), true);
            List<String> itemIds = lista.stream().filter(t -> AuthorizeConst.PRINT.equals(t.getItemType())).map(AuthorizeEntity::getItemId).collect(Collectors.toList());
            itemIds.addAll(printDevService.getListByCreUser(userInfo.getUserId()).stream().map(PrintDevEntity::getId).collect(Collectors.toList()));
            //没有权限的禁用
            idList(treeList, idAll, itemIds, ids);
        }

        vo.setAll(idAll);
        vo.setIds(ids);
        vo.setList(JsonUtil.getJsonToList(treeList, PortalListVO.class));
        return ActionResult.success(vo);
    }

}
