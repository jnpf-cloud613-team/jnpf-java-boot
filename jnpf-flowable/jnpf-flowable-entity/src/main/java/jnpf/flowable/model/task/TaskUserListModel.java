package jnpf.flowable.model.task;

import jnpf.flowable.entity.CirculateEntity;
import jnpf.flowable.entity.OperatorEntity;
import jnpf.flowable.entity.RecordEntity;
import jnpf.flowable.entity.TaskEntity;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/5/21 11:15
 */
@Data
public class TaskUserListModel {

    private List<String> allUserIdList;

    private TaskEntity flowTask;

    private List<OperatorEntity> operatorList = new ArrayList<>();

    private List<CirculateEntity> circulateList = new ArrayList<>();

    private List<RecordEntity> operatorRecordList = new ArrayList<>();
}
