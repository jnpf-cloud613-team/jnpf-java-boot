package jnpf.flowable.model.templatenode.nodejson;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/12/4 14:25
 */
@Data
public class AuxiliaryInfoConfig {
    private Integer on = 0;

    private String content;

    private List<Object> linkList = new ArrayList<>();
    private List<Object> dataList = new ArrayList<>();
    private List<Map<String, Object>> fileList = new ArrayList<>();

    /**
     * 数据读取范围，近七天、近一个月、近半年、近一年、全部
     */
    private Integer dataRange = 0;
}
