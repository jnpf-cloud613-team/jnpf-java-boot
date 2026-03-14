package jnpf.base.model.module;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 功能
 */
@Data
public class ModuleModel implements Serializable {
    private String id;
    private String parentId;
    private String fullName;
    private String icon;
    //1-类别、2-页面
    private int type;
    private String urlAddress;
    private String pageAddress;
    private String linkTarget;
    private String category;
    private String description;
    private Long sortCode=999999L;
    private String enCode;
    private String propertyJson;

    private String moduleId;
    private String systemId;
    private Long creatorTime;
    private Date creatorTimes;
    private Integer noShow;

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
