package jnpf.model;

import jnpf.base.UserInfo;
import jnpf.permission.entity.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BuildUserCommonInfoModel implements Serializable {

    private static final long serialVersionUID = 1L;

    private UserInfo userInfo;
    private UserEntity userEntity;
    private BaseSystemInfo baseSystemInfo;
    private String type;

}
