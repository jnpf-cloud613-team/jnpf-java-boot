package jnpf.base.model.schedule;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class ScheduleNewAppListVO {

    @Schema(description ="app数量")
    private Map<String,Object> signList= new HashMap<>();
    @Schema(description ="当天内容")
    private List<ScheduleNewListVO> todayList= new ArrayList<>();
}
