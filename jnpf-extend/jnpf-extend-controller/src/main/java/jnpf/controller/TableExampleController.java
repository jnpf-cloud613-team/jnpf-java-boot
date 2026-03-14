package jnpf.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jnpf.base.ActionResult;
import jnpf.base.Page;
import jnpf.base.UserInfo;
import jnpf.base.controller.SuperController;
import jnpf.constant.MsgCode;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.service.UserService;
import jnpf.base.vo.PageListVO;
import jnpf.base.vo.PaginationVO;
import jnpf.base.service.DictionaryDataService;
import jnpf.base.service.ProvinceService;
import jnpf.base.vo.ListVO;
import jnpf.entity.TableExampleEntity;
import jnpf.base.entity.DictionaryDataEntity;
import jnpf.base.entity.ProvinceEntity;
import jnpf.exception.DataException;
import jnpf.model.tableexample.*;
import jnpf.model.tableexample.postil.PostilInfoVO;
import jnpf.model.tableexample.postil.PostilModel;
import jnpf.model.tableexample.postil.PostilSendForm;
import jnpf.service.TableExampleService;
import jnpf.util.DateUtil;
import jnpf.util.JsonUtil;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;
import jnpf.util.treeutil.SumTree;
import jnpf.util.treeutil.TreeDotUtils;
import jnpf.util.type.StringNumber;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 表格示例数据
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2019年9月26日 上午9:18
 */
@Tag(name = "表格示例数据", description = "TableExample")
@RestController
@RequestMapping("/api/extend/TableExample")
@RequiredArgsConstructor
public class TableExampleController extends SuperController<TableExampleService, TableExampleEntity> {


    private final TableExampleService tableExampleService;

    private final ProvinceService provinceService;

    private final DictionaryDataService dictionaryDataService;
    

    private final UserService userService;

    /**
     * 列表
     *
     * @param paginationTableExample 分页模型
     * @return
     */
    @Operation(summary = "获取表格数据列表")
    @GetMapping
    @SaCheckPermission("extend.tableDemo.commonTable")
    public ActionResult<PageListVO<TableExampleListVO>> list(PaginationTableExample paginationTableExample) {
        List<TableExampleEntity> data = tableExampleService.getList(paginationTableExample);
        List<TableExampleListVO> list = JsonUtil.getJsonToList(data, TableExampleListVO.class);
        List<String> userId = list.stream().map(TableExampleListVO::getRegistrant).collect(Collectors.toList());
        List<UserEntity> userList = userService.getUserName(userId);
        for (TableExampleListVO tableExampleListVO : list) {
            UserEntity user = userList.stream().filter(t -> t.getId().equals(tableExampleListVO.getRegistrant())).findFirst().orElse(null);
            tableExampleListVO.setRegistrant(user != null ? user.getRealName() + "/" + user.getAccount() : "");
        }
        PaginationVO paginationVO = JsonUtil.getJsonToBean(paginationTableExample, PaginationVO.class);
        return ActionResult.page(list, paginationVO);
    }

    /**
     * 列表（树形表格）
     *
     * @param typeId                 主键
     * @param paginationTableExample 查询模型
     * @return
     */
    @Operation(summary = "（树形表格）")
    @GetMapping("/ControlSample/{typeId}")
    @Parameter(name = "typeId", description = "主键", required = true)
    @SaCheckPermission("extend.tableDemo.tableTree")
    public ActionResult<PageListVO<TableExampleListVO>> list(@PathVariable("typeId") String typeId, PaginationTableExample paginationTableExample) {
        List<TableExampleEntity> data = tableExampleService.getList(typeId, paginationTableExample);
        List<TableExampleListVO> list = JsonUtil.getJsonToList(data, TableExampleListVO.class);
        List<String> userId = list.stream().map(TableExampleListVO::getRegistrant).collect(Collectors.toList());
        List<UserEntity> userList = userService.getUserName(userId);
        for (TableExampleListVO tableExampleListVO : list) {
            UserEntity user = userList.stream().filter(t -> t.getId().equals(tableExampleListVO.getRegistrant())).findFirst().orElse(null);
            tableExampleListVO.setRegistrant(user != null ? user.getRealName() + "/" + user.getAccount() : "");
        }
        PaginationVO paginationVO = JsonUtil.getJsonToBean(paginationTableExample, PaginationVO.class);
        return ActionResult.page(list, paginationVO);
    }

    /**
     * 列表
     *
     * @return
     */
    @Operation(summary = "获取表格分组列表")
    @GetMapping("/All")
    @SaCheckPermission("extend.tableDemo.groupingTable")
    public ActionResult<ListVO<TableExampleListAllVO>> listAll() {
        List<TableExampleEntity> data = tableExampleService.getList();
        List<TableExampleListAllVO> list = JsonUtil.getJsonToList(data, TableExampleListAllVO.class);
        List<String> userId = list.stream().map(TableExampleListAllVO::getRegistrant).collect(Collectors.toList());
        List<UserEntity> userList = userService.getUserName(userId);
        for (TableExampleListAllVO tableExampleListVO : list) {
            UserEntity user = userList.stream().filter(t -> t.getId().equals(tableExampleListVO.getRegistrant())).findFirst().orElse(null);
            tableExampleListVO.setRegistrant(user != null ? user.getRealName() + "/" + user.getAccount() : "");
        }
        ListVO<TableExampleListAllVO> vo = new ListVO<>();
        vo.setList(list);
        return ActionResult.success(vo);
    }

    /**
     * 列表
     *
     * @param page 查询模型
     * @return
     */
    @Operation(summary = "获取延伸扩展列表(行政区划)")
    @GetMapping("/IndustryList")
    @SaCheckPermission("extend.tableDemo.extension")
    public ActionResult<ListVO<TableExampleIndustryListVO>> industryList(Page page) {
        String keyword = page.getKeyword();
        List<ProvinceEntity> data = provinceService.getList("-1");
        if (!StringUtil.isEmpty(keyword)) {
            data = data.stream().filter(t -> t.getFullName().contains(keyword)).collect(Collectors.toList());
        }
        List<TableExampleIndustryListVO> listVos = JsonUtil.getJsonToList(data, TableExampleIndustryListVO.class);
        ListVO<TableExampleIndustryListVO> vo = new ListVO<>();
        vo.setList(listVos);
        return ActionResult.success(vo);
    }

    /**
     * 列表
     *
     * @param id 主键值
     * @return
     */
    @Operation(summary = "获取城市信息列表(获取延伸扩展列表(行政区划))")
    @GetMapping("/CityList/{id}")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission("extend.tableDemo.extension")
    public ActionResult<ListVO<TableExampleCityListVO>> cityList(@PathVariable("id") String id) {
        List<ProvinceEntity> data = provinceService.getList(id);
        List<TableExampleCityListVO> listVos = JsonUtil.getJsonToList(data, TableExampleCityListVO.class);
        ListVO<TableExampleCityListVO> vo = new ListVO<>();
        vo.setList(listVos);
        return ActionResult.success(vo);
    }

    /**
     * 列表（表格树形）
     *
     * @param isTree 类型
     * @return
     */
    @Operation(summary = "表格树形")
    @GetMapping("/ControlSample/TreeList")
    @Parameter(name = "isTree", description = "类型")
    @SaCheckPermission("extend.tableDemo.tableTree")
    public ActionResult<ListVO<TableExampleTreeModel>> treeList(@RequestParam("isTree")String isTree) {
        List<DictionaryDataEntity> data = dictionaryDataService.getList("d59a3cf65f9847dbb08be449e3feae16");
        List<TableExampleTreeModel> treeList = new ArrayList<>();
        for (DictionaryDataEntity entity : data) {
            TableExampleTreeModel treeModel = new TableExampleTreeModel();
            treeModel.setId(entity.getId());
            treeModel.setText(entity.getFullName());
            treeModel.setParentId(entity.getParentId());
            treeModel.setLoaded(true);
            treeModel.setExpanded(true);
            treeModel.setHt(JsonUtil.entityToMap(entity));
            treeList.add(treeModel);
        }
        if (StringNumber.ONE.equals(isTree)) {
            List<SumTree<TableExampleTreeModel>> trees = TreeDotUtils.convertListToTreeDot(treeList);
            List<TableExampleTreeModel> listVO = JsonUtil.getJsonToList(trees, TableExampleTreeModel.class);
            ListVO<TableExampleTreeModel> vo = new ListVO<>();
            vo.setList(listVO);
            return ActionResult.success(vo);
        }
        ListVO<TableExampleTreeModel> vo = new ListVO<>();
        vo.setList(treeList);
        return ActionResult.success(vo);
    }

    /**
     * 信息
     *
     * @param id 主键
     * @return
     */
    @Operation(summary = "获取普通表格示例信息")
    @GetMapping("/{id}")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission("extend.tableDemo.extension")
    public ActionResult<TableExampleInfoVO> info(@PathVariable("id") String id) throws DataException {
        TableExampleEntity entity = tableExampleService.getInfo(id);
        TableExampleInfoVO vo = JsonUtil.getJsonToBeanEx(entity, TableExampleInfoVO.class);
        return ActionResult.success(vo);
    }

    /**
     * 删除
     *
     * @param id 主键
     * @return
     */
    @Operation(summary = "删除项目")
    @DeleteMapping("/{id}")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission("extend.tableDemo.extension")
    public ActionResult<Object>delete(@PathVariable("id") String id) {
        TableExampleEntity entity = tableExampleService.getInfo(id);
        if (entity != null) {
            tableExampleService.delete(entity);
            return ActionResult.success(MsgCode.SU003.get());
        }
        return ActionResult.fail(MsgCode.FA003.get());
    }

    /**
     * 创建
     *
     * @param tableExampleCrForm 项目模型
     * @return
     */
    @Operation(summary = "新建项目")
    @PostMapping
    @Parameter(name = "tableExampleCrForm", description = "项目模型",required = true)
    @SaCheckPermission("extend.tableDemo.extension")
    public ActionResult<Object>create(@RequestBody @Valid TableExampleCrForm tableExampleCrForm) {
        TableExampleEntity entity = JsonUtil.getJsonToBean(tableExampleCrForm, TableExampleEntity.class);
        entity.setCostAmount(entity.getCostAmount() == null ? new BigDecimal("0") : entity.getCostAmount());
        entity.setTunesAmount(entity.getTunesAmount() == null ? new BigDecimal("0") : entity.getTunesAmount());
        entity.setProjectedIncome(entity.getProjectedIncome() == null ? new BigDecimal("0") : entity.getProjectedIncome());
        entity.setSign("0000000");
        tableExampleService.create(entity);
        return ActionResult.success(MsgCode.SU001.get());
    }

    /**
     * 更新
     *
     * @param id                 主键
     * @param tableExampleUpForm 项目模型
     * @return
     */
    @Operation(summary = "更新项目")
    @PutMapping("/{id}")
    @Parameter(name = "tableExampleUpForm", description = "项目模型",required = true)
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission("extend.tableDemo.postilTable")
    public ActionResult<Object>update(@PathVariable("id") String id, @RequestBody @Valid TableExampleUpForm tableExampleUpForm) {
        TableExampleEntity entity = JsonUtil.getJsonToBean(tableExampleUpForm, TableExampleEntity.class);
        entity.setCostAmount(entity.getCostAmount() == null ? new BigDecimal("0") : entity.getCostAmount());
        entity.setTunesAmount(entity.getTunesAmount() == null ? new BigDecimal("0") : entity.getTunesAmount());
        entity.setProjectedIncome(entity.getProjectedIncome() == null ? new BigDecimal("0") : entity.getProjectedIncome());
        boolean flag = tableExampleService.update(id, entity);
        if (!flag) {
            return ActionResult.fail(MsgCode.FA002.get());
        }
        return ActionResult.success(MsgCode.SU004.get());
    }

    /**
     * 更新标签
     *
     * @param id                     主键
     * @param tableExampleSignUpForm 项目模型
     * @return
     */
    @Operation(summary = "更新标记")
    @PutMapping("/UpdateSign/{id}")
    @Parameter(name = "tableExampleSignUpForm", description = "项目模型",required = true)
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission("extend.tableDemo.postilTable")
    public ActionResult<Object>updateSign(@PathVariable("id") String id, @RequestBody @Valid TableExampleSignUpForm tableExampleSignUpForm) {
        TableExampleEntity entity = JsonUtil.getJsonToBean(tableExampleSignUpForm, TableExampleEntity.class);
        TableExampleEntity tableExampleEntity = tableExampleService.getInfo(id);
        if (tableExampleEntity == null) {
            return ActionResult.success(MsgCode.FA002.get());
        }
        tableExampleEntity.setSign(entity.getSign());
        tableExampleService.update(id, entity);
        return ActionResult.success(MsgCode.SU004.get());
    }

    /**
     * 行编辑
     *
     * @param tableExampleRowUpForm 项目模型
     * @param id                    主键
     * @return
     */
    @Operation(summary = "行编辑")
    @PutMapping("/{id}/Actions/RowsEdit")
    @Parameter(name = "tableExampleRowUpForm", description = "项目模型",required = true)
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission("extend.tableDemo.redactTable")
    public ActionResult<Object>rowEditing(@PathVariable("id") String id, @RequestBody @Valid TableExampleRowUpForm tableExampleRowUpForm) {
        TableExampleEntity entity = JsonUtil.getJsonToBean(tableExampleRowUpForm, TableExampleEntity.class);
        entity.setCostAmount(entity.getCostAmount() == null ? new BigDecimal("0") : entity.getCostAmount());
        entity.setTunesAmount(entity.getTunesAmount() == null ? new BigDecimal("0") : entity.getTunesAmount());
        entity.setProjectedIncome(entity.getProjectedIncome() == null ? new BigDecimal("0") : entity.getProjectedIncome());
        entity.setId(id);
        boolean falg = tableExampleService.rowEditing(entity);
        if (!falg) {
            return ActionResult.fail(MsgCode.FA002.get());
        }
        return ActionResult.success(MsgCode.SU004.get());
    }

    /**
     * 发送
     *
     * @param postilSendForm 项目模型
     * @param id             主键
     * @return
     */
    @Operation(summary = "发送批注")
    @PostMapping("/{id}/Postil")
    @Parameter(name = "postilSendForm", description = "项目模型",required = true)
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission("extend.tableDemo.postilTable")
    public ActionResult<Object>sendPostil(@PathVariable("id") String id, @RequestBody PostilSendForm postilSendForm) {
        TableExampleEntity tableExampleEntity = tableExampleService.getInfo(id);
        if (tableExampleEntity == null) {
            return ActionResult.success(MsgCode.FA005.get());
        }
        UserInfo userInfo = UserProvider.getUser();
        PostilModel model = new PostilModel();
        model.setCreatorTime(DateUtil.getNow("+8"));
        model.setText(postilSendForm.getText());
        model.setUserId(userInfo != null ? userInfo.getUserName() + "/" + userInfo.getUserAccount() : "");
        List<PostilModel> list = new ArrayList<>();
        list.add(model);
        if (!StringUtil.isEmpty(tableExampleEntity.getPostilJson())) {
            list.addAll(JsonUtil.getJsonToList(tableExampleEntity.getPostilJson(), PostilModel.class));
        }

        String postilJson = JsonUtil.getObjectToString(list);
        tableExampleEntity.setPostilJson(postilJson);
        tableExampleEntity.setPostilCount(list.size());
        tableExampleService.update(id, tableExampleEntity);
        return ActionResult.success(MsgCode.SU012.get());
    }


    /**
     * 发送
     *
     * @param id 主键值
     * @return
     */
    @Operation(summary = "获取批注")
    @Parameter(name = "id", description = "主键", required = true)
    @GetMapping("/{id}/Actions/Postil")
    @SaCheckPermission("extend.tableDemo.postilTable")
    public ActionResult<PostilInfoVO> getPostil(@PathVariable("id") String id) {
        TableExampleEntity tableExampleEntity = tableExampleService.getInfo(id);
        if (tableExampleEntity == null) {
            return ActionResult.success(MsgCode.FA012.get());
        }
        PostilInfoVO vo = new PostilInfoVO();
        vo.setPostilJson(tableExampleEntity.getPostilJson());
        return ActionResult.success(vo);
    }

    /**
     * 删除批注
     *
     * @param id    主键值
     * @param index 行数
     * @return
     */
    @Operation(summary = "删除批注")
    @DeleteMapping("/{id}/Postil/{index}")
    @Parameter(name = "id", description = "主键", required = true)
    @Parameter(name = "index", description = "行数", required = true)
    @SaCheckPermission("extend.tableDemo.postilTable")
    public ActionResult<Object>deletePostil(@PathVariable("id") String id, @PathVariable("index") int index) {
        TableExampleEntity tableExampleEntity = tableExampleService.getInfo(id);
        if (tableExampleEntity == null) {
            return ActionResult.success(MsgCode.FA003.get());
        }
        List<PostilModel> list = JsonUtil.getJsonToList(tableExampleEntity.getPostilJson(), PostilModel.class);
        list.remove(index);
        String postilJson = JsonUtil.getObjectToString(list);
        tableExampleEntity.setPostilJson(postilJson);
        tableExampleEntity.setPostilCount((list.size()));
        tableExampleService.update(id, tableExampleEntity);
        return ActionResult.success(MsgCode.SU003.get());
    }
}
