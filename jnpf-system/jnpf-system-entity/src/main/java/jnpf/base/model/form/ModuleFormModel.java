package jnpf.base.model.form;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021-09-14
 */
@Data
public class ModuleFormModel implements Serializable {
    private String id;
    private String fullName;
    private String parentId;
    private String enCode;
    private String moduleId;
    private String icon;
    private String systemId;
    private Long sortCode=999999L;
    private Long creatorTime;
    private Date creatorTimes;

    public Long getCreatorTime() {
        if (this.creatorTimes != null && this.creatorTime == null) {
            return this.getCreatorTimes().getTime();
        } else if (this.creatorTime != null){
            return this.creatorTime;
        }
        return 0L;
    }

    public void setCreatorTimes(Date creatorTimes) {
        this.creatorTimes = creatorTimes;
        if(creatorTimes != null && this.creatorTime == null){
            this.creatorTime = creatorTimes.getTime();
        }
    }

    public void setCreatorTime(Long creatorTime) {
        this.creatorTime = creatorTime;
        if(creatorTime != null && this.creatorTimes == null){
            this.creatorTimes = new Date(creatorTime);
        }
    }
}
