package jnpf.onlinedev.service;

import jnpf.base.UserInfo;
import jnpf.base.model.VisualDevJsonModel;
import jnpf.exception.WorkFlowException;
import jnpf.onlinedev.model.online.VisualColumnSearchVO;
import jnpf.onlinedev.model.PaginationModel;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 列表临时接口
 *
 * @author JNPF开发平台组
 * @version V3.2.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021/7/28
 */
@Service
public interface VisualDevListService {


    /**
     * 无表数据
     *
     * @param modelId
     * @return
     */
    List<Map<String, Object>> getWithoutTableData(String modelId);

    /**
     * 有表查询
     *
     * @param visualDevJsonModel
     * @param paginationModel
     * @return
     */
    List<Map<String, Object>> getListWithTable(VisualDevJsonModel visualDevJsonModel, PaginationModel paginationModel, UserInfo userInfo, List<String> columnPropList);

    /**
     * 列表数据
     *
     * @param visualDevJsonModel
     * @param paginationModel
     * @return
     */
    List<Map<String, Object>> getDataList(VisualDevJsonModel visualDevJsonModel, PaginationModel paginationModel) throws WorkFlowException;

    /**
     * 外链列表数据
     *
     * @param visualDevJsonModel
     * @param paginationModel
     * @return
     */
    List<Map<String, Object>> getDataListLink(VisualDevJsonModel visualDevJsonModel, PaginationModel paginationModel) throws WorkFlowException;

    /**
     * 关联表单列表数据
     *
     * @param visualDevJsonModel
     * @param paginationModel
     * @return
     */
    List<Map<String, Object>> getRelationFormList(VisualDevJsonModel visualDevJsonModel, PaginationModel paginationModel);

    /**
     * 列表数据
     */
    List<Map<String,Object>> getListWithTableList(VisualDevJsonModel visualDevJsonModel, PaginationModel paginationModel, UserInfo userInfo);
}
