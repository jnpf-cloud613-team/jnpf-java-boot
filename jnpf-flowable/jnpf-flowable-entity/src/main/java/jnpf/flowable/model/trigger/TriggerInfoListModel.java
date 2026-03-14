package jnpf.flowable.model.trigger;

import jnpf.flowable.entity.TriggerRecordEntity;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/9/21 11:45
 */
@Data
public class TriggerInfoListModel {
    private String id;
    private Long startTime;
    private Integer isAsync = 1;
    private List<TriggerRecordEntity> recordList = new ArrayList<>();
}
