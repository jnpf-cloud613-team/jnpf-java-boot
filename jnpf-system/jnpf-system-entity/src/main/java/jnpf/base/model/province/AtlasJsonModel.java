package jnpf.base.model.province;

import lombok.Data;

import java.util.List;

/**
 * 流程设计
 *
 * @author JNPF开发平台组
 * @version V3.4.2
 * @copyright 引迈信息技术有限公司
 * @date 2023/2/1 9:48:02
 */
@Data
public class AtlasJsonModel {
    private String type;
    private List<AtlasFeaturesModel> features;
}

