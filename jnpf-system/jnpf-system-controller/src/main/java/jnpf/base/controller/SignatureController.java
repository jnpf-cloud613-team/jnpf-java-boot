package jnpf.base.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jnpf.base.ActionResult;
import jnpf.base.entity.SignatureEntity;
import jnpf.base.model.signature.*;
import jnpf.base.service.SignatureService;
import jnpf.base.vo.ListVO;
import jnpf.base.vo.PageListVO;
import jnpf.base.vo.PaginationVO;
import jnpf.constant.MsgCode;
import jnpf.base.model.signature.SignatureListVO;
import jnpf.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.*;

/**
 * 电子签章
 *
 * @author JNPF开发平台组
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 */
@Tag(name = "电子签章", description = "Signature")
@RestController
@RequestMapping("/api/system/Signature")
@RequiredArgsConstructor
public class SignatureController extends SuperController<SignatureService, SignatureEntity> {



    private final SignatureService signatureService;

    /**
     * 获取电子签章列表
     *
     * @param signaturePage 分页模型
     * @return
     */
    @Operation(summary = "获取电子签章列表")
    @GetMapping
    public ActionResult<PageListVO<SignatureListVO>> list(PaginationSignature signaturePage) {
        List<SignatureListVO> list = signatureService.getList(signaturePage);
        PaginationVO paginationVO = JsonUtil.getJsonToBean(signaturePage, PaginationVO.class);
        return ActionResult.page(list, paginationVO);
    }

    /**
     * 获取电子签章下拉框
     *
     * @return
     */
    @Operation(summary = "获取电子签章下拉框")
    @GetMapping("/Selector")
    public ActionResult<ListVO<SignatureSelectorListVO>> selector() {
        List<SignatureEntity> list = signatureService.getList();
        List<SignatureSelectorListVO> listVOS = JsonUtil.getJsonToList(list, SignatureSelectorListVO.class);
        ListVO<SignatureSelectorListVO> vo = new ListVO<>();
        vo.setList(listVOS);
        return ActionResult.success(vo);
    }

    /**
     * 通过主键id集合获取有权限的电子签章列表
     *
     * @return
     */
    @Operation(summary = "通过主键id集合获取有权限的电子签章列表")
    @PostMapping("/ListByIds")
    public ActionResult<ListVO<SignatureSelectorListVO>> listByIds(@RequestBody SignatureListByIdsModel model) {
        List<SignatureSelectorListVO> list = signatureService.getListByIds(model);
        ListVO<SignatureSelectorListVO> vo = new ListVO<>();
        vo.setList(list);
        return ActionResult.success(vo);
    }

    /**
     * 获取电子签章信息
     *
     * @param id 主键
     * @return
     */
    @Operation(summary = "获取电子签章信息")
    @GetMapping("/{id}")
    @Parameter(name = "id", description = "主键", required = true)
    public ActionResult<SignatureInfoVO> info(@PathVariable("id") String id) {
        SignatureInfoVO vo = signatureService.getInfo(id);
        if (vo == null) {
            return ActionResult.fail(MsgCode.FA001.get());
        }
        return ActionResult.success(vo);
    }

    /**
     * 新建电子签章
     *
     * @param signatureCrForm model
     * @return
     */
    @Operation(summary = "新建电子签章")
    @PostMapping
    @Parameter(name = "signatureCrForm", description = "新建电子签章模型", required = true)
    public ActionResult<Object> create(@RequestBody @Valid SignatureCrForm signatureCrForm) {
        SignatureEntity entity = JsonUtil.getJsonToBean(signatureCrForm, SignatureEntity.class);
        if (signatureService.isExistByFullName(entity.getFullName(), entity.getId())) {
            return ActionResult.fail(MsgCode.EXIST001.get());
        }
        if (signatureService.isExistByEnCode(entity.getEnCode(), entity.getId())) {
            return ActionResult.fail(MsgCode.EXIST002.get());
        }
        signatureService.create(entity, signatureCrForm.getUserIds());
        return ActionResult.success(MsgCode.SU001.get());
    }

    /**
     * 更新电子签章
     *
     * @param id             主键
     * @param signatureUpForm 日程模型
     * @return
     */
    @Operation(summary = "更新电子签章")
    @PutMapping("/{id}")
    @Parameter(name = "id", description = "主键", required = true)
    @Parameter(name = "signatureUpForm", description = "更新电子签章模型", required = true)
    public ActionResult<Object> update(@PathVariable("id") String id, @RequestBody @Valid SignatureUpForm signatureUpForm) {
        SignatureEntity entity = signatureService.getInfoById(id);
        if (entity == null) {
            return ActionResult.fail(MsgCode.FA002.get());
        }
        if (signatureService.isExistByFullName(signatureUpForm.getFullName(), id)) {
            return ActionResult.fail(MsgCode.EXIST001.get());
        }
        if (signatureService.isExistByEnCode(signatureUpForm.getEnCode(), id)) {
            return ActionResult.fail(MsgCode.EXIST002.get());
        }
        signatureService.update(id, signatureUpForm);
        return ActionResult.success(MsgCode.SU004.get());
    }

    /**
     * 删除电子签章
     *
     * @param id 主键
     * @return
     */
    @Operation(summary = "删除电子签章")
    @DeleteMapping("/{id}")
    @Parameter(name = "id", description = "主键", required = true)
    public ActionResult<Object> delete(@PathVariable("id") String id) {
        SignatureEntity entity = signatureService.getInfoById(id);
        if (entity == null) {
            return ActionResult.fail(MsgCode.FA003.get());
        }
        signatureService.delete(id);
        return ActionResult.success(MsgCode.SU003.get());
    }

}
