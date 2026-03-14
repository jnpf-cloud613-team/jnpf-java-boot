package jnpf.onlinedev.service;

import jnpf.base.service.SuperService;
import jnpf.onlinedev.entity.VisualPersonalEntity;
import jnpf.onlinedev.model.DataInfoVO;
import jnpf.onlinedev.model.personal.VisualPersonalInfo;
import jnpf.onlinedev.model.personal.VisualPersonalVo;

import java.util.List;

/**
 * 列表个性视图 服务
 *
 * @author JNPF开发平台组
 * @version v5.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/11/5 16:50:00
 */
public interface VisualPersonalService extends SuperService<VisualPersonalEntity> {

    /**
     * 查询个性视图列表
     *
     * @param menuId
     * @return
     */
    List<VisualPersonalEntity> getList(String menuId);

    /**
     * 查询个性视图列表
     *
     * @param menuId
     * @return
     */
    List<VisualPersonalVo> getListVo(String menuId);


    /**
     * 查询个性视图详情
     *
     * @param id
     * @return
     */
    VisualPersonalInfo getInfo(String id);

    /**
     * 重名检测 同菜单下不能同名
     *
     * @param fullName
     * @param id
     * @param menuId
     * @return
     */
    boolean isExistByFullName(String fullName, String id, String menuId);

    /**
     * 页面初始化获取个性化配置
     *
     * @param menuId
     * @return
     */
    void setDataInfoVO(String menuId, DataInfoVO dataInfoVO);
}
