package jnpf.service;

import jnpf.base.ActionResult;
import jnpf.exception.LoginException;
import jnpf.model.LoginVO;
import jnpf.model.logout.LogoutResultModel;

import java.util.Map;

public interface AuthService {
    ActionResult<LoginVO> login(Map<String, String> parameters) throws LoginException;

    ActionResult<Object> kickoutByToken(String... tokens);

    ActionResult<Object> kickoutByUserId(String userId, String tenantId);

    ActionResult<LogoutResultModel> logout(String grandtype);
}
