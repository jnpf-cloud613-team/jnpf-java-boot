package jnpf.base.service;

import jnpf.base.Pagination;
import jnpf.message.model.UserOnlineModel;

import java.util.List;

/**
 * 在线用户
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月26日 上午9:18
 */
public interface UserOnlineService {

    /**
     * 列表
     *
     * @param page 分页参数
     * @return ignore
     */
    List<UserOnlineModel> getList(Pagination page);

    /**
     * 删除
     *
     * @param tokens 主键值
     */
    void delete(String... tokens);
}
