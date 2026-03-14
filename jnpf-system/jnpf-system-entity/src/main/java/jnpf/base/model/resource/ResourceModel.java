package jnpf.base.model.resource;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 资源
 */
@Data
public class ResourceModel implements Serializable {
    private String id;
    private String fullName;
    private String enCode;
    private String conditionJson;
    private String conditionText;
    private Integer allData;
    private String moduleId;
    private String matchLogic;
    private String objectId;
    private String icon;
    private String parentId;
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
