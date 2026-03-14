package jnpf.base.model.datainterface;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author ：JNPF开发平台组
 * @version: V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date ：2022/6/2 18:09
 */
@Data
public class DataInterfaceParamModel implements Serializable {

    private String tenantId;

    private String origin;

    private List<DataInterfaceModel> paramList;

}
