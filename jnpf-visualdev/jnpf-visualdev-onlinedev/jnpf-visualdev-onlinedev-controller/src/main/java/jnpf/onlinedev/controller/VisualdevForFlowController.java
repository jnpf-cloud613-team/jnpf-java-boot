package jnpf.onlinedev.controller;

import jnpf.base.ActionResult;
import jnpf.base.UserInfo;
import jnpf.base.entity.VisualAliasEntity;
import jnpf.base.entity.VisualdevEntity;
import jnpf.base.entity.VisualdevReleaseEntity;
import jnpf.base.mapper.VisualdevReleaseMapper;
import jnpf.base.model.VisualDevJsonModel;
import jnpf.base.model.export.VisualExportVo;
import jnpf.base.model.flow.DataModel;
import jnpf.base.model.flow.FlowFormDataModel;
import jnpf.base.model.flow.FlowStateModel;
import jnpf.base.model.online.VisualdevModelDataInfoVO;
import jnpf.base.service.*;
import jnpf.base.util.FlowFormDataUtil;
import jnpf.base.util.FlowFormHttpReqUtils;
import jnpf.database.model.entity.DbLinkEntity;
import jnpf.exception.WorkFlowException;
import jnpf.onlinedev.model.OnlineInfoModel;
import jnpf.onlinedev.model.PaginationModel;
import jnpf.onlinedev.model.VisualParamModel;
import jnpf.onlinedev.service.VisualDevInfoService;
import jnpf.onlinedev.service.VisualDevListService;
import jnpf.onlinedev.service.VisualdevModelDataService;
import jnpf.onlinedev.util.FlowFormCustomUtils;
import jnpf.onlinedev.util.OnlinePublicUtils;
import jnpf.util.*;
import jnpf.visual.service.VisualdevApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class VisualdevForFlowController implements VisualdevApi {

    private final VisualdevReleaseService visualdevReleaseService;
    private final VisualdevReleaseMapper visualdevReleaseMapper;
    private final VisualDevListService visualDevListService;
    private final VisualDevInfoService visualDevInfoService;
    private final FlowFormHttpReqUtils flowFormHttpReqUtils;
    private final FlowFormCustomUtils flowFormCustomUtils;
    private final FlowFormDataUtil flowDataUtil;
    private final DbLinkService dblinkService;
    private final FlowFormRelationService flowFormRelationService;
    private final VisualdevModelDataService visualdevModelDataService;
    private final VisualdevService visualdevService;
    private final VisualAliasService aliasService;

    @Override
    public ActionResult<Object> saveOrUpdate(FlowFormDataModel flowFormDataModel) {
        DataModel dataModel = null;
        try {
            String id = flowFormDataModel.getId();
            String formId = flowFormDataModel.getFormId();
            String flowId = flowFormDataModel.getFlowId();
            Map<String, Object> map = flowFormDataModel.getMap();
            List<Map<String, Object>> formOperates = flowFormDataModel.getFormOperates();
            VisualdevReleaseEntity entity = visualdevReleaseService.getById(formId);
            VisualdevEntity visualdevEntity = JsonUtil.getJsonToBean(entity, VisualdevEntity.class);

            map.put(FlowFormConstant.FLOWID, flowId);
            if (map.get(TableFeildsEnum.VERSION.getField().toUpperCase()) != null) {//针对Oracle数据库大小写敏感，出现大写字段补充修复
                map.put(TableFeildsEnum.VERSION.getField(), map.get(TableFeildsEnum.VERSION.getField().toUpperCase()));
            }

            //系统表单
            if (entity.getType() == 2) {
                map.put("formOperates", formOperates);
                flowFormHttpReqUtils.saveOrUpdate(visualdevEntity, id, UserProvider.getToken(), map);
            } else {
                dataModel = flowFormCustomUtils.saveOrUpdate(visualdevEntity, flowFormDataModel);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ActionResult.fail(e.getMessage());
        }
        return ActionResult.success(dataModel);
    }

    @Override
    public boolean delete(String formId, String id) {
        try {
            VisualdevReleaseEntity entity = visualdevReleaseService.getById(formId);
            VisualdevEntity visualdevEntity = JsonUtil.getJsonToBean(entity, VisualdevEntity.class);
            //系统表单
            if (entity.getType() == 2) {
                flowFormHttpReqUtils.delete(visualdevEntity, id, UserProvider.getToken());
            } else {
                VisualDevJsonModel visualJsonModel = OnlinePublicUtils.getVisualJsonModel(visualdevEntity);
                DbLinkEntity linkEntity = StringUtil.isNotEmpty(visualdevEntity.getDbLinkId()) ? dblinkService.getInfo(visualdevEntity.getDbLinkId()) : null;
                flowDataUtil.deleteTable(id, visualJsonModel, linkEntity);
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public ActionResult<Object> info(String formId, String id) {
        ActionResult<Object> result = new ActionResult<>();
        Map<String, Object> allDataMap = new HashMap<>();
        VisualdevReleaseEntity entity = visualdevReleaseService.getById(formId);
        VisualdevEntity visualdevEntity = JsonUtil.getJsonToBean(entity, VisualdevEntity.class);
        result.setCode(visualdevEntity == null ? 400 : 200);
        result.setMsg(visualdevEntity == null ? "表单信息不存在" : "");
        if (visualdevEntity != null) {
            //判断是否为系统表单
            boolean b = visualdevEntity.getType() == 2;
            if (b) {
                allDataMap.putAll(flowFormHttpReqUtils.info(visualdevEntity, id, UserProvider.getToken()));
            } else {
                allDataMap.putAll(flowFormCustomUtils.info(visualdevEntity, id));
            }
        }
        result.setData(allDataMap);
        return result;
    }

    @Override
    public VisualdevEntity getFormConfig(String formId) {
        VisualdevReleaseEntity entity = visualdevReleaseService.getById(formId);
        return JsonUtil.getJsonToBean(entity, VisualdevEntity.class);
    }

    @Override
    public List<VisualdevEntity> getFormConfigList(List<String> formIds) {
        List<VisualdevReleaseEntity> list = visualdevReleaseService.selectByIds(formIds,
                VisualdevReleaseEntity::getId,
                VisualdevReleaseEntity::getEnCode,
                VisualdevReleaseEntity::getFullName,
                VisualdevReleaseEntity::getWebType,
                VisualdevReleaseEntity::getType);
        return CollectionUtils.isNotEmpty(list) ? JsonUtil.getJsonToList(list, VisualdevEntity.class) : new ArrayList<>();
    }

    @Override
    public void saveFlowIdByFormIds(String flowId, List<String> formIds) {
        flowFormRelationService.saveFlowIdByFormIds(flowId, formIds);
    }

    @Override
    public VisualdevEntity getReleaseInfo(String formId) {
        VisualdevReleaseEntity visualdevReleaseEntity = visualdevReleaseMapper.selectById(formId);
        return visualdevReleaseEntity != null ? JsonUtil.getJsonToBean(visualdevReleaseEntity, VisualdevEntity.class) : null;
    }

    @Override
    public List<Map<String, Object>> getListWithTableList(VisualDevJsonModel visualDevJsonModel, PaginationModel pagination, UserInfo userInfo) {
        return visualDevListService.getListWithTableList(visualDevJsonModel, pagination, userInfo);
    }

    @Override
    public VisualdevModelDataInfoVO getEditDataInfo(String id, VisualdevEntity visualdevEntity) {
        return visualDevInfoService.getEditDataInfo(id, visualdevEntity, OnlineInfoModel.builder().build());
    }

    @Override
    public DataModel visualCreate(VisualParamModel model) throws WorkFlowException {
        return visualdevModelDataService.visualCreate(VisualParamModel.builder().visualdevEntity(model.getVisualdevEntity()).data(model.getData()).build());
    }

    @Override
    public DataModel visualUpdate(VisualParamModel model) throws WorkFlowException {
        return visualdevModelDataService.visualUpdate(model);
    }

    @Override
    public void visualDelete(VisualParamModel model) throws WorkFlowException {
        visualdevModelDataService.visualDelete(model.getVisualdevEntity(), model.getDataList());
    }

    @Override
    public void deleteByTableName(FlowFormDataModel model) throws SQLException, WorkFlowException {
        visualdevModelDataService.deleteByTableName(model);
    }

    @Override
    public void saveState(FlowStateModel model) {
        List<String> formIds = model.getFormIds();
        String flowTaskId = model.getFlowTaskId();
        Integer flowState = model.getFlowState();
        for (String formId : formIds) {
            VisualdevReleaseEntity entity = visualdevReleaseService.getById(formId);
            if (entity == null) continue;
            VisualdevEntity visualdevEntity = JsonUtil.getJsonToBean(entity, VisualdevEntity.class);
            //系统表单
            if (entity.getType() == 2) {
                flowFormHttpReqUtils.saveState(visualdevEntity, flowTaskId, flowState);
            } else {
                flowDataUtil.saveState(visualdevEntity, flowTaskId, flowState);
            }
        }
    }

    @Override
    public List<VisualExportVo> getExportList(String systemId) {
        return visualdevService.getExportList(systemId);
    }

    @Override
    public boolean importCopy(List<VisualExportVo> list, String systemId) {
        try {
            for (VisualExportVo item : list) {
                VisualdevEntity entity = JsonUtil.getJsonToBean(item, VisualdevEntity.class);
                String id = RandomUtil.uuId();
                entity.setId(id);
                entity.setCreatorTime(DateUtil.getNowDate());
                entity.setCreatorUserId(UserProvider.getUser().getUserId());
                entity.setLastModifyTime(null);
                entity.setLastModifyUserId(null);
                entity.setState(0);
                entity.setSystemId(systemId);
                visualdevService.setAutoEnCode(entity);
                visualdevService.save(entity);
                if (StringUtil.isNotEmpty(item.getAliasListJson())) {
                    List<VisualAliasEntity> jsonToList = JsonUtil.getJsonToList(item.getAliasListJson(), VisualAliasEntity.class);
                    for (VisualAliasEntity aliasEntity : jsonToList) {
                        aliasService.copyEntity(aliasEntity, id);
                    }
                }
            }
            return true;
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return false;
    }

    @Override
    public void deleteBySystemId(String systemId) {
        visualdevService.deleteBySystemId(systemId);
    }

}
