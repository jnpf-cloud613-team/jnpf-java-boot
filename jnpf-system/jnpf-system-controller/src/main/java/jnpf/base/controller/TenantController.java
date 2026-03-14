package jnpf.base.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jnpf.base.ActionResult;
import jnpf.database.util.LoginSaasUtil;
import jnpf.exception.DataException;
import jnpf.model.login.JoinCompanyVo;
import jnpf.util.DesUtil;
import jnpf.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
@Tag(name = "租户", description = "tenant")
@RestController
@RequestMapping("/api/system/tenant/saas")
public class TenantController {



    @Operation(summary = "企业信息数据")
    @GetMapping("/inviteurl")
    public ActionResult<Object>analysisUrl(@RequestParam("encryption") String encryption) {


        String decrypt = DesUtil.aesOrDecode(encryption, false, true);
        String[] split = decrypt.split("\\|");
        if (split.length != 2) {
            throw new DataException("链接失效");
        }
        String time = split[1];
        String now = new SimpleDateFormat("yyyyMMdd").format(new Date());
        if (time.compareTo(now) < 0) {
            throw new DataException("邀请链接过期");
        }
        JoinCompanyVo joinCompanyVo = LoginSaasUtil.getJoinCompanyVo(split[0]);
        String encrypt = DesUtil.aesOrDecode(JsonUtil.getObjectToString(joinCompanyVo), true, true);
        return ActionResult.success("",encrypt);
    }


}
