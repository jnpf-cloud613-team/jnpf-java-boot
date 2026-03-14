package jnpf.flowable.model.time;

import jnpf.flowable.entity.OperatorEntity;
import jnpf.flowable.model.task.FlowModel;
import jnpf.util.RedisUtil;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/7/19 16:20
 */
@Data
public class TimeModel {
    private List<OperatorEntity> operatorList = new ArrayList<>();
    private FlowModel flowModel = new FlowModel();
    private RedisUtil redisUtil;
}
