package jnpf.base.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.poi.ss.formula.functions.T;
import org.mybatis.dynamic.sql.util.mybatis3.*;

/**
 * mybatis3 表单mapper对象
 *
 * @author JNPF开发平台组
 * @version V3.4.5
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2022/9/27
 */
@Mapper
public interface FlowFormDataMapper extends CommonCountMapper, CommonDeleteMapper, CommonInsertMapper<T>, CommonSelectMapper,
        CommonUpdateMapper {
}
