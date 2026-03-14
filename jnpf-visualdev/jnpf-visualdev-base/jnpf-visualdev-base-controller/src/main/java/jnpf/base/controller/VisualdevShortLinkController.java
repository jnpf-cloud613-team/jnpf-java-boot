package jnpf.base.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.alibaba.fastjson.JSONObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jnpf.base.ActionResult;
import jnpf.base.UserInfo;
import jnpf.base.entity.VisualdevEntity;
import jnpf.base.entity.VisualdevReleaseEntity;
import jnpf.base.entity.VisualdevShortLinkEntity;
import jnpf.base.model.ColumnDataModel;
import jnpf.base.model.VisualDevJsonModel;
import jnpf.base.model.VisualWebTypeEnum;
import jnpf.base.model.online.VisualdevModelDataInfoVO;
import jnpf.base.model.shortlink.*;
import jnpf.base.service.VisualdevReleaseService;
import jnpf.base.service.VisualdevService;
import jnpf.base.service.VisualdevShortLinkService;
import jnpf.base.util.VisualUtil;
import jnpf.base.vo.PageListVO;
import jnpf.base.vo.PaginationVO;
import jnpf.config.ConfigValueUtil;
import jnpf.constant.MsgCode;
import jnpf.consts.DeviceType;
import jnpf.database.util.TenantDataSourceUtil;
import jnpf.exception.DataException;
import jnpf.exception.LoginException;
import jnpf.exception.WorkFlowException;
import jnpf.model.OnlineDevData;
import jnpf.onlinedev.model.DataInfoVO;
import jnpf.onlinedev.model.PaginationModel;
import jnpf.onlinedev.model.VisualParamModel;
import jnpf.onlinedev.model.VisualdevModelDataCrForm;
import jnpf.onlinedev.service.VisualDevInfoService;
import jnpf.onlinedev.service.VisualDevListService;
import jnpf.onlinedev.service.VisualdevModelDataService;
import jnpf.onlinedev.util.OnlinePublicUtils;
import jnpf.onlinedev.util.OnlineSwapDataUtils;
import jnpf.util.*;
import jnpf.util.context.RequestContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 在线开发表单外链Controller
 *
 * @author JNPF开发平台组
 * @version V3.4.2
 * @copyright 引迈信息技术有限公司
 * @date 2022/12/30 11:33:17
 */
@Tag(name = "表单外链", description = "BaseShortLink")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/visualdev/ShortLink")
public class VisualdevShortLinkController extends SuperController<VisualdevShortLinkService, VisualdevShortLinkEntity> {

    private final VisualdevShortLinkService visualdevShortLinkService;
    private final ConfigValueUtil configValueUtil;
    private final VisualdevService visualdevService;
    private final VisualdevReleaseService visualdevReleaseService;
    private final VisualdevModelDataService visualdevModelDataService;
    private final OnlineSwapDataUtils onlineSwapDataUtils;
    private final VisualDevListService visualDevListService;
    private final VisualDevInfoService visualDevInfoService;

    @Operation(summary = "获取外链信息")
    @Parameter(name = "id", description = "主键")
    @GetMapping("/{id}")
    @SaCheckPermission("onlineDev.formDesign")
    public ActionResult<Object> getInfo(@PathVariable("id") String id) {
        VisualdevShortLinkEntity info = visualdevShortLinkService.getById(id);
        VisualdevShortLinkVo vo;
        if (info != null) {
            vo = JsonUtil.getJsonToBean(info, VisualdevShortLinkVo.class);
            vo.setAlreadySave(true);
        } else {
            vo = new VisualdevShortLinkVo();
            vo.setId(id);
        }
        vo.setFormLink(geturl(id, "form"));
        vo.setColumnLink(geturl(id, "list"));
        return ActionResult.success(vo);
    }

    /**
     * 获取url
     *
     * @param
     * @return
     * @copyright 引迈信息技术有限公司
     * @date 2023/3/9
     */
    private String geturl(String id, String type) {
        String url = configValueUtil.getApiDomain() + "/api/visualdev/ShortLink/trigger/" + id + "?encryption=";
        UserInfo userInfo = UserProvider.getUser();
        JSONObject obj = new JSONObject();
        obj.put("type", type);
        if (configValueUtil.isMultiTenancy()) {
            obj.put("tenantId", userInfo.getTenantId());
        }
        //参数加密
        String encryption = DesUtil.aesOrDecode(obj.toJSONString(), true, true);
        url += encryption;
        return url;
    }


    @Operation(summary = "修改外链信息")
    @PutMapping("")
    @SaCheckPermission("onlineDev.formDesign")
    public ActionResult<Object> saveOrupdate(@RequestBody VisualdevShortLinkForm data) {
        VisualdevShortLinkEntity entity = JsonUtil.getJsonToBean(data, VisualdevShortLinkEntity.class);
        if (entity.getFormLink().contains(configValueUtil.getApiDomain())) {
            entity.setFormLink(entity.getFormLink().replace(configValueUtil.getApiDomain(), ""));
        }
        if (entity.getColumnLink().contains(configValueUtil.getApiDomain())) {
            entity.setColumnLink(entity.getColumnLink().replace(configValueUtil.getApiDomain(), ""));
        }
        VisualdevShortLinkEntity info = visualdevShortLinkService.getById(data.getId());
        UserInfo userInfo = UserProvider.getUser();
        if (info != null) {
            entity.setLastModifyTime(new Date());
            entity.setLastModifyUserId(userInfo.getUserId());
        } else {
            entity.setCreatorTime(new Date());
            entity.setCreatorUserId(userInfo.getUserId());
        }

        String pcLink = "/formShortLink";
        String appLink = "/pages/formShortLink/index";
        entity.setRealPcLink(pcLink);
        entity.setRealAppLink(appLink);
        entity.setUserId(userInfo.getUserId());
        visualdevShortLinkService.saveOrUpdate(entity);
        return ActionResult.success(MsgCode.SU002.get());
    }

    /**
     * 参数解密切换数据源
     *
     * @param
     * @return
     * @copyright 引迈信息技术有限公司
     * @date 2023/3/9
     */
    private VisualdevShortLinkModel aesDecodeMatchDatabase(String encryption) throws LoginException {
        //参数解密
        String str = DesUtil.aesOrDecode(encryption, false, true);
        if (StringUtil.isEmpty(str)) {
            throw new LoginException(MsgCode.VS009.get());
        }
        VisualdevShortLinkModel model = JsonUtil.getJsonToBean(str, VisualdevShortLinkModel.class);
        if (configValueUtil.isMultiTenancy()) {
            if (StringUtil.isNotEmpty(model.getTenantId())) {
                //切换成租户库
                TenantDataSourceUtil.switchTenant(model.getTenantId());
            } else {
                throw new LoginException(MsgCode.LOG115.get());
            }
        }
        return model;
    }

    @NoDataSourceBind
    @Operation(summary = "外链请求入口")
    @Parameter(name = "id", description = "主键")
    @GetMapping("/trigger/{id}")
    public ActionResult<Object> getlink(@PathVariable("id") String id,
                                        @RequestParam(value = "encryption") String encryption,
                                        HttpServletResponse response) throws LoginException, IOException {
        VisualdevShortLinkModel model = aesDecodeMatchDatabase(encryption);
        String link = "";
        VisualdevShortLinkEntity entity = visualdevShortLinkService.getById(id);
        DeviceType deviceType = UserProvider.getDeviceForAgent();
        if (entity != null) {
            if (DeviceType.PC.equals(deviceType)) {
                link = configValueUtil.getFrontDomain() + entity.getRealPcLink();
            } else {
                link = configValueUtil.getAppDomain() + entity.getRealAppLink();
            }
        } else {
            return ActionResult.fail(MsgCode.VS010.get());
        }
        JSONObject obj = new JSONObject();
        obj.put("modelId", id);
        obj.put("type", model.getType());
        if (configValueUtil.isMultiTenancy()) {
            obj.put("tenantId", model.getTenantId());
        }
        //新链接参数加密
        String encryptionNew = DesUtil.aesOrDecode(obj.toJSONString(), true, true);
        link += "?encryption=" + encryptionNew;
        response.sendRedirect(link);
        return ActionResult.success(MsgCode.SU000.get());
    }

    @NoDataSourceBind
    @Operation(summary = "获取外链配置")
    @Parameter(name = "id", description = "主键")
    @GetMapping("/getConfig/{id}")
    public ActionResult<Object> getConfig(@PathVariable("id") String id, @RequestParam("encryption") String encryption) throws LoginException {
        aesDecodeMatchDatabase(encryption);

        VisualdevShortLinkEntity info = visualdevShortLinkService.getById(id);
        VisualdevShortLinkConfigVo vo = JsonUtil.getJsonToBean(info, VisualdevShortLinkConfigVo.class);
        vo.setFormLink(geturl(id, "form"));
        vo.setColumnLink(geturl(id, "list"));
        return ActionResult.success(vo);
    }

    @NoDataSourceBind
    @Operation(summary = "密码验证")
    @PostMapping("/checkPwd")
    public ActionResult<Object> checkPwd(@RequestBody VisualdevShortLinkPwd form) throws LoginException {
        //参数解密
        aesDecodeMatchDatabase(form.getEncryption());

        VisualdevShortLinkEntity info = visualdevShortLinkService.getById(form.getId());
        boolean flag = false;
        if ((OnlineDevData.STATE_ENABLE.equals(info.getFormPassUse()) && 0 == form.getType()
                && Md5Util.getStringMd5(info.getFormPassword()).equals(form.getPassword()))
                || (OnlineDevData.STATE_ENABLE.equals(info.getColumnPassUse()) && 1 == form.getType()
                && Md5Util.getStringMd5(info.getColumnPassword()).equals(form.getPassword()))) {
            flag = true;
        }
        if (flag) {
            return ActionResult.success();
        }
        return ActionResult.fail(MsgCode.VS011.get());
    }

    @NoDataSourceBind
    @Operation(summary = "获取列表表单配置JSON")
    @GetMapping("/{modelId}/Config")
    public ActionResult<Object> getData(@PathVariable("modelId") String modelId, @RequestParam(value = "type", required = false) String type,
                                        @RequestParam("encryption") String encryption) throws LoginException {
        aesDecodeMatchDatabase(encryption);
        VisualdevEntity entity;
        //线上版本
        if ("0".equals(type)) {
            entity = visualdevService.getInfo(modelId);
        } else {
            VisualdevReleaseEntity releaseEntity = visualdevReleaseService.getById(modelId);
            entity = JsonUtil.getJsonToBean(releaseEntity, VisualdevEntity.class);
        }
        if (entity == null) {
            return ActionResult.fail(MsgCode.VS012.get());
        }

        String s = VisualUtil.checkPublishVisualModel(entity, MsgCode.VS005.get());
        if (s != null) {
            return ActionResult.fail(s);
        }
        DataInfoVO vo = JsonUtil.getJsonToBean(entity, DataInfoVO.class);
        return ActionResult.success(vo);
    }

    @NoDataSourceBind
    @Operation(summary = "外链数据列表")
    @Parameter(name = "modelId", description = "模板id")
    @PostMapping("/{modelId}/ListLink")
    public ActionResult<PageListVO<Map<String, Object>>> listLink(@PathVariable("modelId") String modelId, @RequestParam("encryption") String encryption,
                                                                  @RequestBody PaginationModel paginationModel) throws WorkFlowException, LoginException {
        aesDecodeMatchDatabase(encryption);

        VisualdevReleaseEntity visualdevEntity = visualdevReleaseService.getById(modelId);
        VisualDevJsonModel visualJsonModel = OnlinePublicUtils.getVisualJsonModel(visualdevEntity);
        //判断请求客户端来源
        if (!RequestContext.isOrignPc()) {
            visualJsonModel.setColumnData(visualJsonModel.getAppColumnData());
        }
        List<Map<String, Object>> realList;
        if (VisualWebTypeEnum.DATA_VIEW.getType().equals(visualdevEntity.getWebType())) {//
            //数据视图的接口数据获取、
            ColumnDataModel columnDataModel = JsonUtil.getJsonToBean(visualdevEntity.getColumnData(), ColumnDataModel.class);
            realList = onlineSwapDataUtils.getInterfaceData(visualdevEntity, paginationModel, columnDataModel);
        } else {
            realList = visualDevListService.getDataListLink(visualJsonModel, paginationModel);
        }
        PaginationVO paginationVO = JsonUtil.getJsonToBean(paginationModel, PaginationVO.class);
        return ActionResult.page(realList, paginationVO);
    }

    @NoDataSourceBind
    @Operation(summary = "获取数据信息(带转换数据)")
    @Parameter(name = "modelId", description = "模板id")
    @Parameter(name = "id", description = "数据id")
    @GetMapping("/{modelId}/{id}/DataChange")
    public ActionResult<Object> infoWithDataChange(@PathVariable("modelId") String modelId, @PathVariable("id") String id,
                                                   @RequestParam("encryption") String encryption) throws DataException, LoginException {
        aesDecodeMatchDatabase(encryption);

        modelId = XSSEscape.escape(modelId);
        id = XSSEscape.escape(id);
        VisualdevEntity visualdevEntity = visualdevService.getReleaseInfo(modelId);
        VisualdevModelDataInfoVO vo = visualDevInfoService.getDetailsDataInfo(id, visualdevEntity);
        return ActionResult.success(vo);
    }

    //**********以下微服务和单体不同
    @NoDataSourceBind
    @Operation(summary = "添加数据")
    @Parameter(name = "modelId", description = "模板id")
    @Parameter(name = "visualdevModelDataCrForm", description = "功能数据创建表单")
    @PostMapping("/{modelId}")
    public ActionResult<Object> create(@PathVariable("modelId") String modelId, @RequestParam("encryption") String encryption,
                                       @RequestBody VisualdevModelDataCrForm visualdevModelDataCrForm) throws WorkFlowException, LoginException {
        VisualdevShortLinkModel visualdevShortLinkModel = aesDecodeMatchDatabase(encryption);
        VisualdevShortLinkEntity info = visualdevShortLinkService.getById(modelId);
        if (1 != info.getFormUse()) {
            return ActionResult.fail(MsgCode.VS013.get());
        }
        String tenantId = visualdevShortLinkModel.getTenantId();
        try {
            if (configValueUtil.isMultiTenancy()) {
                if (StringUtil.isNotEmpty(tenantId)) {
                    //切换成租户库
                    TenantDataSourceUtil.switchTenant(tenantId);
                } else {
                    return ActionResult.fail(MsgCode.LOG115.get());
                }
            }
            VisualdevEntity visualdevEntity = visualdevService.getReleaseInfo(modelId);
            Map<String, Object> map = JsonUtil.stringToMap(visualdevModelDataCrForm.getData());
            visualdevModelDataService.visualCreate(VisualParamModel.builder().visualdevEntity(visualdevEntity).data(map).isLink(true).build());
        } catch (Exception e) {
            throw new WorkFlowException(e.getMessage(), e);
        }
        return ActionResult.success(MsgCode.SU001.get());
    }
}
