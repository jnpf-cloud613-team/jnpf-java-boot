package jnpf.flowable.controller;

import cn.hutool.core.util.ObjectUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jnpf.base.ActionResult;
import jnpf.config.ConfigValueUtil;
import jnpf.constant.MsgCode;
import jnpf.database.util.TenantDataSourceUtil;
import jnpf.exception.WorkFlowException;
import jnpf.flowable.entity.TemplateJsonEntity;
import jnpf.flowable.enums.TemplateJsonStatueEnum;
import jnpf.flowable.model.trigger.TriggerWebHookInfoVo;
import jnpf.flowable.service.TemplateJsonService;
import jnpf.flowable.util.OperatorUtil;
import jnpf.util.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.binary.Base64;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/9/21 15:39
 */
@Tag(name = "webhook触发", description = "WebHook")
@RestController
@RequestMapping("/api/workflow/Hooks")
@RequiredArgsConstructor
public class TriggerWebHookController {



    private final  RedisUtil redisUtil;

    private final  OperatorUtil operatorUtil;

    private final  ConfigValueUtil configValueUtil;

    private  final TemplateJsonService templateJsonService;

    private static final String WEBHOOK_RED_KEY = "webhook_trigger";
    private static final long DEFAULT_CACHE_TIME = 300;

    @Operation(summary = "数据接收接口")
    @Parameter(name = "id", description = "base64转码id", required = true)
    @Parameter(name = "tenantId", description = "租户id", required = false)
    @PostMapping("/{id}")
    @NoDataSourceBind
    public ActionResult<Object> webhookTrigger(@PathVariable("id") String id,
                                       @RequestParam(value = "tenantId", required = false) String tenantId,
                                       @RequestBody Map<String, Object> body) throws WorkFlowException {
        String idReal = new String(Base64.decodeBase64(id.getBytes(StandardCharsets.UTF_8)));
        if (configValueUtil.isMultiTenancy()&&StringUtil.isNotEmpty(tenantId)) {
            // 判断是不是从外面直接请求
                //切换成租户库
                try {
                    TenantDataSourceUtil.switchTenant(tenantId);
                } catch (Exception e) {
                    return ActionResult.fail(MsgCode.LOG105.get());
                }

        }
        try {
            operatorUtil.handleWebhookTrigger(idReal, tenantId, body);
        } catch (WorkFlowException e) {
            e.printStackTrace();
            throw e;
        }
        return ActionResult.success();
    }

    @Operation(summary = "获取webhookUrl")
    @Parameter(name = "id", description = "主键", required = true)
    @GetMapping("/getUrl")
    public ActionResult<Object> getWebhookUrl(@RequestParam("id") String id) {
        String enCodeBase64 = new String(Base64.encodeBase64(id.getBytes(StandardCharsets.UTF_8)));
        String randomStr = UUID.randomUUID().toString().substring(0, 5);
        TriggerWebHookInfoVo vo = new TriggerWebHookInfoVo();
        vo.setEnCodeStr(enCodeBase64);
        vo.setRandomStr(randomStr);
        vo.setWebhookUrl("/api/workflow/Hooks/" + enCodeBase64);
        vo.setRequestUrl("/api/workflow/Hooks/" + enCodeBase64 + "/params/" + randomStr);
        return ActionResult.success(vo);
    }

    @Operation(summary = "通过get接口获取参数")
    @Parameter(name = "id", description = "base64转码id", required = true)
    @Parameter(name = "randomStr", description = "获取webhookUrl提供的随机字符", required = true)
    @GetMapping("/{id}/params/{randomStr}")
    @NoDataSourceBind
    public ActionResult<Object> getWebhookParams(@PathVariable("id") String id,
                                         @PathVariable("randomStr") String randomStr) throws WorkFlowException {
        insertRedis(id, randomStr, new HashMap<>());
        return ActionResult.success();
    }

    @Operation(summary = "通过post接口获取参数")
    @Parameter(name = "id", description = "base64转码id", required = true)
    @Parameter(name = "randomStr", description = "获取webhookUrl提供的随机字符", required = true)
    @PostMapping("/{id}/params/{randomStr}")
    @NoDataSourceBind
    public ActionResult<Object> postWebhookParams(@PathVariable("id") String id,
                                          @PathVariable("randomStr") String randomStr,
                                          @RequestBody Map<String, Object> obj) throws WorkFlowException {
        insertRedis(id, randomStr, new HashMap<>(obj));
        return ActionResult.success();
    }

    /**
     * 助手id查询信息，写入缓存
     *
     * @param id
     * @param randomStr
     * @param resultMap
     * @throws WorkFlowException
     */
    private void insertRedis(String id, String randomStr, Map<String, Object> resultMap) throws WorkFlowException {
        String idReal = new String(Base64.decodeBase64(id.getBytes(StandardCharsets.UTF_8)));
        String key1 = WEBHOOK_RED_KEY + "_" + idReal + "_" + randomStr;
        if (!redisUtil.exists(key1)) {
            throw new WorkFlowException(MsgCode.VS016.get());
        }
        String tenantId = redisUtil.getString(key1).toString();

        if (configValueUtil.isMultiTenancy()&&StringUtil.isNotEmpty(tenantId)) {
            // 判断是不是从外面直接请求

                //切换成租户库
                try {
                    TenantDataSourceUtil.switchTenant(tenantId);
                } catch (Exception e) {
                    throw new WorkFlowException(MsgCode.LOG105.get());
                }
        }
        TemplateJsonEntity jsonEntity = templateJsonService.getById(idReal);
        if (!ObjectUtil.equals(jsonEntity.getState(), TemplateJsonStatueEnum.START.getCode())) {
            throw new WorkFlowException("版本未启用");
        }
        Map<String, Object> parameterMap = new HashMap<>(ServletUtil.getRequest().getParameterMap());
        for (String key : parameterMap.keySet()) {
            String[] parameterValues = ServletUtil.getRequest().getParameterValues(key);
            if (parameterValues.length == 1) {
                parameterMap.put(key, parameterValues[0]);
            } else {
                parameterMap.put(key, parameterValues);
            }
        }
        resultMap.putAll(parameterMap);
        if (!resultMap.isEmpty()) {
            redisUtil.insert(WEBHOOK_RED_KEY + "_" + randomStr, resultMap, DEFAULT_CACHE_TIME);
            redisUtil.remove(key1);
        }
    }

    @Operation(summary = "请求参数添加触发接口")
    @Parameter(name = "id", description = "base64转码id", required = true)
    @Parameter(name = "randomStr", description = "获取webhookUrl提供的随机字符", required = true)
    @GetMapping("/{id}/start/{randomStr}")
    public ActionResult<Object> start(@PathVariable("id") String id,
                              @PathVariable("randomStr") String randomStr) {
        redisUtil.remove(WEBHOOK_RED_KEY + "_" + randomStr);
        redisUtil.insert(WEBHOOK_RED_KEY + "_" + id + "_" + randomStr, UserProvider.getUser().getTenantId(), DEFAULT_CACHE_TIME);
        return ActionResult.success();
    }

    @Operation(summary = "获取缓存的接口参数")
    @Parameter(name = "randomStr", description = "获取webhookUrl提供的随机字符", required = true)
    @GetMapping("/getParams/{randomStr}")
    public ActionResult<Object> getRedisParams(@PathVariable("randomStr") String randomStr) {
        Map<String, Object> mapRedis = new HashMap<>();
        String key = WEBHOOK_RED_KEY + "_" + randomStr;
        if (redisUtil.exists(key)) {
            mapRedis = redisUtil.getMap(key);
        }
        List<Map<String, Object>> list = new ArrayList<>();

        for (Map.Entry<String, Object> stringObjectEntry : mapRedis.entrySet()) {
            String redisKey = stringObjectEntry.getKey();
            Map<String, Object> map = new HashMap<>();
            map.put("id", redisKey);
            map.put("fullName", mapRedis.get(redisKey));
            list.add(map);
        }
        return ActionResult.success(list);
    }

}
