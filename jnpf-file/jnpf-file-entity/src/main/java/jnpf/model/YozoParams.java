package jnpf.model;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author JNPF
 */
@Data
@Component
public class YozoParams {


    @Value("${config.YozoDomainKey}")
    private String domainKey;

    @Value("${config.YozoDomain}")
    private String domain;

    @Value("${config.YozoCloudDomain}")
    private String cloudDomain;

    @Value("${config.YozoAppId}")
    private String appId;

    @Value("${config.YozoAppKey}")
    private String appKey;

    @Value("${config.YozoEditDomain}")
    private  String editDomain;

    @Value("${config.ApiDomain}")
    private String jnpfDomain;


}
