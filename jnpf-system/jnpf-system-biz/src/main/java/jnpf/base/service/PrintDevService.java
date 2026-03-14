package jnpf.base.service;

import jnpf.base.entity.OperatorRecordEntity;
import jnpf.base.entity.PrintDevEntity;
import jnpf.base.model.export.PrintExportVo;
import jnpf.base.model.print.*;
import jnpf.base.model.vo.PrintDevVO;

import java.util.List;
import java.util.Map;

/**
 * 打印模板-服务类
 *
 * @author JNPF开发平台组 YY
 * @version V3.2.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月30日
 */
public interface PrintDevService extends SuperService<PrintDevEntity> {

    /**
     * 列表
     *
     * @return 打印实体类
     */
    List<PrintDevEntity> getList(PaginationPrint paginationPrint);

    /**
     * 根据id列表获取数据
     *
     * @param idList
     * @return
     */
    List<PrintDevEntity> getListByIds(List<String> idList);

    /**
     * 获取用户创建的所有模板
     *
     * @param creUser
     * @return
     */
    List<PrintDevEntity> getListByCreUser(String creUser);

    /**
     * 创建
     */
    void create(PrintDevFormDTO dto);

    /**
     * 获取详情
     *
     * @return PrintDevInfoVO
     */
    PrintDevInfoVO getVersionInfo(String versionId);

    /**
     * 保存或者发布 通过type：0-保存，1-发布
     *
     * @param form
     */
    void saveOrRelease(PrintDevUpForm form);

    /**
     * 获取打印模板对象树形模型(selector)
     *
     * @param category 打印模板类型
     * @return 打印模型树
     * @throws Exception 字典分类不存在BUG
     */
    List<PrintDevVO> getTreeModel(String category);

    /**
     * 新增更新校验
     *
     * @param printDevEntity 打印模板对象
     * @param fullNameCheck  重名校验开关
     * @param encodeCheck    重码校验开关
     */
    void creUpdateCheck(PrintDevEntity printDevEntity, Boolean fullNameCheck, Boolean encodeCheck);

    Boolean isExistByEnCode(String enCode, String id);

    /**
     * 查询打印列表
     *
     * @param ids
     * @return
     */
    List<PrintOption> getPrintTemplateOptions(List<String> ids);

    /**
     * 导入打印模板
     *
     * @param infoVO
     * @param type
     * @return
     */
    String importData(PrintExportVo infoVO, Integer type);

    /**
     * 获取流程经办记录集合
     *
     * @param taskId 任务ID
     * @return 经办记录集合
     */
    List<OperatorRecordEntity> getFlowTaskOperatorRecordList(String taskId);

    /**
     * 根据sql获取数据
     *
     * @param templateId 打印模板id
     * @param formId     表达数据id
     * @param params     参数
     * @return
     */
    Map<String, Object> getDataMap(String templateId, String formId, String flwoTaskId, Map<String, Object> params);

    /**
     * 复制打印模板
     *
     * @param templateId 打印模板id
     * @return
     */
    void copyPrintdev(String templateId);

    /**
     * 获取打印业务列表
     *
     * @param pagination
     * @return
     */
    List<PrintDevEntity> getWorkSelector(PaginationPrint pagination);

    /**
     * 获取打印业务列表
     *
     * @return 打印模型树
     */
    List<PrintDevEntity> getWorkSelector(List<String> id);

    List<PrintExportVo> getExportList(String systemId);

    /**
     * 应用-导入数据
     *
     * @param list
     * @param systemId
     * @return
     */
    boolean importCopy(List<PrintExportVo> list, String systemId);

    void deleteBySystemId(String systemId);
}
