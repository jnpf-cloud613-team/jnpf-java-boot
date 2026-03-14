package jnpf.base.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jnpf.base.ActionResult;
import jnpf.base.entity.DataInterfaceEntity;
import jnpf.base.entity.DataInterfaceLogEntity;
import jnpf.base.entity.DataInterfaceUserEntity;
import jnpf.base.entity.InterfaceOauthEntity;
import jnpf.base.model.interfaceoauth.*;
import jnpf.base.model.datainterface.DataInterfaceVo;
import jnpf.base.service.DataInterfaceLogService;
import jnpf.base.service.DataInterfaceService;
import jnpf.base.service.DataInterfaceUserService;
import jnpf.base.service.InterfaceOauthService;
import jnpf.base.vo.PageListVO;
import jnpf.base.vo.PaginationVO;
import jnpf.constant.MsgCode;
import jnpf.exception.DataException;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.service.UserService;
import jnpf.util.JsonUtil;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;


/**
 * 接口认证控制器
 *
 * @author JNPF开发平台组
 * @version V3.4.2
 * @copyright 引迈信息技术有限公司
 * @date 2022/6/8
 */
@Tag(name = "接口认证", description = "interfaceoauth")
@RestController
@RequestMapping(value = "/api/system/InterfaceOauth")
@RequiredArgsConstructor
public class InterfaceOauthController extends SuperController<InterfaceOauthService, InterfaceOauthEntity> {
    
    private final DataInterfaceService dataInterfaceService;
    
    private final  DataInterfaceLogService dataInterfaceLogService;

    
    private final  InterfaceOauthService interfaceOauthService;

    
    private final  UserService userService;


    
    private final  DataInterfaceUserService dataInterfaceUserService;


    /**
     * 获取接口认证列表(分页)
     *
     * @param pagination 分页参数
     * @return ignore
     */
    @Operation(summary = "获取接口认证列表(分页)")
    @SaCheckPermission("dataCenter.interfaceOauth")
    @GetMapping
    public ActionResult<PageListVO<InterfaceIdentListVo>> getList(PaginationOauth pagination) {
        List<InterfaceOauthEntity> data = interfaceOauthService.getList(pagination);
        List<InterfaceIdentListVo> jsonToList = JsonUtil.getJsonToList(data, InterfaceIdentListVo.class);
        jsonToList.forEach(item -> {
            if (StringUtil.isNotEmpty(UserProvider.getUser().getTenantId())) {
                item.setTenantId(UserProvider.getUser().getTenantId());
            }
            if (item.getCreatorUserId() != null) {
                String creUser = userService.getInfo(item.getCreatorUserId()) != null ? userService.getInfo(item.getCreatorUserId()).getRealName() : "";
                item.setCreatorUser(creUser);
            }
        });
        PaginationVO paginationVO = JsonUtil.getJsonToBean(pagination, PaginationVO.class);
        return ActionResult.page(jsonToList, paginationVO);
    }

    /**
     * 添加接口认证
     *
     * @param interfaceIdentForm 添加接口认证模型
     * @return ignore
     */
    @Operation(summary = "添加接口认证")
    @Parameter(name = "interfaceIdentForm", description = "添加接口认证模型", required = true)
    @SaCheckPermission("dataCenter.interfaceOauth")
    @PostMapping
    public ActionResult<Object>create(@RequestBody @Valid InterfaceIdentForm interfaceIdentForm) {
        InterfaceOauthEntity entity = JsonUtil.getJsonToBean(interfaceIdentForm, InterfaceOauthEntity.class);
        if (interfaceOauthService.isExistByAppName(entity.getAppName(), entity.getId())) {
            return ActionResult.fail(MsgCode.EXIST001.get());
        }
        if (interfaceOauthService.isExistByAppId(entity.getAppId(), entity.getId())) {
            return ActionResult.fail("AppId" + MsgCode.EXIST103.get());
        }
        interfaceOauthService.create(entity);
        return ActionResult.success(MsgCode.SU001.get());
    }


    /**
     * 修改接口认证
     *
     * @param interfaceIdentForm 添加接口认证模型
     * @return ignore
     */
    @Operation(summary = "修改接口认证")
    @Parameter(name = "interfaceIdentForm", description = "添加接口认证模型", required = true)
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission("dataCenter.interfaceOauth")
    @PutMapping("/{id}")
    public ActionResult<Object>update(@RequestBody @Valid InterfaceIdentForm interfaceIdentForm, @PathVariable("id") String id) throws DataException {
        InterfaceOauthEntity entity = JsonUtil.getJsonToBean(interfaceIdentForm, InterfaceOauthEntity.class);
        if (interfaceOauthService.isExistByAppName(entity.getAppName(), id)) {
            return ActionResult.fail(MsgCode.EXIST001.get());
        }
        if (interfaceOauthService.isExistByAppId(entity.getAppId(), id)) {
            return ActionResult.fail("AppId" + MsgCode.EXIST103.get());
        }
        boolean flag = interfaceOauthService.update(entity, id);
        if (!flag) {
            return ActionResult.fail(MsgCode.FA002.get());
        }
        return ActionResult.success(MsgCode.SU004.get());
    }

    /**
     * 删除接口认证
     *
     * @param id 主键
     * @return
     */
    @Operation(summary = "删除接口")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission("dataCenter.interfaceOauth")
    @DeleteMapping("/{id}")
    public ActionResult<Object>delete(@PathVariable String id) {
        InterfaceOauthEntity entity = interfaceOauthService.getInfo(id);
        if (entity != null) {
            interfaceOauthService.delete(entity);
            return ActionResult.success(MsgCode.SU003.get());
        }
        return ActionResult.fail(MsgCode.FA003.get());
    }

    /**
     * 获取秘钥
     *
     * @return
     */
    @Operation(summary = "获取接口认证密钥")
    @SaCheckPermission("dataCenter.interfaceOauth")
    @GetMapping("/getAppSecret")
    public ActionResult<Object>getAppSecret() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return ActionResult.success(MsgCode.SU019.get(), uuid);
    }


    /**
     * 保存綁定认证接口
     *
     * @return
     */
    @Operation(summary = "保存綁定认证接口")
    @Parameter(name = "identInterfaceListModel", description = "授权接口列表模型", required = true)
    @SaCheckPermission("dataCenter.interfaceOauth")
    @PostMapping("/saveInterfaceList")
    public ActionResult<Object>getInterfaceList(@RequestBody IdentInterfaceListModel identInterfaceListModel) {
        InterfaceOauthEntity entity = new InterfaceOauthEntity();
        entity.setId(identInterfaceListModel.getInterfaceIdentId());
        entity.setDataInterfaceIds(identInterfaceListModel.getDataInterfaceIds());
        boolean b = interfaceOauthService.updateById(entity);
        if (b) {
            return ActionResult.success(MsgCode.SU002.get());
        }
        return ActionResult.success(MsgCode.FA101.get());
    }

    /**
     * 获取接口授权绑定接口列表
     *
     * @param id 主键
     * @return
     */
    @Operation(summary = "获取认证基础信息及接口列表")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission("dataCenter.interfaceOauth")
    @GetMapping("/{id}")
    public ActionResult<Object>getInterfaceList(@PathVariable("id") String id) {
        InterfaceOauthEntity entity = interfaceOauthService.getInfo(id);
        InterfaceIdentVo bean = JsonUtil.getJsonToBean(entity, InterfaceIdentVo.class);
        if (StringUtils.isNotEmpty(bean.getDataInterfaceIds())) {
            List<DataInterfaceVo> listDataInterfaceVo = new ArrayList<>();
            List<DataInterfaceEntity> list = dataInterfaceService.getList(false);
            list.forEach(item -> {
                if (bean.getDataInterfaceIds().contains(item.getId())) {
                    DataInterfaceVo dataInterfaceVo = JsonUtil.getJsonToBean(item, DataInterfaceVo.class);
                    listDataInterfaceVo.add(dataInterfaceVo);
                }
            });
            bean.setList(listDataInterfaceVo);
        }

        //添加授权用户信息
        List<InterfaceUserVo> listIuv = new ArrayList<>();
        List<DataInterfaceUserEntity> select = dataInterfaceUserService.select(id);
        for (DataInterfaceUserEntity diue : select) {
            String userId = diue.getUserId();
            UserEntity info = userService.getInfo(userId);
            InterfaceUserVo iuv = new InterfaceUserVo();
            iuv.setUserId(userId);
            iuv.setUserKey(diue.getUserKey());
            iuv.setUserName(info.getRealName() + "/" + info.getAccount());
            listIuv.add(iuv);
        }
        bean.setUserList(listIuv);
        return ActionResult.success(MsgCode.SU019.get(), bean);
    }

    /**
     * 获取日志列表
     *
     * @param id                    主键
     * @param paginationIntrfaceLog 分页参数
     * @return
     */
    @Operation(summary = "获取日志列表")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission("dataCenter.interfaceOauth")
    @GetMapping("/dataInterfaceLog/{id}")
    public ActionResult<PageListVO<IdentDataInterfaceLogVO>> getInterfaceList(@PathVariable("id") String id, PaginationIntrfaceLog paginationIntrfaceLog) {
        InterfaceOauthEntity entity = interfaceOauthService.getInfo(id);
        List<IdentDataInterfaceLogVO> voList = null;
        PaginationVO vo = null;
        if (entity != null && StringUtils.isNotEmpty(entity.getDataInterfaceIds())) {
            String dataInterfaceIds = entity.getDataInterfaceIds();
            String[] split = dataInterfaceIds.split(",");
            List<String> list = Arrays.asList(split);
            List<DataInterfaceLogEntity> listByIds = dataInterfaceLogService.getListByIds(entity.getAppId(), list, paginationIntrfaceLog);
            voList = JsonUtil.getJsonToList(listByIds, IdentDataInterfaceLogVO.class);
            List<DataInterfaceEntity> listDataInt = dataInterfaceService.getList(false);
            for (IdentDataInterfaceLogVO invo : voList) {
                if (StringUtil.isNotEmpty(UserProvider.getUser().getTenantId())) {
                    invo.setTenantId(UserProvider.getUser().getTenantId());
                }
                //绑定用户
                UserEntity userEntity = userService.getInfo(invo.getUserId());
                if (userEntity != null) {
                    invo.setUserId(userEntity.getRealName() + "/" + userEntity.getAccount());
                }
                //绑定接口基础数据
                listDataInt.forEach(item -> {
                    if (invo.getInvokId().contains(item.getId())) {
                        DataInterfaceVo dataInterfaceVo = JsonUtil.getJsonToBean(item, DataInterfaceVo.class);
                        invo.setFullName(dataInterfaceVo.getFullName());
                        invo.setEnCode(dataInterfaceVo.getEnCode());
                    }
                });
            }
            vo = JsonUtil.getJsonToBean(paginationIntrfaceLog, PaginationVO.class);

        }
        return ActionResult.page(voList, vo);
    }


    @Operation(summary = "授权用户")
    @SaCheckPermission("dataCenter.interfaceOauth")
    @PostMapping("/SaveUserList")
    public ActionResult<Object>saveUserList(@RequestBody InterfaceUserForm interfaceUserForm) {
        dataInterfaceUserService.saveUserList(interfaceUserForm);
        return ActionResult.success(MsgCode.SU002.get());
    }

}
