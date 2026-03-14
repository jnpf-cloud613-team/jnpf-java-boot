package jnpf.onlinedev.model;

import jnpf.base.UserInfo;
import jnpf.base.model.flow.DataModel;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/12/19 17:12
 */
@Data
public class AsyncExecuteModel {
    private String modelId;
    private Integer trigger;
    private List<String> dataId;
    private List<Map<String, Object>> dataMap;
    private UserInfo userInfo;
    private DataModel dataModel;
}
