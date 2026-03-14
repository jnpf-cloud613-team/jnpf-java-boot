package jnpf.message.model;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.io.Serializable;

/**
 */
@Data
public class WxgzhMessageModel implements Serializable {
    //模板ID
    @JSONField(name = "template_id")
    private String templateId;
    //模板标题
    private String title;
    //模板所属行业的一级行业
    @JSONField(name = "primary_industry")
    private String primaryIndustry;
    //模板所属行业的二级行业
    @JSONField(name = "deputy_industry")
    private String deputyIndustry;
    //模板内容
    private String content;
    //模板示例
    private String example;
}
