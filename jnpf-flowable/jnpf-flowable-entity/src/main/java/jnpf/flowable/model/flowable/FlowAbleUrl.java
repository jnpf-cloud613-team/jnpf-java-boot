package jnpf.flowable.model.flowable;

import com.alibaba.fastjson.JSONObject;
import jnpf.config.ConfigValueUtil;
import jnpf.constant.MsgCode;
import jnpf.exception.WorkFlowException;
import jnpf.util.JsonUtil;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;
import jnpf.util.text.CharsetKit;
import jnpf.util.wxutil.HttpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class FlowAbleUrl {


    private final ConfigValueUtil configValueUtil;

    public static final String DEPLOYMENT_ID = "?deploymentId=";
    public static final String TASK_ID = "?taskId=";
    public static final String TASK_KEY = "&taskKey=";

    public String deployFlowAble(String flowXml, String key) throws WorkFlowException {
        String url = getFlowAbleUrl() + "/api/Flow/definition/deploy";
        FlowAbleForm flowAbleForm = new FlowAbleForm();
        flowAbleForm.setKey(key);
        try {
            flowAbleForm.setBpmnXml(URLDecoder.decode(flowXml, CharsetKit.UTF_8));
        } catch (Exception e) {
            flowAbleForm.setBpmnXml(flowXml);
        }
        JSONObject jsonObject = HttpUtil.httpRequest(url, "POST", JsonUtil.getObjectToString(flowAbleForm), UserProvider.getToken());
        FlowAbleModel flowAbleModel = JsonUtil.getJsonToBean(jsonObject, FlowAbleModel.class);
        if (null == flowAbleModel) {
            log.error(MsgCode.WF090.get());
            throw new WorkFlowException(MsgCode.WF142.get());
        }
        if (Boolean.FALSE.equals(flowAbleModel.getSuccess())) {
            throw new WorkFlowException(flowAbleModel.getMsg());
        }
        FlowAbleData data = JsonUtil.getJsonToBean(flowAbleModel.getData(), FlowAbleData.class);
        return data.getDeploymentId();
    }

    private String getFlowAbleUrl() {
        return configValueUtil.getFlowDomain();
    }

    /**
     * 启动流程实例
     *
     * @param deploymentId 引擎部署ID
     * @param variables    变量
     */
    public String startInstance(String deploymentId, Map<String, Object> variables) throws WorkFlowException {
        String url = getFlowAbleUrl() + "/api/Flow/instance/start";
        InstanceStartFo fo = new InstanceStartFo();
        fo.setDeploymentId(deploymentId);
        fo.setVariables(variables);
        JSONObject jsonObject = HttpUtil.httpRequest(url, "POST", JsonUtil.getObjectToString(fo), UserProvider.getToken());
        FlowAbleModel flowAbleModel = JsonUtil.getJsonToBean(jsonObject, FlowAbleModel.class);
        if (null == flowAbleModel) {
            log.error(MsgCode.WF091.get());
            throw new WorkFlowException(MsgCode.WF142.get());
        }
        if (Boolean.FALSE.equals(flowAbleModel.getSuccess())) {
            throw new WorkFlowException(flowAbleModel.getMsg());
        }
        FlowAbleData data = JsonUtil.getJsonToBean(flowAbleModel.getData(), FlowAbleData.class);
        return data.getInstanceId();
    }

    /**
     * 获取当前任务
     *
     * @param instanceId 引擎实例ID
     */
    public List<FlowableTaskModel> getCurrentTask(String instanceId) throws WorkFlowException {
        String url = getFlowAbleUrl() + "/api/Flow/task/list/" + instanceId;
        JSONObject jsonObject = HttpUtil.httpRequest(url, "GET", null, UserProvider.getToken());
        FlowAbleModel flowAbleModel = JsonUtil.getJsonToBean(jsonObject, FlowAbleModel.class);
        if (null == flowAbleModel) {
            log.error(MsgCode.WF092.get());
            throw new WorkFlowException(MsgCode.WF142.get());
        }
        if (Boolean.FALSE.equals(flowAbleModel.getSuccess())) {
            throw new WorkFlowException(flowAbleModel.getMsg());
        }
        return JsonUtil.getJsonToList(flowAbleModel.getData(), FlowableTaskModel.class);
    }

    /**
     * 删除流程实例
     *
     * @param instanceId   实例ID
     * @param deleteReason 删除原因
     */
    public void deleteInstance(String instanceId, String deleteReason) throws WorkFlowException {
        String url = getFlowAbleUrl() + "/api/Flow/instance?instanceId=" + instanceId;
        if (StringUtil.isNotEmpty(deleteReason)) {
            url += "&deleteReason=" + deleteReason;
        }
        JSONObject jsonObject = HttpUtil.httpRequest(url, "DELETE", null, UserProvider.getToken());
        FlowAbleModel flowAbleModel = JsonUtil.getJsonToBean(jsonObject, FlowAbleModel.class);
        if (null == flowAbleModel || !flowAbleModel.getSuccess()) {
            log.error("流程实例删除异常: {}", flowAbleModel);
        }
    }

    /**
     * 获取出线Key集合
     *
     * @param fo 参数类
     */
    public List<String> getOutgoingFlows(OutgoingFlowsFo fo) throws WorkFlowException {
        String url = getFlowAbleUrl() + "/api/Flow/task/outgoing/flows";
        if (StringUtil.isNotEmpty(fo.getTaskId())) {
            url += TASK_ID + fo.getTaskId();
        } else {
            url += DEPLOYMENT_ID + fo.getDeploymentId() + TASK_KEY + fo.getTaskKey();
        }
        JSONObject jsonObject = HttpUtil.httpRequest(url, "GET", null, UserProvider.getToken());
        FlowAbleModel flowAbleModel = JsonUtil.getJsonToBean(jsonObject, FlowAbleModel.class);
        if (null == flowAbleModel) {
            log.error(MsgCode.WF094.get());
            throw new WorkFlowException(MsgCode.WF142.get());
        }
        if (Boolean.FALSE.equals(flowAbleModel.getSuccess())) {
            throw new WorkFlowException(flowAbleModel.getMsg());
        }
        return JsonUtil.getJsonToList(flowAbleModel.getData(), String.class);
    }

    /**
     * 获取线之后的任务节点
     *
     * @param deploymentId 部署id
     * @param flowKey      连接线的Key
     */
    public List<String> getTaskKeyAfterFlow(String deploymentId, String flowKey) throws WorkFlowException {
        String url = getFlowAbleUrl() + "/api/Flow/task/flow/target?deploymentId=" + deploymentId + "&flowKey=" + flowKey;
        JSONObject jsonObject = HttpUtil.httpRequest(url, "GET", null, UserProvider.getToken());
        FlowAbleModel flowAbleModel = JsonUtil.getJsonToBean(jsonObject, FlowAbleModel.class);
        if (null == flowAbleModel) {
            log.error(MsgCode.WF095.get());
            throw new WorkFlowException(MsgCode.WF142.get());
        }
        if (Boolean.FALSE.equals(flowAbleModel.getSuccess())) {
            throw new WorkFlowException(flowAbleModel.getMsg());
        }
        return JsonUtil.getJsonToList(flowAbleModel.getData(), String.class);
    }

    /**
     * 获取下一级任务节点集合
     *
     * @param fo 参数类
     */
    public List<FlowableNodeModel> getNext(NextOrPrevFo fo) throws WorkFlowException {
        String url = getFlowAbleUrl() + "/api/Flow/task/next";
        if (StringUtil.isNotEmpty(fo.getTaskId())) {
            url += TASK_ID + fo.getTaskId();
        } else {
            url += DEPLOYMENT_ID + fo.getDeploymentId() + TASK_KEY + fo.getTaskKey();
        }
        JSONObject jsonObject = HttpUtil.httpRequest(url, "GET", null, UserProvider.getToken());
        FlowAbleModel flowAbleModel = JsonUtil.getJsonToBean(jsonObject, FlowAbleModel.class);
        if (null == flowAbleModel) {
            log.error(MsgCode.WF096.get());
            throw new WorkFlowException(MsgCode.WF142.get());
        }
        if (Boolean.FALSE.equals(flowAbleModel.getSuccess())) {
            throw new WorkFlowException(flowAbleModel.getMsg());
        }
        return JsonUtil.getJsonToList(flowAbleModel.getData(), FlowableNodeModel.class);
    }

    /**
     * 获取上一级任务节点id集合
     *
     * @param fo 参数类
     */
    public List<String> getPrev(NextOrPrevFo fo) throws WorkFlowException {
        String url = getFlowAbleUrl() + "/api/Flow/task/prev";
        if (StringUtil.isNotEmpty(fo.getTaskId())) {
            url += TASK_ID + fo.getTaskId();
        } else {
            url += DEPLOYMENT_ID + fo.getDeploymentId() + TASK_KEY + fo.getTaskKey();
        }
        JSONObject jsonObject = HttpUtil.httpRequest(url, "GET", null, UserProvider.getToken());
        FlowAbleModel flowAbleModel = JsonUtil.getJsonToBean(jsonObject, FlowAbleModel.class);
        if (null == flowAbleModel) {
            log.error(MsgCode.WF097.get());
            throw new WorkFlowException(MsgCode.WF142.get());
        }
        if (Boolean.FALSE.equals(flowAbleModel.getSuccess())) {
            throw new WorkFlowException(flowAbleModel.getMsg());
        }
        return JsonUtil.getJsonToList(flowAbleModel.getData(), String.class);
    }

    /**
     * 完成流程任务
     *
     * @param fo 参数类
     */
    public void complete(CompleteFo fo) throws WorkFlowException {
        String url = getFlowAbleUrl() + "/api/Flow/task/complete";
        JSONObject jsonObject = HttpUtil.httpRequest(url, "POST", JsonUtil.getObjectToString(fo), UserProvider.getToken());
        FlowAbleModel flowAbleModel = JsonUtil.getJsonToBean(jsonObject, FlowAbleModel.class);
        if (null == flowAbleModel) {
            log.error(MsgCode.WF098.get());
            throw new WorkFlowException(MsgCode.WF142.get());
        }
        if (Boolean.FALSE.equals(flowAbleModel.getSuccess())) {
            throw new WorkFlowException(flowAbleModel.getMsg());
        }
    }

    /**
     * 获取流程实例
     *
     * @param instanceId 实例id
     */
    public FlowableInstanceModel getInstance(String instanceId) throws WorkFlowException {
        String url = getFlowAbleUrl() + "/api/Flow/instance/" + instanceId;
        JSONObject jsonObject = HttpUtil.httpRequest(url, "GET", null, UserProvider.getToken());
        FlowAbleModel flowAbleModel = JsonUtil.getJsonToBean(jsonObject, FlowAbleModel.class);
        if (null == flowAbleModel) {
            log.error(MsgCode.WF099.get());
            throw new WorkFlowException(MsgCode.WF142.get());
        }
        if (Boolean.FALSE.equals(flowAbleModel.getSuccess())) {
            throw new WorkFlowException(flowAbleModel.getMsg());
        }
        return JsonUtil.getJsonToBean(flowAbleModel.getData(), FlowableInstanceModel.class);
    }

    /**
     * 获取未经过的节点
     *
     * @param instanceId 引擎实例主键
     */
    public List<String> getTobePass(String instanceId) throws WorkFlowException {
        String url = getFlowAbleUrl() + "/api/Flow/task/tobe/pass/" + instanceId;
        JSONObject jsonObject = HttpUtil.httpRequest(url, "GET", null, UserProvider.getToken());
        FlowAbleModel flowAbleModel = JsonUtil.getJsonToBean(jsonObject, FlowAbleModel.class);
        if (null == flowAbleModel) {
            log.error(MsgCode.WF100.get());
            throw new WorkFlowException(MsgCode.WF142.get());
        }
        if (Boolean.FALSE.equals(flowAbleModel.getSuccess())) {
            throw new WorkFlowException(flowAbleModel.getMsg());
        }
        return JsonUtil.getJsonToList(flowAbleModel.getData(), String.class);
    }

    /**
     * 获取节点的后续节点
     *
     * @param fo 参数
     */
    public List<String> getAfter(AfterFo fo) throws WorkFlowException {
        String url = getFlowAbleUrl() + "/api/Flow/task/after";
        JSONObject jsonObject = HttpUtil.httpRequest(url, "POST", JsonUtil.getObjectToString(fo), UserProvider.getToken());
        FlowAbleModel flowAbleModel = JsonUtil.getJsonToBean(jsonObject, FlowAbleModel.class);
        if (null == flowAbleModel) {
            log.error(MsgCode.WF101.get());
            throw new WorkFlowException(MsgCode.WF142.get());
        }
        if (Boolean.FALSE.equals(flowAbleModel.getSuccess())) {
            throw new WorkFlowException(flowAbleModel.getMsg());
        }
        return JsonUtil.getJsonToList(flowAbleModel.getData(), String.class);
    }

    /**
     * 获取可回退的节点ID
     *
     * @param taskId 引擎任务主键
     */
    public List<String> getFallbacks(String taskId) throws WorkFlowException {
        String url = getFlowAbleUrl() + "/api/Flow/task/fallbacks/" + taskId;
        JSONObject jsonObject = HttpUtil.httpRequest(url, "GET", null, UserProvider.getToken());
        FlowAbleModel flowAbleModel = JsonUtil.getJsonToBean(jsonObject, FlowAbleModel.class);
        if (null == flowAbleModel) {
            log.error(MsgCode.WF102.get());
            throw new WorkFlowException(MsgCode.WF142.get());
        }
        if (Boolean.FALSE.equals(flowAbleModel.getSuccess())) {
            throw new WorkFlowException(flowAbleModel.getMsg());
        }
        return JsonUtil.getJsonToList(flowAbleModel.getData(), String.class);
    }

    /**
     * 退回
     *
     * @param fo 参数
     */
    public void back(BackFo fo) throws WorkFlowException {
        String url = getFlowAbleUrl() + "/api/Flow/task/back";
        JSONObject jsonObject = HttpUtil.httpRequest(url, "POST", JsonUtil.getObjectToString(fo), UserProvider.getToken());
        FlowAbleModel flowAbleModel = JsonUtil.getJsonToBean(jsonObject, FlowAbleModel.class);
        if (null == flowAbleModel) {
            log.error(MsgCode.WF103.get());
            throw new WorkFlowException(MsgCode.WF142.get());
        }
        if (Boolean.FALSE.equals(flowAbleModel.getSuccess())) {
            throw new WorkFlowException(flowAbleModel.getMsg());
        }
    }

    /**
     * 节点跳转
     *
     * @param fo 参数
     */
    public void jump(JumpFo fo) throws WorkFlowException {
        String url = getFlowAbleUrl() + "/api/Flow/task/jump";
        JSONObject jsonObject = HttpUtil.httpRequest(url, "POST", JsonUtil.getObjectToString(fo), UserProvider.getToken());
        FlowAbleModel flowAbleModel = JsonUtil.getJsonToBean(jsonObject, FlowAbleModel.class);
        if (null == flowAbleModel) {
            log.error(MsgCode.WF104.get());
            throw new WorkFlowException(MsgCode.WF142.get());
        }
        if (Boolean.FALSE.equals(flowAbleModel.getSuccess())) {
            throw new WorkFlowException(flowAbleModel.getMsg());
        }
    }

    /**
     * 任务完成的补偿
     *
     * @param fo 参数
     */
    public List<FlowableTaskModel> compensate(CompensateFo fo) throws WorkFlowException {
        String url = getFlowAbleUrl() + "/api/Flow/task/compensate";
        JSONObject jsonObject = HttpUtil.httpRequest(url, "POST", JsonUtil.getObjectToString(fo), UserProvider.getToken());
        FlowAbleModel flowAbleModel = JsonUtil.getJsonToBean(jsonObject, FlowAbleModel.class);
        if (null == flowAbleModel) {
            log.error(MsgCode.WF105.get());
            throw new WorkFlowException(MsgCode.WF142.get());
        }
        if (Boolean.FALSE.equals(flowAbleModel.getSuccess())) {
            throw new WorkFlowException(flowAbleModel.getMsg());
        }
        return JsonUtil.getJsonToList(flowAbleModel.getData(), FlowableTaskModel.class);
    }

    /**
     * 获取历史节点
     *
     * @param instanceId 实例主键
     */
    public List<FlowableHistoricModel> getHistoric(String instanceId) throws WorkFlowException {
        String url = getFlowAbleUrl() + "/api/Flow/task/historic/" + instanceId;
        JSONObject jsonObject = HttpUtil.httpRequest(url, "GET", null, UserProvider.getToken());
        FlowAbleModel flowAbleModel = JsonUtil.getJsonToBean(jsonObject, FlowAbleModel.class);
        if (null == flowAbleModel) {
            log.error("获取历史节点失败");
            throw new WorkFlowException(MsgCode.WF142.get());
        }
        if (Boolean.FALSE.equals(flowAbleModel.getSuccess())) {
            throw new WorkFlowException(flowAbleModel.getMsg());
        }
        return JsonUtil.getJsonToList(flowAbleModel.getData(), FlowableHistoricModel.class);
    }

    /**
     * 获取历史结束节点
     *
     * @param instanceId 实例主键
     */
    public List<String> getHistoricEnd(String instanceId) throws WorkFlowException {
        String url = getFlowAbleUrl() + "/api/Flow/task/historic/end/" + instanceId;
        JSONObject jsonObject = HttpUtil.httpRequest(url, "GET", null, UserProvider.getToken());
        FlowAbleModel flowAbleModel = JsonUtil.getJsonToBean(jsonObject, FlowAbleModel.class);
        if (null == flowAbleModel) {
            log.error("获取历史结束节点失败");
            throw new WorkFlowException(MsgCode.WF142.get());
        }
        if (Boolean.FALSE.equals(flowAbleModel.getSuccess())) {
            throw new WorkFlowException(flowAbleModel.getMsg());
        }
        return JsonUtil.getJsonToList(flowAbleModel.getData(), String.class);
    }

    /**
     * 获取元素信息
     *
     * @param deploymentId 部署id
     * @param key          节点key
     */
    public FlowableNodeModel getElementInfo(String deploymentId, String key) throws WorkFlowException {
        String url = getFlowAbleUrl() + "/api/Flow/task/element/info";
        url += DEPLOYMENT_ID + deploymentId + "&key=" + key;
        JSONObject jsonObject = HttpUtil.httpRequest(url, "GET", null, UserProvider.getToken());
        FlowAbleModel flowAbleModel = JsonUtil.getJsonToBean(jsonObject, FlowAbleModel.class);
        if (null == flowAbleModel) {
            log.error("获取元素信息失败");
            throw new WorkFlowException(MsgCode.WF142.get());
        }
        if (Boolean.FALSE.equals(flowAbleModel.getSuccess())) {
            throw new WorkFlowException(flowAbleModel.getMsg());
        }
        return JsonUtil.getJsonToBean(flowAbleModel.getData(), FlowableNodeModel.class);
    }
}
