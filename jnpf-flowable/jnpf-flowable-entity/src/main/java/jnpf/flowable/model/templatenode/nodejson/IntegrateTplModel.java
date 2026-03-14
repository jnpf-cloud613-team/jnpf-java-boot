package jnpf.flowable.model.templatenode.nodejson;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/9/9 10:47
 */
@Data
public class IntegrateTplModel {
    //远端接口
    private String field;
    private Boolean required = false;
    private Integer sourceType;
    private String relationField;

    //发送配置
    private String id;
    private String templateId;
    private String sendConfigId;
    private String msgTemplateName;
    private List<IntegrateParamModel> paramJson = new ArrayList<>();
}
