package jnpf.base.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import jnpf.base.ActionResult;
import jnpf.base.service.ComFieldsService;
import jnpf.base.vo.ListVO;
import jnpf.base.entity.ComFieldsEntity;
import jnpf.constant.GenerateConstant;
import jnpf.constant.MsgCode;
import jnpf.exception.DataException;
import jnpf.base.model.comfields.ComFieldsCrForm;
import jnpf.base.model.comfields.ComFieldsInfoVO;
import jnpf.base.model.comfields.ComFieldsListVO;
import jnpf.base.model.comfields.ComFieldsUpForm;
import jnpf.util.JsonUtil;
import jnpf.util.JsonUtilEx;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.ArrayList;
import java.util.List;

/**
 * 常用字段
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/12 15:31
 */
@Tag(name = "常用字段", description = "CommonFields")
@RestController
@RequestMapping("/api/system/CommonFields")
@RequiredArgsConstructor
public class ComFieldsController extends SuperController<ComFieldsService, ComFieldsEntity> {


    private final ComFieldsService comFieldsService;

    /**
     * 获取常用字段列表
     *
     * @return ignore
     */
    @Operation(summary = "获取常用字段列表")
    @SaCheckPermission("dataCenter.dataModel")
    @GetMapping
    public ActionResult<ListVO<ComFieldsListVO>> list() {
        List<ComFieldsEntity> data = comFieldsService.getList();
        List<ComFieldsListVO> list = JsonUtil.getJsonToList(data, ComFieldsListVO.class);
        ListVO<ComFieldsListVO> vo = new ListVO<>();
        vo.setList(list);
        return ActionResult.success(vo);
    }

    /**
     * 获取常用字段
     *
     * @param id 主键
     * @return ignore
     */
    @Operation(summary = "获取常用字段")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission("dataCenter.dataModel")
    @GetMapping("/{id}")
    public ActionResult<ComFieldsInfoVO> info(@PathVariable("id") String id) throws DataException {
        ComFieldsEntity entity = comFieldsService.getInfo(id);
        ComFieldsInfoVO vo = JsonUtilEx.getJsonToBeanEx(entity, ComFieldsInfoVO.class);
        return ActionResult.success(vo);
    }

    /**
     * 新增常用字段
     *
     * @param comFieldsCrForm 新增常用字段模型
     * @return ignore
     */
    @Operation(summary = "添加常用字段")
    @Parameter(name = "comFieldsCrForm", description = "新建模型", required = true)
    @SaCheckPermission("dataCenter.dataModel")
    @PostMapping
    public ActionResult<Object> create(@RequestBody @Valid ComFieldsCrForm comFieldsCrForm) {
        ComFieldsEntity entity = JsonUtil.getJsonToBean(comFieldsCrForm, ComFieldsEntity.class);
        List<String> javaSql = new ArrayList<>();
        javaSql.addAll(GenerateConstant.getJavaKeyword());
        javaSql.addAll(GenerateConstant.getSqlKeyword());
        if(javaSql.contains(entity.getField())){
            return ActionResult.fail(MsgCode.SYS128.get("列名" + entity.getField()));
        }
        if (comFieldsService.isExistByFullName(entity.getField(), entity.getId())) {
            return ActionResult.fail(MsgCode.EXIST001.get());
        }
        comFieldsService.create(entity);
        return ActionResult.success(MsgCode.SU001.get());
    }

    /**
     * 修改常用字段
     *
     * @param id              主键
     * @param comFieldsUpForm 修改常用字段模型
     * @return ignore
     */
    @Operation(summary = "修改常用字段")
    @Parameter(name = "id", description = "主键", required = true)
    @Parameter(name = "comFieldsUpForm", description = "修改模型", required = true)
    @SaCheckPermission("dataCenter.dataModel")
    @PutMapping("/{id}")
    public ActionResult<Object> update(@PathVariable("id") String id, @RequestBody @Valid ComFieldsUpForm comFieldsUpForm) {
        ComFieldsEntity entity = JsonUtil.getJsonToBean(comFieldsUpForm, ComFieldsEntity.class);
        List<String> javaSql = new ArrayList<>();
        javaSql.addAll(GenerateConstant.getJavaKeyword());
        javaSql.addAll(GenerateConstant.getSqlKeyword());
        if(javaSql.contains(entity.getField())){
            return ActionResult.fail(MsgCode.SYS128.get("列名" + entity.getField()));
        }
        if (comFieldsService.isExistByFullName(entity.getField(), id)) {
            return ActionResult.fail(MsgCode.EXIST001.get());
        }
        boolean flag = comFieldsService.update(id, entity);
        if (!flag) {
            return ActionResult.fail(MsgCode.FA002.get());
        }
        return ActionResult.success(MsgCode.SU004.get());
    }

    /**
     * 删除常用字段
     *
     * @param id 主键
     * @return ignore
     */
    @Operation(summary = "删除常用字段")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission("dataCenter.dataModel")
    @DeleteMapping("/{id}")
    public ActionResult<Object> delete(@PathVariable("id") String id) {
        ComFieldsEntity entity = comFieldsService.getInfo(id);
        if (entity != null) {
            comFieldsService.delete(entity);
            return ActionResult.success(MsgCode.SU003.get());
        }
        return ActionResult.fail(MsgCode.FA003.get());
    }
}

