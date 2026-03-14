package jnpf.exception;

import cn.dev33.satoken.exception.SameTokenInvalidException;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.alibaba.fastjson.JSON;
import jnpf.base.ActionResult;
import jnpf.base.ActionResultCode;
import jnpf.base.UserInfo;
import jnpf.config.ConfigValueUtil;
import jnpf.constant.MsgCode;
import jnpf.database.util.NotTenantPluginHolder;
import jnpf.database.util.TenantDataSourceUtil;
import jnpf.entity.LogEntity;
import jnpf.service.LogService;
import jnpf.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/16 10:10
 */
@Slf4j
@Controller
@ControllerAdvice
public class ResultExceptionHand extends BasicErrorController {


    private LogService logService;

    private ConfigValueUtil configValueUtil;
    @Autowired
    public void setLogService(LogService logService) {
        this.logService = logService;
    }
    @Autowired
    public void setConfigValueUtil(ConfigValueUtil configValueUtil) {
        this.configValueUtil = configValueUtil;
    }

    public ResultExceptionHand(){
        super(new DefaultErrorAttributes(), new ErrorProperties());
    }

    @ResponseBody
    @ExceptionHandler(value = LoginException.class)
    public ActionResult<Object> loginException(LoginException e) {
        ActionResult<Object> result = ActionResult.fail(ActionResultCode.FAIL.getCode(), e.getMessage());
        result.setData(e.getData());
        return result;
    }

    /**
     * 简单异常处理
     */
    @ResponseBody
    @ExceptionHandler(value = {ImportException.class, DataException.class, EncryptFailException.class})
    public ActionResult<Object> simpleException(Exception e) {
        return ActionResult.fail(ActionResultCode.FAIL.getCode(), e.getMessage());
    }



    /**
     * 租户数据库异常
     *
     * @param e
     * @return
     */
    @ResponseBody
    @ExceptionHandler(value = {TenantDatabaseException.class, TenantInvalidException.class})
    public ActionResult<String> tenantDatabaseException(TenantInvalidException e) {
        String msg;
        if(e.getMessage() == null){
            if (configValueUtil.getMultiTenancyUrl().contains("https")) {
                // https 官网提示
                msg = MsgCode.LOG109.get();
            } else {
                msg = MsgCode.LOG110.get();
            }
        }else{
            msg = e.getMessage();
        }
        if(e.getLogMsg() != null){
            log.error(e.getLogMsg());
        }
        return ActionResult.fail(ActionResultCode.FAIL.getCode(), msg);
    }





    @ResponseBody
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ActionResult<Object> methodArgumentNotValidException(MethodArgumentNotValidException e) {
        Map<String, String> map = new HashMap<>(16);
        List<ObjectError> allErrors = e.getBindingResult().getAllErrors();
        for (ObjectError allError : allErrors) {
            String[] codes = allError.getCodes();
            if (codes != null && codes.length > 0) {
                String s = codes[0];
                //用分割的方法得到字段名
                String[] parts = s.split("\\.");
                String part1 = parts[parts.length - 1];
                map.put(part1, allError.getDefaultMessage());
            }

        }
        String json = JSON.toJSONString(map);
        ActionResult<Object> result = ActionResult.fail(ActionResultCode.VALIDATEERROR.getCode(), json);
        printLog(e, "字段验证异常", 4);
        return result;
    }

    @ResponseBody
    @ExceptionHandler(value = WorkFlowException.class)
    public ActionResult<Object> workFlowException(WorkFlowException e) {
        if (e.getCode() == 200) {
            Map<String, Object> map = JsonUtil.stringToMap(e.getMessage());
            return ActionResult.success(map);
        } else {
            if(e.getSuppressed()!=null) {
                printLog(e, "系统异常", 4);
            }
            return ActionResult.fail(e.getMessage());
        }
    }

    @ResponseBody
    @ExceptionHandler(value = WxErrorException.class)
    public ActionResult<Object> wxErrorException(WxErrorException e) {
        return ActionResult.fail(e.getError().getErrorCode(), MsgCode.AD103.get());
    }

    @ResponseBody
    @ExceptionHandler(value = ServletException.class)
    public void exception(ServletException e) throws Exception {
        log.error("系统异常:" + e.getMessage(), e);
        printLog(e, "系统异常", 4);
        throw new IllegalArgumentException();
    }

    @ResponseBody
    @ExceptionHandler(value = Exception.class)
    public ActionResult<Object> exception(Exception e) {
        log.error("系统异常:" + e.getMessage(), e);
        printLog(e, "系统异常", 4);
        if(e instanceof ConnectDatabaseException || e.getCause() instanceof ConnectDatabaseException){
            Throwable t = e;
            if(e.getCause() instanceof ConnectDatabaseException){
                t = e.getCause();
            }
            return ActionResult.fail(ActionResultCode.FAIL.getCode(), t.getMessage());
        }
        return ActionResult.fail(ActionResultCode.FAIL.getCode(), MsgCode.AD102.get());
    }

    /**
     * 权限码异常
     */
    @ResponseBody
    @ExceptionHandler(NotPermissionException.class)
    public ActionResult<Void> handleNotPermissionException(NotPermissionException e) {
        return ActionResult.fail(ActionResultCode.FAIL.getCode(),  MsgCode.AD104.get());
    }

    /**
     * 角色权限异常
     */
    @ResponseBody
    @ExceptionHandler(NotRoleException.class)
    public ActionResult<Void> handleNotRoleException(NotRoleException e) {
        return ActionResult.fail(ActionResultCode.VALIDATEERROR.getCode(), MsgCode.AD104.get());
    }

    /**
     * 认证失败
     */
    @ResponseBody
    @ExceptionHandler(NotLoginException.class)
    public ActionResult<Void> handleNotLoginException(NotLoginException e) {
        return ActionResult.fail(ActionResultCode.SESSIONOVERDUE.getCode(), MsgCode.AD105.get());
    }

    /**
     * 无效认证
     */
    @ResponseBody
    @ExceptionHandler(SameTokenInvalidException.class)
    public ActionResult<Void> handleIdTokenInvalidException(SameTokenInvalidException e) {
        return ActionResult.fail(ActionResultCode.SESSIONOVERDUE.getCode(), MsgCode.AD106.get());
    }

    private void printLog(Exception e, String msg, int type) {
        try {
            UserInfo userInfo = UserProvider.getUser();
            if (userInfo.getId() == null) {
                e.printStackTrace();
                return;
            }
            //接口错误将不会进入数据库切源拦截器需要手动设置
            if (configValueUtil.isMultiTenancy() && TenantHolder.getDatasourceId() == null) {
                try {
                    TenantDataSourceUtil.switchTenant(userInfo.getTenantId());
                } catch (Exception ee){
                    e.printStackTrace();
                    return;
                }
            }
            LogEntity entity = new LogEntity();
            entity.setId(RandomUtil.uuId());
            entity.setUserId(userInfo.getUserId());
            entity.setUserName(userInfo.getUserName() + "/" + userInfo.getUserAccount());

                entity.setDescription(msg);

            StringBuilder sb = new StringBuilder();
            sb.append(e.toString() + "\n");
            StackTraceElement[] stackArray = e.getStackTrace();
            for (int i = 0; i < stackArray.length; i++) {
                StackTraceElement element = stackArray[i];
                sb.append(element.toString()).append("\n");
            }
            entity.setJsons(sb.toString());
            entity.setRequestUrl(ServletUtil.getRequest().getServletPath());
            entity.setRequestMethod(ServletUtil.getRequest().getMethod());
            entity.setType(type);
            entity.setUserId(userInfo.getUserId());
            // ip
            String ipAddr = IpUtil.getIpAddr();
            entity.setIpAddress(ipAddr);
            entity.setIpAddressName(IpUtil.getIpCity(ipAddr));
            entity.setCreatorTime(new Date());
            UserAgent userAgent = UserAgentUtil.parse(ServletUtil.getUserAgent());
            if (userAgent != null) {
                entity.setPlatForm(userAgent.getPlatform().getName() + " " + userAgent.getOsVersion());
                entity.setBrowser(userAgent.getBrowser().getName() + " " + userAgent.getVersion());
            }
            if (configValueUtil.isMultiTenancy() && StringUtil.isEmpty(TenantHolder.getDatasourceId())) {
                log.error("请求异常， 无登陆租户：" + ReflectionUtil.toString(entity), e);
            } else {
                logService.save(entity);
            }
        }catch (Exception g){
            log.error(g.getMessage());
        }finally {
            UserProvider.clearLocalUser();
            TenantProvider.clearBaseSystemIfo();
            TenantDataSourceUtil.clearLocalTenantInfo();
            NotTenantPluginHolder.clearNotSwitchFlag();
        }
    }


    /**
     * 覆盖默认的JSON响应
     */
    @Override
    @RequestMapping
    public ResponseEntity<Map<String, Object>> error(HttpServletRequest request) {
        HttpStatus status = getStatus(request);

        if (status == HttpStatus.NOT_FOUND) {
            return new ResponseEntity<>(status);
        }
        return super.error(request);
    }

    /**
     * 权限异常
     * @param e
     * @return
     */
    @ResponseBody
    @ExceptionHandler(value = NoPermiLoginException.class)
    public ActionResult<Object> noPermiLoginException(NoPermiLoginException e) {
        ActionResult<Object> result = ActionResult.fail(ActionResultCode.PEIMISSIONEXP.getCode(), e.getMessage());
        result.setData(e.getData());
        return result;
    }

}
