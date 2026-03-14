package jnpf.base.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.Page;
import jnpf.base.Pagination;
import jnpf.base.UserInfo;
import jnpf.base.entity.ModuleDataEntity;
import jnpf.base.entity.SystemEntity;
import jnpf.base.mapper.ModuleDataMapper;
import jnpf.base.mapper.SystemMapper;
import jnpf.base.model.module.ModuleModel;
import jnpf.base.service.ModuleDataService;
import jnpf.base.service.SuperServiceImpl;
import jnpf.constant.JnpfConst;
import jnpf.model.UserMenuModel;
import jnpf.model.login.AllMenuSelectVO;
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
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author JNPF开发平台组
 * @version V3.4.2
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2022/5/30
 */
@Service
@RequiredArgsConstructor
public class ModuleDataServiceImpl extends SuperServiceImpl<ModuleDataMapper, ModuleDataEntity> implements ModuleDataService {


    private final AuthorizeService authorizeApi;

    private final SystemMapper systemService;

    @Override
    public List<ModuleDataEntity> getList(String category, Page page) {
        List<ModuleModel> moduleModels = menuList(category).stream().filter(t -> category.equals(t.getCategory())).collect(Collectors.toList());
        if (StringUtil.isNotEmpty(page.getKeyword())) {
            moduleModels = moduleModels.stream().filter(t -> t.getFullName().contains(page.getKeyword())).collect(Collectors.toList());
        }
        List<String> moduleId = moduleModels.stream().map(ModuleModel::getId).collect(Collectors.toList());
        if (moduleId.isEmpty()) {
            return new ArrayList<>();
        }
        return this.baseMapper.getList(category, moduleId);
    }

    @Override
    public void create(String moduleId) {
        this.baseMapper.create(moduleId);
    }

    @Override
    public ModuleDataEntity getInfo(String objectId) {
        return this.baseMapper.getInfo(objectId);
    }

    @Override
    public boolean isExistByObjectId(String moduleId) {
        return this.baseMapper.isExistByObjectId(moduleId);
    }

    @Override
    public void delete(ModuleDataEntity entity) {
        this.removeById(entity.getId());
    }

    @Override
    public void delete(String moduleId) {
        this.baseMapper.deleteByModuleId(moduleId);
    }

    @Override
    public List<AllMenuSelectVO> getDataList(Page page) {
        List<String> idAll = getList(JnpfConst.APP, new Page()).stream().map(ModuleDataEntity::getModuleId).collect(Collectors.toList());
        List<ModuleModel> menuListAll = menuList();
        List<ModuleModel> menuList = menuListAll;
        if (StringUtil.isNotEmpty(page.getKeyword())) {
            menuList = menuList.stream().filter(t -> t.getFullName().contains(page.getKeyword())).collect(Collectors.toList());
        }
        List<UserMenuModel> list = JsonUtil.getJsonToList(ListToTreeUtil.treeWhere(menuList, menuListAll), UserMenuModel.class);
        for (UserMenuModel model : list) {
            model.setIsData(idAll.contains(model.getId()));
        }
        List<UserMenuModel> modelList = modelList(list);
        List<SumTree<UserMenuModel>> menuAll = TreeDotUtils.convertListToTreeDot(modelList, "-1");
        return JsonUtil.getJsonToList(menuAll, AllMenuSelectVO.class);
    }

    @Override
    public List<AllMenuSelectVO> getAppDataList(Pagination pagination) {
        List<String> idAll = getList(JnpfConst.APP, new Page()).stream().map(ModuleDataEntity::getModuleId).collect(Collectors.toList());
        List<ModuleModel> moduleModels = menuList();
        List<ModuleModel> appData = moduleModels.stream().filter(t -> idAll.contains(t.getId())).collect(Collectors.toList());
        String keyword = pagination.getKeyword();
        if (StringUtil.isNotEmpty(keyword)) {
            appData = appData.stream().filter(t -> t.getFullName().contains(keyword)).collect(Collectors.toList());
        }
        if (appData.isEmpty()) {
            return new ArrayList<>();
        }
        List<UserMenuModel> list = JsonUtil.getJsonToList(ListToTreeUtil.treeWhere(appData, moduleModels), UserMenuModel.class);
        List<UserMenuModel> modelList = modelList(list);
        List<SumTree<UserMenuModel>> menuAll = TreeDotUtils.convertListToTreeDot(modelList);
        return new LinkedList<>(JsonUtil.getJsonToList(menuAll, AllMenuSelectVO.class));
    }

    private List<UserMenuModel> modelList(List<UserMenuModel> list) {
        List<UserMenuModel> lists = list.stream().filter(t -> "-1".equals(t.getParentId())).collect(Collectors.toList());
        List<UserMenuModel> modelList = new ArrayList<>();
        for (UserMenuModel userMenuModel : lists) {
            List<String> idList = new ArrayList<>();
            String id = userMenuModel.getId();
            parentList(list, id, idList);
            List<UserMenuModel> collect = list.stream().filter(t -> idList.contains(t.getId())).collect(Collectors.toList());
            for (UserMenuModel menuModel : collect) {
                menuModel.setParentId(id);
            }
            modelList.addAll(collect);
            modelList.add(userMenuModel);
        }
        return modelList;
    }

    private void parentList(List<UserMenuModel> list, String parentId, List<String> idList) {
        List<UserMenuModel> menuList = list.stream().filter(t -> parentId.equals(t.getParentId())).collect(Collectors.toList());
        for (UserMenuModel menu : menuList) {
            List<UserMenuModel> collect = list.stream().filter(t -> t.getParentId().equals(menu.getId())).collect(Collectors.toList());
            idList.addAll(collect.stream().filter(t -> !Objects.equals(t.getType(), 1)).map(UserMenuModel::getId).collect(Collectors.toList()));
            if (!collect.isEmpty()) {
                parentList(list, menu.getId(), idList);
            }
            if (!Objects.equals(menu.getType(), 1)) {
                idList.add(menu.getId());
            }
        }
    }

    private List<ModuleModel> menuList() {
        String appSystemId = UserProvider.getUser().getAppSystemId();
        AuthorizeVO authorizeModel = authorizeApi.getAuthorizeByUser(false);
        return authorizeModel.getModuleList().stream().filter(t ->
                !JnpfConst.MODULE_CODE.contains(t.getEnCode()) && JnpfConst.APP.equals(t.getCategory()) && t.getSystemId().equals(appSystemId)
        ).collect(Collectors.toList());
    }

    private List<ModuleModel> menuList(String category) {
        String appCode = RequestContext.getAppCode();
        AuthorizeVO authorizeModel = authorizeApi.getAuthorizeByUser(true);
        List<ModuleModel> menuList = authorizeModel.getModuleList().stream().filter(t -> category.equals(t.getCategory())).collect(Collectors.toList());
        if (!JnpfConst.MAIN_SYSTEM_CODE.equals(appCode)) {
            SystemEntity systemEntity = systemService.getInfoByEnCode(appCode);
            String systemId = JnpfConst.WEB.equals(category) ? systemEntity.getId() : UserProvider.getUser().getAppSystemId();
            menuList = menuList.stream().filter(t -> t.getSystemId().equals(systemId)).collect(Collectors.toList());
        }
        return menuList;
    }

    @Override
    public List<ModuleModel> getFavoritesList(List<ModuleModel> moduleList) {
        String category = RequestContext.isOrignPc() ? JnpfConst.WEB : JnpfConst.APP;
        SystemEntity infoByEnCode = systemService.getInfoByEnCode(RequestContext.getAppCode());
        List<ModuleModel> moduleModels = moduleList.stream().filter(t -> category.equals(t.getCategory())).collect(Collectors.toList());
        Map<String, ModuleModel> map = moduleModels.stream().collect(Collectors.toMap(ModuleModel::getId, t -> t));
        List<String> moduleId = moduleModels.stream().map(ModuleModel::getId).collect(Collectors.toList());
        if (moduleId.isEmpty()) {
            return new ArrayList<>();
        }
        UserInfo userInfo = UserProvider.getUser();
        QueryWrapper<ModuleDataEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ModuleDataEntity::getCreatorUserId, userInfo.getUserId());
        queryWrapper.lambda().eq(ModuleDataEntity::getModuleType, category);
        queryWrapper.lambda().in(ModuleDataEntity::getModuleId, moduleId);
        if (!RequestContext.isOrignPc() && infoByEnCode != null) {
            queryWrapper.lambda().in(ModuleDataEntity::getSystemId, infoByEnCode.getId());
        }
        List<ModuleDataEntity> list = this.list(queryWrapper);
        List<ModuleModel> listRes = new ArrayList<>();
        for (ModuleDataEntity moduleDataEntity : list) {
            ModuleModel moduleModel = map.get(moduleDataEntity.getModuleId());
            listRes.add(moduleModel);
        }
        return listRes;
    }

}
