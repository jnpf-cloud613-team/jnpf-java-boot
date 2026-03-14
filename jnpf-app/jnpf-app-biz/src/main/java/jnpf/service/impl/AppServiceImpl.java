package jnpf.service.impl;

import jnpf.model.AppUserInfoVO;
import jnpf.permission.entity.OrganizeEntity;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.service.OrganizeService;
import jnpf.permission.service.PositionService;
import jnpf.permission.service.UserService;
import jnpf.service.AppService;
import jnpf.util.JsonUtil;
import jnpf.util.StringUtil;
import jnpf.util.UploaderUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * app用户信息
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021-08-08
 */
@Service
@RequiredArgsConstructor
public class AppServiceImpl implements AppService {


    private final UserService userService;

    private final PositionService positionService;

    private final OrganizeService organizeService;


    @Override
    public AppUserInfoVO getInfo(String id) {
        AppUserInfoVO userInfoVO = new AppUserInfoVO();
        UserEntity entity = userService.getInfo(id);
        if (entity != null) {
            userInfoVO = JsonUtil.getJsonToBean(entity, AppUserInfoVO.class);
            List<String> positionIds = StringUtil.isNotEmpty(entity.getPositionId()) ? Arrays.asList(entity.getPositionId().split(",")) : new ArrayList<>();
            List<String> positionName = positionService.getPositionName(positionIds, false).stream().map(t -> t.getFullName()).collect(Collectors.toList());
            userInfoVO.setPositionName(String.join(",", positionName));
            OrganizeEntity info = organizeService.getInfo(entity.getOrganizeId());
            userInfoVO.setOrganizeName(info != null ? info.getFullName() : "");
            userInfoVO.setHeadIcon(UploaderUtil.uploaderImg(userInfoVO.getHeadIcon()));
        }
        return userInfoVO;
    }
}
