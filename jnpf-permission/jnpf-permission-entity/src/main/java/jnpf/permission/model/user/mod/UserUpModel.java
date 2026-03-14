package jnpf.permission.model.user.mod;

import jnpf.permission.entity.UserEntity;
import lombok.Data;

/**
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021-11-23
 */
@Data
public class UserUpModel {

    private Integer num;

    private UserEntity entity;

    public UserUpModel() {
    }

    public UserUpModel(Integer num, UserEntity entity) {
        this.num = num;
        this.entity = entity;
    }
}
