package jnpf.base.controller;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jnpf.base.ActionResult;
import jnpf.base.UserInfo;
import jnpf.base.entity.ModuleEntity;
import jnpf.base.entity.SystemEntity;
import jnpf.base.model.base.SystemBaeModel;
import jnpf.base.model.home.ChartModel;
import jnpf.base.model.home.HomeModel;
import jnpf.base.model.home.MenuModel;
import jnpf.base.model.home.TeamWorkModel;
import jnpf.base.model.module.MenuSelectByUseNumVo;
import jnpf.base.model.module.ModuleModel;
import jnpf.base.service.*;
import jnpf.constant.JnpfConst;
import jnpf.flowable.model.task.TaskTo;
import jnpf.flowable.model.template.TemplatePagination;
import jnpf.message.model.message.MessageInfoVO;
import jnpf.message.model.message.NoticeVO;
import jnpf.message.service.MessageService;
import jnpf.model.BaseSystemInfo;
import jnpf.model.login.UserSystemVO;
import jnpf.permission.model.authorize.AuthorizeVO;
import jnpf.permission.service.AuthorizeService;
import jnpf.permission.service.RoleService;
import jnpf.util.JsonUtil;
import jnpf.util.StringUtil;
import jnpf.util.TopSortUtil;
import jnpf.util.UserProvider;
import jnpf.util.context.RequestContext;
import jnpf.workflow.service.TaskApi;
import jnpf.workflow.service.TemplateApi;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 控制台首页数据
 *
 * @author JNPF开发平台组
 * @version v6.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2025/4/1 11:51:02
 */
@Tag(name = "控制台首页数据", description = "HomeDataController")
@RestController
@RequestMapping("/api/system/HomeData")
@RequiredArgsConstructor
public class HomeDataController {

    private static final String NO1_WELCOME = "welcome";
    private static final String NO2_BANNER = "banner";
    private static final String NO3_WORK_FLOW = "workFlow";
    private static final String NO4_TEAMWORK = "teamwork";
    private static final String NO5_NOTICE = "notice";
    private static final String NO6_MESSAGE = "message";
    private static final String NO7_LATELY_USE = "latelyUse";
    private static final String NO8_COMMON_USE = "commonUse";
    private static final String NO9_FAVORITES = "favorites";
    private static final String N10_SYSTEM = "system";
    private static final String NO11_PIE_CHART = "pieChart";
    private static final String NO12_COLUMN_CHART = "columnChart";
    private static final String NO13_SERVER = "server";
    private static final String NO14_HELP = "help";

    
    private final TaskApi taskApi;
    
    private final  TemplateApi templateApi;
    
    private final  AuthorizeService authorizeService;
    
    private final  MessageService messageService;
    
    private final  SystemService systemService;
    
    private final  RoleService roleService;
    
    private final  ModuleUseNumService moduleUseNumService;
    
    private final  ModuleDataService moduleDataService;
    
    private final  SysconfigService sysConfigApi;
    
    private final  ModuleService moduleService;
    
    private final  SystemTopService systemTopService;

    @Operation(summary = "首页数据")
    @GetMapping
    public ActionResult<Object>get() {
        UserInfo userInfo = UserProvider.getUser();
        BaseSystemInfo sysInfo = sysConfigApi.getSysInfo();
        List<HomeModel> list = new ArrayList<>();
        AuthorizeVO authorize = authorizeService.getAuthorizeByUser(false);
        List<SystemEntity> sysListAll = systemService.getList();
        List<ModuleEntity> allMenu = moduleService.getList();
        Map<String, SystemEntity> sysMap = sysListAll.stream().collect(Collectors.toMap(SystemEntity::getId, t -> t));
        List<ModuleModel> moduleList = authorize.getModuleList();
        List<String> authMenuList = moduleList.stream().filter(t -> JnpfConst.WEB.equals(t.getCategory()))
                .map(ModuleModel::getId).collect(Collectors.toList());
        Boolean iaAdmin = userInfo.getIsAdministrator();//超管
        Boolean isManageRole = userInfo.getIsManageRole();
        Boolean isDevRole = userInfo.getIsDevRole();
        boolean isFlowEnabled = Objects.equals(userInfo.getWorkflowEnabled(), 1);
        boolean isTeamEnabled = moduleList.stream().anyMatch(t -> JnpfConst.TEAMWORK_MODULE.contains(t.getEnCode()) && JnpfConst.WEB.equals(t.getCategory()));
        //1-欢迎语
        list.add(new HomeModel(NO1_WELCOME, 1, null));
        //2-banner
        if (isManageRole || isDevRole || Boolean.TRUE.equals(iaAdmin)) {
            list.add(new HomeModel(NO2_BANNER, 1, null));
        } else {
            list.add(new HomeModel(NO2_BANNER, 0, null));
        }
        //3-流程数据
        if (isFlowEnabled) {
            SystemEntity flowSys = systemService.getInfoByEnCode(JnpfConst.WORK_FLOW_CODE);
            List<ModuleModel> flowMenuList = moduleList.stream().filter(t -> t.getSystemId().equals(flowSys.getId())).collect(Collectors.toList());
            setFlow(flowMenuList, list);

        } else {
            list.add(new HomeModel(NO3_WORK_FLOW, 0, null));
        }
        //4-协作数据
        if (isTeamEnabled) {
            SystemEntity teamSys = systemService.getInfoByEnCode(JnpfConst.TEAMWORK_CODE);
            List<ModuleModel> teamworkList = moduleList.stream().filter(t -> t.getSystemId().equals(teamSys.getId())).collect(Collectors.toList());
            setTeamwork(teamworkList, list);
        } else {
            list.add(new HomeModel(NO4_TEAMWORK, 0, null));
        }

        //5-公告
        List<NoticeVO> noticeList = JsonUtil.getJsonToList(messageService.getNoticeList(new ArrayList<>()), NoticeVO.class);
        if (!noticeList.isEmpty()) {
            noticeList = Lists.partition(noticeList, 5).get(0);
        }
        list.add(new HomeModel(NO5_NOTICE, 1, noticeList));
        //6-消息
        List<MessageInfoVO> messageList = messageService.getUserMessageList();
        list.add(new HomeModel(NO6_MESSAGE, 1, messageList));

        //7-最近使用
        List<Object> menuNum = new ArrayList<>();
        List<MenuSelectByUseNumVo> menuLatelyList = moduleUseNumService.getMenuUseNum(1, authMenuList, allMenu);
        for (MenuSelectByUseNumVo item : menuLatelyList) {
            MenuModel menuModel = JsonUtil.getJsonToBean(item, MenuModel.class);
            setMenuModel(menuModel, sysMap, menuNum);
        }
        HomeModel latelyModel = HomeModel.builder().code(NO7_LATELY_USE).enable(1).appList(menuNum).flowEnabled(isFlowEnabled).build();
        if (isFlowEnabled) {
            List<Object> flowNum = new ArrayList<>(templateApi.getMenuUseNum(1, authorize.getFlowIdList()));
            latelyModel.setFlowList(flowNum);
        }
        list.add(latelyModel);
        //8-最近常用
        List<Object> menuUseNum = new ArrayList<>();
        List<MenuSelectByUseNumVo> menuComList = moduleUseNumService.getMenuUseNum(0, authMenuList, allMenu);
        for (MenuSelectByUseNumVo item : menuComList) {
            MenuModel menuModel = JsonUtil.getJsonToBean(item, MenuModel.class);
            setMenuModel(menuModel, sysMap, menuUseNum);
        }
        HomeModel commonUseModel = HomeModel.builder().code(NO8_COMMON_USE).enable(1).appList(menuUseNum).flowEnabled(isFlowEnabled).build();
        if (isFlowEnabled) {
            List<Object> flowUseNum = new ArrayList<>(templateApi.getMenuUseNum(0, authorize.getFlowIdList()));
            commonUseModel.setFlowList(flowUseNum);
        }
        list.add(commonUseModel);
        //9-我的收藏
        List<Object> favoritesMenuNum = new ArrayList<>();
        List<ModuleModel> favoritesList = moduleDataService.getFavoritesList(moduleList);
        for (ModuleModel item : favoritesList) {
            MenuModel menuModel = JsonUtil.getJsonToBean(item, MenuModel.class);
            setMenuModel(menuModel, sysMap, favoritesMenuNum);
        }
        HomeModel favoritesModel = HomeModel.builder().code(NO9_FAVORITES).enable(1).appList(favoritesMenuNum).flowEnabled(isFlowEnabled).build();
        if (isFlowEnabled || Boolean.TRUE.equals(iaAdmin)) {
            TemplatePagination pagination = new TemplatePagination();
            pagination.setPageSize(10000);
            pagination.setCategory("commonFlow");
            pagination.setIsLaunch(1);
            pagination.setAuthorize(authorize);
            List<Object> favoritesNum = new ArrayList<>(templateApi.getCommonList(pagination));
            favoritesModel.setFlowList(favoritesNum);
        }
        list.add(favoritesModel);

        //10-我的应用
        List<SystemBaeModel> systemList = getMySystem(authorize, sysInfo);
        list.add(new HomeModel(N10_SYSTEM, 1, systemList));

        if (isManageRole || iaAdmin) {
            //11-饼图
            Map<String, Integer> roleCount = roleService.roleUserCount();
            List<ChartModel> roleCountList = roleCount.entrySet().stream().map(entry -> new ChartModel(entry.getKey(), entry.getValue())).collect(Collectors.toList());
            list.add(new HomeModel(NO11_PIE_CHART, 1, roleCountList));
            //12-柱形图
            setColumnChart(list);
            //13-服务器信息
            list.add(new HomeModel(NO13_SERVER, 1, null));
        } else {
            list.add(new HomeModel(NO11_PIE_CHART, 0, null));
            list.add(new HomeModel(NO12_COLUMN_CHART, 0, null));
            list.add(new HomeModel(NO13_SERVER, 0, null));
        }

        String help = sysConfigApi.getValueByKey(NO14_HELP);
        if (StringUtils.isNotBlank(help)) {
            JSONObject jsonObject = JSON.parseObject(help);
            list.add(new HomeModel(NO14_HELP, 1, jsonObject.get("java")));
        } else {
            list.add(new HomeModel(NO14_HELP, 0, null));
        }
        return ActionResult.success(list);
    }

    @Operation(summary = "首页数据")
    @GetMapping("/app")
    public ActionResult<Object>getApp() {
        List<HomeModel> list = new ArrayList<>();
        AuthorizeVO authorize = authorizeService.getAuthorize(false, RequestContext.getAppCode(), 0, false);
        List<SystemEntity> sysListAll = systemService.getList();
        List<ModuleEntity> allMenu = moduleService.getList();
        Map<String, SystemEntity> sysMap = sysListAll.stream().collect(Collectors.toMap(SystemEntity::getId, t -> t));
        List<ModuleModel> moduleList = authorize.getModuleList();
        List<String> authMenuList = moduleList.stream().filter(t -> JnpfConst.APP.equals(t.getCategory()))
                .map(ModuleModel::getId).collect(Collectors.toList());
        boolean isFlowEnabled = moduleList.stream().anyMatch(t -> JnpfConst.MODULE_CODE.contains(t.getEnCode()) && JnpfConst.APP.equals(t.getCategory()));

        //7-最近使用
        List<Object> menuNum = new ArrayList<>();
        List<MenuSelectByUseNumVo> menuLatelyList = moduleUseNumService.getMenuUseNum(1, authMenuList, allMenu);
        for (MenuSelectByUseNumVo item : menuLatelyList) {
            MenuModel menuModel = JsonUtil.getJsonToBean(item, MenuModel.class);
            setMenuModel(menuModel, sysMap, menuNum);
        }
        HomeModel latelyModel = HomeModel.builder().code(NO7_LATELY_USE).enable(1).appList(menuNum).flowEnabled(isFlowEnabled).build();
        List<Object> flowNum = new ArrayList<>(templateApi.getMenuUseNum(1, authorize.getFlowIdList()));
        latelyModel.setFlowList(flowNum);
        list.add(latelyModel);
        //8-最近常用
        List<Object> menuUseNum = new ArrayList<>();
        List<MenuSelectByUseNumVo> menuComList = moduleUseNumService.getMenuUseNum(0, authMenuList, allMenu);
        for (MenuSelectByUseNumVo item : menuComList) {
            MenuModel menuModel = JsonUtil.getJsonToBean(item, MenuModel.class);
            setMenuModel(menuModel, sysMap, menuUseNum);
        }
        HomeModel commonUseModel = HomeModel.builder().code(NO8_COMMON_USE).enable(1).appList(menuUseNum).flowEnabled(isFlowEnabled).build();
        List<Object> flowUseNum = new ArrayList<>(templateApi.getMenuUseNum(0, authorize.getFlowIdList()));
        commonUseModel.setFlowList(flowUseNum);
        list.add(commonUseModel);
        //9-我的收藏
        List<Object> favoritesMenuNum = new ArrayList<>();
        List<ModuleModel> favoritesList = moduleDataService.getFavoritesList(moduleList);
        for (ModuleModel item : favoritesList) {
            MenuModel menuModel = JsonUtil.getJsonToBean(item, MenuModel.class);
            setMenuModel(menuModel, sysMap, favoritesMenuNum);
        }
        HomeModel favoritesModel = HomeModel.builder().code(NO9_FAVORITES).enable(1).appList(favoritesMenuNum).flowEnabled(isFlowEnabled).build();
        SystemEntity infoByEnCode = systemService.getInfoByEnCode(RequestContext.getAppCode());
        TemplatePagination pagination = new TemplatePagination();
        pagination.setPageSize(10000);
        pagination.setCategory("commonFlow");
        pagination.setIsLaunch(1);
        pagination.setSystemId(infoByEnCode.getId());
        List<Object> favoritesNum = new ArrayList<>(templateApi.getCommonList(pagination));
        favoritesModel.setFlowList(favoritesNum);
        list.add(favoritesModel);
        return ActionResult.success(list);
    }

    @Operation(summary = "我的应用列表")
    @GetMapping("/mySystem")
    public ActionResult<Object>mySystem() {
        BaseSystemInfo sysInfo = sysConfigApi.getSysInfo();
        AuthorizeVO authorize = authorizeService.getAuthorizeByUser(false);
        List<SystemBaeModel> systemList = getMySystem(authorize, sysInfo);
        return ActionResult.success(systemList);
    }

    /**
     * 获取我的应用
     *
     * @param authorize
     * @param sysInfo
     * @return
     */
    private List<SystemBaeModel> getMySystem(AuthorizeVO authorize, BaseSystemInfo sysInfo) {
        //10-我的应用
        List<SystemBaeModel> systemList = authorize.getSystemList().stream().filter(t -> !Objects.equals(t.getIsMain(), 1)).collect(Collectors.toList());
        //根据置顶排序
        String standId = null;
        if (Objects.equals(sysInfo.getStandingSwitch(), 1) && CollUtil.isNotEmpty(authorize.getStandingList())) {
            UserSystemVO userSystemVO = authorize.getStandingList().stream().filter(UserSystemVO::isCurrentStanding).findFirst().orElse(null);
            if (userSystemVO != null) standId = userSystemVO.getId();
        }
        List<String> topList = systemTopService.getObjectIdList(JnpfConst.SYS_HOME, standId);
        systemList = TopSortUtil.sortByTopIds(systemList, topList, SystemBaeModel::getId);
        for (SystemBaeModel item : systemList) {
            if (topList.contains(item.getId())) {
                item.setIsTop(true);
            }
        }
        return systemList;
    }

    /**
     * 菜单信息补充
     *
     * @param menuModel
     * @param sysMap
     * @param list
     */
    private static void setMenuModel(MenuModel menuModel, Map<String, SystemEntity> sysMap, List<Object> list) {
        JSONObject propertyJSON = JSON.parseObject(Optional.of(menuModel.getPropertyJson()).orElse("{}"));
        menuModel.setIconBackground(propertyJSON.getString("iconBackgroundColor"));
        List<String> appComModule = new ArrayList<>();
        appComModule.addAll(JnpfConst.APP_CONFIG_MODULE);
        appComModule.addAll(JnpfConst.ONLINE_DEV_MODULE);
        SystemEntity systemEntity = sysMap.get(menuModel.getSystemId());
        if (systemEntity != null) {
            menuModel.setSystemName(systemEntity.getFullName());
            if (StringUtil.isEmpty(menuModel.getSystemCode())) {
                menuModel.setSystemCode(systemEntity.getEnCode());
            }
        }
        if (appComModule.contains(menuModel.getEnCode())) {
            menuModel.setIsBackend(1);
        } else {
            menuModel.setIsBackend(0);
        }
        list.add(menuModel);
    }

    /**
     * 柱形图数据
     *
     * @param list
     */
    private void setColumnChart(List<HomeModel> list) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<String> dateList = IntStream.rangeClosed(0, 30)
                .mapToObj(i -> LocalDate.now().minusDays(30L - i))
                .map(formatter::format)
                .collect(Collectors.toList());
        List<String> category = dateList.stream().map(t -> t.substring(5)).collect(Collectors.toList());
        List<SystemEntity> allSysList = systemService.getList();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Map<String, List<SystemEntity>> createDate = allSysList.stream()
                .collect(Collectors.groupingBy(obj -> sdf.format(obj.getCreatorTime())));
        Map<String, List<SystemEntity>> updateDate = allSysList.stream()
                .collect(Collectors.groupingBy(obj -> Optional.ofNullable(obj.getLastModifyTime()).map(sdf::format).orElse("N/A")));
        List<Integer> createData = new ArrayList<>();
        List<Integer> updateData = new ArrayList<>();
        for (String t : dateList) {
            createData.add(CollUtil.isEmpty(createDate.get(t)) ? 0 : createDate.get(t).size());
            updateData.add(CollUtil.isEmpty(updateDate.get(t)) ? 0 : updateDate.get(t).size());
        }
        List<ChartModel> listChart = new ArrayList<>();
        ChartModel createModel = new ChartModel();
        createModel.setName("创建");
        createModel.setData(createData);
        createModel.setCategory(category);
        listChart.add(createModel);
        ChartModel updateModel = new ChartModel();
        updateModel.setName("修改");
        updateModel.setData(updateData);
        updateModel.setCategory(category);
        listChart.add(updateModel);
        list.add(new HomeModel(NO12_COLUMN_CHART, 1, listChart));
    }

    /**
     * 添加流程数据
     *
     * @param moduleList
     * @param list
     */
    private void setFlow(List<ModuleModel> moduleList, List<HomeModel> list) {
        List<TeamWorkModel> flowData = new ArrayList<>();
        List<ModuleModel> flowMenu = moduleList.stream().filter(t -> JnpfConst.MODULE_CODE.contains(t.getEnCode())
                && !JnpfConst.WORK_FLOW_CODE.equals(t.getEnCode())).collect(Collectors.toList());
        if (CollUtil.isNotEmpty(flowMenu)) {
            TaskTo flowModel = new TaskTo();
            flowModel.setModuleList(moduleList);
            TaskTo taskTo = taskApi.getFlowTodoCount(flowModel);
            for (ModuleModel menu : flowMenu) {
                TeamWorkModel model = JsonUtil.getJsonToBean(menu, TeamWorkModel.class);
                switch (menu.getEnCode()) {
                    case JnpfConst.WORK_FLOWLAUNCH:
                        model.setCount(taskTo.getFlowLaunch());
                        break;
                    case JnpfConst.WORK_FLOWSIGN:
                        model.setCount(taskTo.getFlowToSign());
                        break;
                    case JnpfConst.WORK_FLOWTODO:
                        model.setCount(taskTo.getFlowTodo());
                        break;
                    case JnpfConst.WORK_FLOWDOING:
                        model.setCount(taskTo.getFlowDoing());
                        break;
                    case JnpfConst.WORK_FLOWDONE:
                        model.setCount(taskTo.getFlowDone());
                        break;
                    case JnpfConst.WORK_FLOWCIRCULATE:
                        model.setCount(taskTo.getFlowCirculate());
                        break;
                    default:
                        break;
                }
                flowData.add(model);
            }
            list.add(new HomeModel(NO3_WORK_FLOW, 1, flowData));
        } else {
            list.add(new HomeModel(NO3_WORK_FLOW, 0, flowData));
        }
    }

    /**
     * 添加协作数据
     *
     * @param moduleList
     * @param list
     */
    private void setTeamwork(List<ModuleModel> moduleList, List<HomeModel> list) {
        List<TeamWorkModel> teamworkData = new ArrayList<>();
        List<ModuleModel> flowMenu = moduleList.stream().filter(t -> JnpfConst.TEAMWORK_MODULE.contains(t.getEnCode())
                && !JnpfConst.TEAMWORK_CODE.equals(t.getEnCode())).collect(Collectors.toList());
        if (CollUtil.isNotEmpty(flowMenu)) {
            teamworkData.addAll(JsonUtil.getJsonToList(flowMenu, TeamWorkModel.class));
            list.add(new HomeModel(NO4_TEAMWORK, 1, teamworkData));
        } else {
            list.add(new HomeModel(NO4_TEAMWORK, 0, teamworkData));
        }
    }
}
