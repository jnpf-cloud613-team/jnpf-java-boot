package jnpf.base.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import jnpf.base.ActionResult;
import jnpf.base.UserInfo;
import jnpf.base.entity.AdvancedQueryEntity;
import jnpf.base.model.advancedquery.AdvancedQueryListVO;
import jnpf.base.model.advancedquery.AdvancedQuerySchemeForm;
import jnpf.base.service.AdvancedQueryService;
import jnpf.base.vo.ListVO;
import jnpf.constant.MsgCode;
import jnpf.exception.DataException;
import jnpf.util.JsonUtil;
import jnpf.util.JsonUtilEx;
import jnpf.util.UserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 高级查询方案管理
 *
 * @author JNPF开发平台组
 * @version V3.4.2
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date  2022/5/30
 */
@Tag(name = "高级查询方案管理", description = "AdvancedQuery")
@RestController
@RequestMapping("/api/system/AdvancedQuery")
@RequiredArgsConstructor
public class AdvancedQueryController extends SuperController<AdvancedQueryService, AdvancedQueryEntity> {


	private final AdvancedQueryService queryService;

	/**
	 * 新建
	 *
	 * @param advancedQuerySchemeForm 实体对象
	 * @return
	 */
	@Operation(summary = "新建方案")
	@Parameter(name = "advancedQuerySchemeForm", description = "实体对象", required = true)
	@PostMapping
	public ActionResult<Object> create(@RequestBody @Valid AdvancedQuerySchemeForm advancedQuerySchemeForm) {
		AdvancedQueryEntity entity = JsonUtil.getJsonToBean(advancedQuerySchemeForm, AdvancedQueryEntity.class);
		queryService.create(entity);
		return ActionResult.success(MsgCode.SU001.get());
	}

	/**
	 * 修改方案
	 *
	 * @param id 主键
	 * @param advancedQuerySchemeForm 实体对象
	 * @return
	 */
	@Operation(summary = "修改方案")
	@Parameter(name = "id", description = "主键", required = true)
	@Parameter(name = "advancedQuerySchemeForm", description = "实体对象", required = true)
	@PutMapping("/{id}")
	public ActionResult<Object> update(@PathVariable("id") String id, @RequestBody @Valid AdvancedQuerySchemeForm advancedQuerySchemeForm) {
		AdvancedQueryEntity entity = JsonUtil.getJsonToBean(advancedQuerySchemeForm, AdvancedQueryEntity.class);
		entity.setId(id);
		queryService.updateById(entity);
		return ActionResult.success(MsgCode.SU004.get());
	}

	/**
	 * 删除
	 *
	 * @param id 主键值
	 * @return ignore
	 */
	@Operation(summary = "删除方案")
	@Parameter(name = "id", description = "主键", required = true)
	@DeleteMapping("/{id}")
	public ActionResult<Object> delete(@PathVariable("id") String id) {
		UserInfo userInfo = UserProvider.getUser();
		AdvancedQueryEntity entity = queryService.getInfo(id,userInfo.getUserId());
		if (entity != null) {
			queryService.removeById(entity);
			return ActionResult.success(MsgCode.SU003.get());
		}
		return ActionResult.fail(MsgCode.FA003.get());
	}

	/**
	 * 列表
	 *
	 * @param moduleId 功能主键
	 * @return ignore
	 */
	@Operation(summary = "方案列表")
	@Parameter(name = "moduleId", description = "功能主键", required = true)
	@GetMapping("/{moduleId}/List")
	public ActionResult<ListVO<AdvancedQueryListVO>> list(@PathVariable("moduleId") String moduleId) {
		UserInfo userInfo = UserProvider.getUser();
		List<AdvancedQueryEntity> data = queryService.getList(moduleId,userInfo);
		List<AdvancedQueryListVO> list = JsonUtil.getJsonToList(data, AdvancedQueryListVO.class);
		ListVO<AdvancedQueryListVO> vo = new ListVO<>();
		vo.setList(list);
		return ActionResult.success(vo);
	}
	/**
	 * 信息
	 *
	 * @param id 主键值
	 * @return ignore
	 * @throws DataException ignore
	 */
	@Operation(summary = "获取方案信息")
	@Parameter(name = "id", description = "主键值", required = true)
	@GetMapping("/{id}")
	public ActionResult<AdvancedQuerySchemeForm> info(@PathVariable("id") String id) throws DataException {
		UserInfo userInfo = UserProvider.getUser();
		AdvancedQueryEntity entity = queryService.getInfo(id,userInfo.getUserId());
		AdvancedQuerySchemeForm vo = JsonUtilEx.getJsonToBeanEx(entity, AdvancedQuerySchemeForm.class);
		return ActionResult.success(vo);
	}

}
