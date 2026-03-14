package jnpf.controller;

import jnpf.base.controller.SuperController;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import jnpf.base.ActionResult;
import jnpf.constant.MsgCode;
import jnpf.entity.AppDataEntity;
import jnpf.model.AppDataCrForm;
import jnpf.service.AppDataService;
import jnpf.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * app常用数据
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021-07-08
 */
@Tag(name = "app常用数据", description = "data")
@RestController
@RequestMapping("/api/app/Data")
@RequiredArgsConstructor
public class AppDataController extends SuperController<AppDataService, AppDataEntity> {


    private final AppDataService appDataService;


    /**
     * 新建
     *
     * @param appDataCrForm 新建模型
     * @return
     */
    @PostMapping
    @Operation(summary = "新建")
    @Parameter(name = "appDataCrForm", description = "常用模型", required = true)
    public ActionResult<Object> create(@RequestBody @Valid AppDataCrForm appDataCrForm) {
        AppDataEntity entity = JsonUtil.getJsonToBean(appDataCrForm, AppDataEntity.class);
        if (appDataService.isExistByObjectId(entity.getObjectId(), appDataCrForm.getSystemId())) {
            return ActionResult.fail(MsgCode.FA036.get());
        }
        appDataService.create(entity);
        return ActionResult.success(MsgCode.SU001.get());
    }

    /**
     * 删除
     *
     * @param objectId 主键
     * @return
     */
    @Operation(summary = "删除")
    @DeleteMapping("/{objectId}")
    @Parameter(name = "objectId", description = "主键", required = true)
    public ActionResult<Object> create(@PathVariable("objectId") String objectId) {
        AppDataEntity entity = appDataService.getInfo(objectId);
        if (entity != null) {
            appDataService.delete(entity);
            return ActionResult.success(MsgCode.SU003.get());
        }
        return ActionResult.fail(MsgCode.FA003.get());
    }

}
