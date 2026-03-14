package jnpf.onlinedev.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.Method;
import jnpf.base.UserInfo;
import jnpf.base.model.VisualLogModel;
import jnpf.base.model.flow.DataModel;
import jnpf.config.ConfigValueUtil;
import jnpf.flowable.model.trigger.TriggerDataFo;
import jnpf.flowable.model.trigger.TriggerDataModel;
import jnpf.flowable.model.trigger.TriggerModel;
import jnpf.onlinedev.model.AsyncExecuteModel;
import jnpf.util.Constants;
import jnpf.util.JsonUtil;
import jnpf.workflow.service.TriggerApi;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class IntegrateUtil {

    private final ConfigValueUtil configValueUtil;
    private final TriggerApi triggerApi;

    public List<TriggerDataModel> asyncDelExecute(AsyncExecuteModel executeModel) {
        String modelId = executeModel.getModelId();
        Integer trigger = executeModel.getTrigger();
        List<String> dataId = executeModel.getDataId();
        DataModel dataModel = executeModel.getDataModel();
        List<Map<String, Object>> dataMap = executeModel.getDataMap();

        TriggerDataFo fo = new TriggerDataFo();
        fo.setModelId(modelId);
        fo.setTrigger(trigger);
        fo.setDataId(dataId);
        fo.setDataMap(dataMap);
        if (dataModel != null) {
            List<VisualLogModel> listLog = dataModel.getListLog();
            if (CollUtil.isNotEmpty(listLog)) {
                List<String> updateFields = this.getUpdateFields(listLog);
                fo.setUpdateFields(updateFields);
            }
        }

        return triggerApi.getTriggerDataModel(fo);
    }

    @Async
    public void asyncExecute(AsyncExecuteModel executeModel) {
        String modelId = executeModel.getModelId();
        Integer trigger = executeModel.getTrigger();
        List<String> dataId = executeModel.getDataId();
        UserInfo userInfo = executeModel.getUserInfo();
        DataModel dataModel = executeModel.getDataModel();
        List<Map<String, Object>> dataMap = executeModel.getDataMap();

        TriggerDataFo fo = new TriggerDataFo();
        fo.setModelId(modelId);
        fo.setTrigger(trigger);
        fo.setDataId(dataId);
        fo.setDataMap(dataMap);
        if (dataModel != null) {
            List<VisualLogModel> listLog = dataModel.getListLog();
            if (CollUtil.isNotEmpty(listLog)) {
                List<String> updateFields = this.getUpdateFields(listLog);
                fo.setUpdateFields(updateFields);
            }
        }

        List<TriggerDataModel> resultData = triggerApi.getTriggerDataModel(fo);
        if (!resultData.isEmpty()) {
            String url = configValueUtil.getApiDomain() + "/api/workflow/trigger/Execute";
            TriggerModel model = new TriggerModel();
            model.setUserInfo(userInfo);
            model.setDataList(resultData);
            HttpRequest request = HttpRequest.of(url).method(Method.POST).body(JsonUtil.getObjectToString(model));
            request.header(Constants.AUTHORIZATION, userInfo.getToken());
            request.execute().body();
        }
    }

    private List<String> getUpdateFields(List<VisualLogModel> listLog) {
        List<String> updateFields = new ArrayList<>();
        if (CollUtil.isNotEmpty(listLog)) {
            for (VisualLogModel logModel : listLog) {
                String field = logModel.getField();
                List<Map<String, Object>> chidField = logModel.getChidField();
                List<Map<String, Object>> chidData = logModel.getChidData();
                if (CollUtil.isNotEmpty(chidField) && CollUtil.isNotEmpty(chidData)) {
                    // 子表
                    for (Map<String, Object> map : chidField) {
                        String key = (String) map.get("prop");
                        for (Map<String, Object> child : chidData) {
                            if (!ObjectUtil.equals(child.get(key), child.get("jnpf_old_" + key))) {
                                updateFields.add(field + "-" + key);
                                break;
                            }
                        }
                    }
                } else {
                    updateFields.add(field);
                }
            }
        }
        return updateFields;
    }
}

