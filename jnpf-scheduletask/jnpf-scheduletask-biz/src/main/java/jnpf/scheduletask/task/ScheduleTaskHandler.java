package jnpf.scheduletask.task;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import jnpf.base.UserInfo;
import jnpf.base.entity.DataInterfaceEntity;
import jnpf.base.service.DataInterfaceService;
import jnpf.base.service.DbLinkService;
import jnpf.database.util.TenantDataSourceUtil;
import jnpf.emnus.TemplateEnum;
import jnpf.exception.TenantInvalidException;
import jnpf.model.visualjson.TemplateJsonModel;
import jnpf.scheduletask.entity.TimeTaskEntity;
import jnpf.scheduletask.model.ContentNewModel;
import jnpf.scheduletask.model.UpdateTaskModel;
import jnpf.scheduletask.rest.RestScheduleTaskUtil;
import jnpf.util.AuthUtil;
import jnpf.util.JsonUtil;
import jnpf.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Api和数据接口使用
 *
 * @author ：JNPF开发平台组
 * @version: V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date ：2022/3/23 9:25
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleTaskHandler {


    private final DataInterfaceService dataInterfaceService;

    private final DbLinkService dbLinkService;

    @XxlJob("defaultHandler")
    public void defaultHandler() {
        // 获取参数
        String param = XxlJobHelper.getJobParam();
        // 转换成模型
        TimeTaskEntity entity = JsonUtil.getJsonToBean(param, TimeTaskEntity.class);
        ContentNewModel model = JsonUtil.getJsonToBean(param, ContentNewModel.class);
        String tenantId = StringUtil.isNotEmpty(model.getUserInfo().getTenantId()) ? model.getUserInfo().getTenantId() : "";
        String userId = StringUtil.isNotEmpty(model.getUserInfo().getUserId()) ? model.getUserInfo().getUserId() : "";
        String token = AuthUtil.loginTempUser(userId, tenantId, true);

        // 切换租户
        UserInfo userInfo = model.getUserInfo();
        // 切换数据源
        if (userInfo != null && StringUtil.isNotEmpty(userInfo.getTenantId())) {
            try {
                TenantDataSourceUtil.switchTenant(userInfo.getTenantId());
            }catch (TenantInvalidException e){
                // 租户无效 禁用任务
                log.error("ScheduleTaskHandler, 租户无效, 禁用任务：{}", userInfo.getTenantId());
                entity = RestScheduleTaskUtil.getInfo(entity.getId(), model.getUserInfo());
                entity.setEnabledMark(0);
                UpdateTaskModel updateTaskModel = new UpdateTaskModel();
                updateTaskModel.setEntity(entity);
                updateTaskModel.setUserInfo(model.getUserInfo());
                RestScheduleTaskUtil.updateTask(updateTaskModel);
            }
        }
        // 如果是http
        if ("1".equals(entity.getExecuteType())||("2".equals(entity.getExecuteType()))) {
            callHttp(model, token);
        }
    }

    // ---------------START callSQL


    // ---------------START callHttp

    /**
     * HTTP调用
     *
     * @param model 系统调度参数
     * @return
     */
    private Boolean callHttp(ContentNewModel model, String token) {
        try {
            // 得到数据接口信息
            String tenantId = StringUtil.isNotEmpty(model.getUserInfo().getTenantId()) ? model.getUserInfo().getTenantId() : "0";
            DataInterfaceEntity entity = dataInterfaceService.getInfo(model.getInterfaceId());
            if (entity != null) {
                Map<String, String> map = null;
                if (model.getParameter() != null && !model.getParameter().isEmpty()) {
                    map = new HashMap<>(16);
                    for (TemplateJsonModel parameterModel : model.getParameter()) {
                        String value = Objects.equals(parameterModel.getSourceType(), TemplateEnum.EMPTY.getCode()) ? "" : parameterModel.getRelationField();
                        map.put(parameterModel.getField(), value);
                    }
                }
                dataInterfaceService.infoToId(entity.getId(), tenantId, map, token, null, null, null, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


}
