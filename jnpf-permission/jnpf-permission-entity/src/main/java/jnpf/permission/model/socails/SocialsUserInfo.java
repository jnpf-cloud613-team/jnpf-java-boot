package jnpf.permission.model.socails;

import com.alibaba.fastjson.JSONArray;
import jnpf.base.UserInfo;
import lombok.Data;

/**
 * 流程设计
 *
 * @author JNPF开发平台组
 * @version V3.4.2
 * @copyright 引迈信息技术有限公司
 * @date 2022/9/8 11:33:59
 */
@Data
public class SocialsUserInfo {
    UserInfo userInfo;
    JSONArray tenantUserInfo;
    String socialUnionid;
    String socialName;
}
