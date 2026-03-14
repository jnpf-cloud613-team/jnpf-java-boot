package jnpf.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jnpf.base.ActionResult;
import jnpf.base.Page;
import jnpf.base.UserInfo;
import jnpf.base.model.base.SystemBaeModel;
import jnpf.base.model.module.ModuleModel;
import jnpf.base.vo.ListVO;
import jnpf.constant.JnpfConst;
import jnpf.model.AppMenuListVO;
import jnpf.model.UserMenuModel;
import jnpf.model.login.UserSystemVO;
import jnpf.permission.model.authorize.AuthorizeVO;
import jnpf.permission.service.AuthorizeService;
import jnpf.util.JsonUtil;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;
import jnpf.util.context.RequestContext;
import jnpf.util.treeutil.ListToTreeUtil;
import jnpf.util.treeutil.SumTree;
import jnpf.util.treeutil.newtreeutil.TreeDotUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

/**
 * app应用
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021-07-08
 */
@Tag(name = "app应用", description = "Menu")
@RestController
@RequestMapping("/api/app/Menu")
@RequiredArgsConstructor
public class AppMenuController {

    private final AuthorizeService authorizeService;



    /**
     * 获取菜单列表
     *
     * @param page 分页模型
     * @return
     */
    @Operation(summary = "获取菜单列表")
    @GetMapping
    public ActionResult<ListVO<AppMenuListVO>> list(Page page) {
        AuthorizeVO authorizeModel = authorizeService.getAuthorize(false, RequestContext.getAppCode(), 0);
        List<ModuleModel> buttonListAll = authorizeModel.getModuleList().stream().filter(t ->
                JnpfConst.APP.equals(t.getCategory())
                        && !Objects.equals(t.getNoShow(), 1)
                        && !JnpfConst.MODULE_CODE.contains(t.getEnCode())).collect(Collectors.toList());
        // 通过系统id捞取相应的菜单
        buttonListAll = buttonListAll.stream().filter(t -> UserProvider.getUser().getAppSystemId() != null && UserProvider.getUser().getAppSystemId().equals(t.getSystemId())).collect(Collectors.toList());
        List<ModuleModel> buttonList = buttonListAll;
        if (StringUtil.isNotEmpty(page.getKeyword())) {
            buttonList = buttonListAll.stream().filter(t -> t.getFullName().contains(page.getKeyword())).collect(Collectors.toList());
        }
        List<UserMenuModel> list = JsonUtil.getJsonToList(ListToTreeUtil.treeWhere(buttonList, buttonListAll), UserMenuModel.class);
        List<SumTree<UserMenuModel>> menuAll = TreeDotUtils.convertListToTreeDot(list, "-1");
        List<AppMenuListVO> data = JsonUtil.getJsonToList(menuAll, AppMenuListVO.class);
        ListVO<AppMenuListVO> listVO = new ListVO<>();
        listVO.setList(data);
        return ActionResult.success(listVO);
    }

    /**
     * 获取子集菜单
     *
     * @return
     */
    @Operation(summary = "获取子集菜单")
    @GetMapping("/getChildList/{id}")
    public ActionResult<List<AppMenuListVO>> getChildList(@PathVariable("id") String id) {
        AuthorizeVO authorizeModel = authorizeService.getAuthorize(false, RequestContext.getAppCode(), 0);
        List<ModuleModel> buttonListAll = authorizeModel.getModuleList().stream().filter(t -> JnpfConst.APP.equals(t.getCategory())).collect(Collectors.toList());
        // 通过系统id捞取相应的菜单
        buttonListAll = buttonListAll.stream().filter(t -> UserProvider.getUser().getAppSystemId() != null && UserProvider.getUser().getAppSystemId().equals(t.getSystemId())).collect(Collectors.toList());
        Set<ModuleModel> models = new HashSet<>();
        next(buttonListAll, id, models);
        List<UserMenuModel> list = JsonUtil.getJsonToList(models, UserMenuModel.class);
        List<SumTree<UserMenuModel>> menuAll = TreeDotUtils.convertListToTreeDot(list);
        List<AppMenuListVO> data = JsonUtil.getJsonToList(menuAll, AppMenuListVO.class);
        return ActionResult.success(data);
    }

    private void next(List<ModuleModel> buttonListAll, String parentId, Set<ModuleModel> list) {
        List<ModuleModel> menuList = buttonListAll.stream().filter(t -> t.getId().equals(parentId) || t.getParentId().equals(parentId)).collect(Collectors.toList());
        for (ModuleModel model : menuList) {
            if (!list.contains(model)) {
                list.add(model);
                next(buttonListAll, model.getId(), list);
            }
        }
    }

    @Operation(summary = "获取应用列表")
    @GetMapping("/sys")
    public ActionResult<List<UserSystemVO>> listSys() {
        AuthorizeVO authorizeModel = authorizeService.getAuthorizeByUser(false);
        List<SystemBaeModel> systemList = authorizeModel.getSystemList();
        UserInfo userInfo = UserProvider.getUser();
        List<UserSystemVO> jsonToList1 = new ArrayList<>();
        systemList.forEach(t -> {
            UserSystemVO systemVO = new UserSystemVO();
            systemVO.setId(t.getId());
            systemVO.setName(t.getFullName());
            systemVO.setIcon(t.getIcon());
            if (userInfo.getAppSystemId().equals(t.getId())) {
                systemVO.setCurrentSystem(true);
            }
            systemVO.setEnCode(t.getEnCode());
            jsonToList1.add(systemVO);
        });
        return ActionResult.success(jsonToList1);
    }

}
