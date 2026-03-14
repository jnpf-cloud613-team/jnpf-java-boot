package jnpf.base.model.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.Date;


@Data
public class PrintLogVO {

    /**
     * 打印人
     */
    private String printMan;
    /**
     * 打印时间
     */
    private Long printTime;
    /**
     * 打印条数
     */
    private Integer printNum;
    /**
     * 打印功能名称
     */
    private String printTitle;

    /**
     * 基于哪一个模板
     */
    private String printId;

    /**
     * 日志id
     */
    @JsonIgnore
    private String id;

    /**
     * 账号
     */
    @JsonIgnore
    private String account;

    /**
     * 名称
     */
    @JsonIgnore
    private String realName;
    /**
     * 打印时间
     */
    @JsonIgnore
    private Date creatorTime;

    public Long getPrintTime() {
        if (this.creatorTime == null) {
            return null;
        }
        return this.creatorTime.getTime();
    }

    public String getPrintMan() {
        return this.realName + "/" + this.account;
    }
}
