package jnpf.base.model.schedule;

import jnpf.base.UserInfo;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class ScheduleJobModel {
    private UserInfo userInfo = new UserInfo();
    private Date scheduleTime = new Date();
    private List<String> userList = new ArrayList<>();
    private String id;
}
