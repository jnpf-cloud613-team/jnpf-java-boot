package jnpf.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 第三方未绑定模型
 *
 * @author JNPF开发平台组
 * @version V3.4.2
 * @copyright 引迈信息技术有限公司
 * @date 2022/9/19 15:06:31
 */
@Data
@AllArgsConstructor
public class SocialUnbindModel {
    String socialType;
    String socialUnionid;
    String socialName;
}
