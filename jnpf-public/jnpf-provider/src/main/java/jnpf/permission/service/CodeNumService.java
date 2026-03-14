package jnpf.permission.service;

import jnpf.base.service.SuperService;
import jnpf.permission.entity.CodeNumEntity;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * 编码序号
 *
 * @author JNPF开发平台组
 * @version v6.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2025/2/28 11:14:52
 */
public interface CodeNumService extends SuperService<CodeNumEntity> {

    /**
     * 根据类型获取数据
     *
     * @param type
     * @return
     */
    Integer getNumByType(String type, Integer times);

    /**
     * 获取多次编码
     *
     * @param type
     * @param num
     * @return
     */
    List<String> getCode(String type, Integer num);

    /**
     * 获取一次编码
     *
     * @param type
     * @return
     */
    String getCodeOnce(String type);

    /**
     * 函数式获取编码
     *
     * @param getCode   获取编码
     * @param existCode 判断编码是否存在
     * @return
     */
    String getCodeFunction(Supplier<String> getCode, Predicate<String> existCode);
}
