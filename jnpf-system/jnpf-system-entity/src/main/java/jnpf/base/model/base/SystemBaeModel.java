package jnpf.base.model.base;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author ：JNPF开发平台组
 * @version: V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date ：2022/6/22 14:26
 */
@Data
public class SystemBaeModel extends SystemCrModel implements Serializable {

    private String id;
    private Long creatorTime;
    private Date creatorTimes;
    private Integer isMain;
    private Boolean isTop;

    public Long getCreatorTime() {
        if (this.creatorTimes != null && this.creatorTime == null) {
            return this.getCreatorTimes().getTime();
        } else if (this.creatorTime != null){
            return this.creatorTime;
        }
        return 0L;
    }

}
