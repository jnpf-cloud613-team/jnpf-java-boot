package jnpf.base.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import jnpf.base.ActionResult;
import jnpf.base.ActionResultCode;
import jnpf.base.entity.VisualdevEntity;
import jnpf.config.ConfigValueUtil;
import jnpf.constant.MsgCode;
import jnpf.exception.WorkFlowException;
import jnpf.util.JsonUtil;
import jnpf.util.UserProvider;
import jnpf.util.context.SpringContext;
import jnpf.util.wxutil.HttpUtil;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 流程表单 http请求处理表单
 *
 * @author JNPF开发平台组
 * @version V3.4.5
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2022/10/21
 */
@Component
public class FlowFormHttpReqUtils {

    private static ConfigValueUtil configValueUtil = SpringContext.getBean(ConfigValueUtil.class);

    public Map<String, Object> info(VisualdevEntity visualdevEntity, String id, String token) {
        String requestURL = this.getReqURL(visualdevEntity, id);
        JSONObject jsonObject = HttpUtil.httpRequest(requestURL, "GET", null, token);
        ActionResult<Object> actionResult = JSON.toJavaObject(jsonObject, ActionResult.class);
        if (actionResult == null) {
            return new HashMap<>();
        }
        Object data = actionResult.getData();
        return data != null ? JsonUtil.entityToMap(data) : new HashMap<>();
    }

    public boolean isUpdate(VisualdevEntity visualdevEntity, String id, String token) {
        String requestURL = this.getReqURL(visualdevEntity, id);
        JSONObject jsonObject = HttpUtil.httpRequest(requestURL, "GET", null, token);
        ActionResult<Object> actionResult = JSON.toJavaObject(jsonObject, ActionResult.class);
        return actionResult != null && actionResult.getData() != null;
    }

    public void create(VisualdevEntity visualdevEntity, String id, String token, Map<String, Object> map) throws WorkFlowException {
        String requestURL = this.getReqURL(visualdevEntity, id);
        map.remove("id");
        JSONObject jsonObject = HttpUtil.httpRequest(requestURL, "POST", JsonUtil.getObjectToString(map), token);
        ActionResult<Object> actionResult = JSON.toJavaObject(jsonObject, ActionResult.class);
        boolean b = actionResult != null && ActionResultCode.SUCCESS.getCode().equals(actionResult.getCode());
        if (!b) {
            String msg = actionResult != null ? actionResult.getMsg() : MsgCode.FM001.get();
            throw new WorkFlowException(msg);
        }
    }

    public void update(VisualdevEntity visualdevEntity, String id, String token, Map<String, Object> map) throws WorkFlowException {
        String requestURL = this.getReqURL(visualdevEntity, id);
        JSONObject jsonObject = HttpUtil.httpRequest(requestURL, "PUT", JsonUtil.getObjectToString(map), token);
        ActionResult<Object> actionResult = JSON.toJavaObject(jsonObject, ActionResult.class);
        boolean b = actionResult != null && ActionResultCode.SUCCESS.getCode().equals(actionResult.getCode());
        if (!b) {
            String msg = actionResult != null ? actionResult.getMsg() : MsgCode.FM001.get();
            throw new WorkFlowException(msg);
        }
    }

    public void saveOrUpdate(VisualdevEntity visualdevEntity, String id, String token, Map<String, Object> map) throws WorkFlowException {
        boolean update = this.isUpdate(visualdevEntity, id, token);
        if (update) {
            this.update(visualdevEntity, id, token, map);
        } else {
            this.create(visualdevEntity, id, token, map);
        }
    }


    private String getReqURL(VisualdevEntity visualdevEntity, String id) {
        //请求来源
        String requestURL = visualdevEntity.getInterfaceUrl();
        boolean isHttp = requestURL.toLowerCase().startsWith("http");
        if (!isHttp) {
            //补全(内部)
            requestURL = configValueUtil.getApiDomain() + requestURL;
        }
        return requestURL + "/" + id;
    }

    /**
     * 删除数据
     *
     * @param visualdevEntity
     * @param id
     * @param token
     * @throws WorkFlowException
     */
    public void delete(VisualdevEntity visualdevEntity, String id, String token) throws WorkFlowException {
        String requestURL = this.getReqURL(visualdevEntity, id) + "?forceDel=true";
        JSONObject jsonObject = HttpUtil.httpRequest(requestURL, "DELETE", null, token);
        ActionResult<Object> actionResult = JSON.toJavaObject(jsonObject, ActionResult.class);
        boolean b = actionResult != null && ActionResultCode.SUCCESS.getCode().equals(actionResult.getCode());
        if (!b) {
            String msg = actionResult != null ? actionResult.getMsg() : MsgCode.FM001.get();
            throw new WorkFlowException(msg);
        }
    }

    /**
     * 流程状态修改
     *
     * @param visualdevEntity
     * @param flowTaskId
     * @param flowState
     */
    public void saveState(VisualdevEntity visualdevEntity, String flowTaskId, Integer flowState) {
        //请求来源
        String requestURL = visualdevEntity.getInterfaceUrl();
        boolean isHttp = requestURL.toLowerCase().startsWith("http");
        if (!isHttp) {
            //补全(内部)
            requestURL = configValueUtil.getApiDomain() + requestURL;
        }
        requestURL += "/saveState?flowTaskId=" + flowTaskId + "&flowState=" + flowState;
        HttpUtil.httpRequest(requestURL, "POST", null, UserProvider.getToken());
    }
}
