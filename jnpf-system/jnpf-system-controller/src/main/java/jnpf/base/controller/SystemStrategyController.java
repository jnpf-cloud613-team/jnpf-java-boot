package jnpf.base.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jnpf.base.ActionResult;
import jnpf.base.entity.SysConfigEntity;
import jnpf.base.model.systemconfig.SystemStrategyModel;
import jnpf.base.service.SysconfigService;
import jnpf.constant.MsgCode;
import jnpf.util.JsonUtil;
import jnpf.util.RandomUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "系统策略", description = "strategy")
@RestController
@RequestMapping("/api/system/strategy")
@RequiredArgsConstructor
public class SystemStrategyController {


    private final SysconfigService sysconfigService;

    @Operation(summary = "更新系统策略")
    @PostMapping
    public ActionResult<Object>saveSystemStrategy(@RequestBody SystemStrategyModel model) {
        if (Objects.nonNull(model.getVerificationCodeNumber())) {
            if (model.getVerificationCodeNumber() > 6) {
                return ActionResult.fail(MsgCode.SYS029.get());
            }
            if (model.getVerificationCodeNumber() < 3) {
                return ActionResult.fail(MsgCode.SYS030.get());
            }
        }

        List<SysConfigEntity> entitys = new ArrayList<>();
        Map<String, Object> map = JsonUtil.entityToMap(model);
        map.put("isLog", "1");
        map.put("sysTheme", "1");
        map.put("pageSize", "30");
        map.put("lastLoginTime", 1);
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            SysConfigEntity entity = new SysConfigEntity();
            entity.setId(RandomUtil.uuId());
            entity.setFkey(entry.getKey());
            entity.setValue(String.valueOf(entry.getValue()));
            entitys.add(entity);
        }

        sysconfigService.save(entitys);
        return ActionResult.success(MsgCode.SU005.get());
    }

    /**
     * 系统策略列表
     *
     * @return ignore
     */
    @Operation(summary = "系统策略")
    @GetMapping
    public ActionResult<SystemStrategyModel> list() {
        List<SysConfigEntity> list = sysconfigService.getList("SysConfig");
        Map<String, String> collect = list.stream()
                .collect(Collectors.toMap(SysConfigEntity::getFkey, SysConfigEntity::getValue));
        SystemStrategyModel systemStrategyModel = JsonUtil.getJsonToBean(collect, SystemStrategyModel.class);
        return ActionResult.success(systemStrategyModel);
    }

}
