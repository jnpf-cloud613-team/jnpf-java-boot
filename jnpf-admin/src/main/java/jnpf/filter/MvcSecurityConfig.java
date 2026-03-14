package jnpf.filter;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.context.model.SaRequest;
import cn.dev33.satoken.context.model.SaResponse;
import cn.dev33.satoken.filter.SaFilterAuthStrategy;
import cn.dev33.satoken.router.SaRouter;
import jnpf.properties.MvcSecurityProperties;
import jnpf.util.Constants;
import jnpf.util.IpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.util.ObjectUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.net.URI;

/**
 * mvc配置
 *
 * @author JNPF开发平台组
 * @version V5.0.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2025-01-21
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class MvcSecurityConfig {

    private static final String DOMAIN_FORMAT = "%s://%s";


    private final MvcSecurityProperties mvcSecurityProperties;



    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        // 允许发送凭据
        config.setAllowCredentials(true);
        //允许任意域名跨域访问接口
        config.setAllowedOrigins(mvcSecurityProperties.getCors().getAllowedOrigins());
        config.setAllowedOriginPatterns(mvcSecurityProperties.getCors().getAllowedOriginPatterns());
        // 允许所有头部信息
        config.setAllowedHeaders(mvcSecurityProperties.getCors().getAllowedHeaders());
        // 允许所有请求方法
        config.setAllowedMethods(mvcSecurityProperties.getCors().getAllowedMethods());
        // 应用于所有路径
        source.registerCorsConfiguration("/**", config);
        return new MyCorsFilter(source);
    }

    @Bean
    @ConditionalOnMissingBean
    public SaFilterAuthStrategy defaultBeforeAuthStrategy() {
        CorsConfiguration csrfConfiguration;
        if (!mvcSecurityProperties.getCsrfOrigins().isEmpty() || !mvcSecurityProperties.getCsrfOriginsPatterns().isEmpty()) {
            csrfConfiguration = new CorsConfiguration();
            csrfConfiguration.setAllowedOrigins(mvcSecurityProperties.getCsrfOrigins());
            csrfConfiguration.setAllowedOriginPatterns(mvcSecurityProperties.getCsrfOriginsPatterns());
        } else {
            csrfConfiguration = null;
        }
        return obj -> {
            SaRequest request = SaHolder.getRequest();
            // ---------- 设置跨域响应头 ----------
            SaResponse response = SaHolder.getResponse();
            if (!ObjectUtils.isEmpty(mvcSecurityProperties.getHeaders().getServerName())) {
                response.setServer(mvcSecurityProperties.getHeaders().getServerName());
            }
            if (!ObjectUtils.isEmpty(mvcSecurityProperties.getHeaders().getXFrameOptions()) && !MvcSecurityProperties.XFrameOptionsMode.DISABLED.equals(mvcSecurityProperties.getHeaders().getXFrameOptions())) {
                response.setHeader(MvcSecurityProperties.HEADER_XFRAME_OPTIONS, mvcSecurityProperties.getHeaders().getXFrameOptions().getMode());
            }
            if (!ObjectUtils.isEmpty(mvcSecurityProperties.getHeaders().getXXssProtection()) && !MvcSecurityProperties.XXssProtectionMode.DISABLED.equals(mvcSecurityProperties.getHeaders().getXXssProtection())) {
                response.setHeader(MvcSecurityProperties.HEADER_XSS_PROTECTION, mvcSecurityProperties.getHeaders().getXXssProtection().getMode());
            }
            if (!ObjectUtils.isEmpty(mvcSecurityProperties.getHeaders().getXContentTypeOptions()) && !MvcSecurityProperties.XContentTypeOptions.DISABLED.equals(mvcSecurityProperties.getHeaders().getXContentTypeOptions())) {
                response.setHeader(MvcSecurityProperties.HEADER_CONTENT_TYPE_OPTIONS, mvcSecurityProperties.getHeaders().getXContentTypeOptions().getMode());
            }

            if (csrfConfiguration != null) {
                String referer = request.getHeader("referer");
                if (!ObjectUtils.isEmpty(referer)) {
                    URI uri = URI.create(referer);
                    String refererDomain = String.format(DOMAIN_FORMAT, uri.getScheme(), uri.getAuthority());
                    String allowOrign = csrfConfiguration.checkOrigin(refererDomain);
                    if (ObjectUtils.isEmpty(allowOrign)) {
                        log.error("Reject CSRF Request: {}, {}, {}, {}", request.getRequestPath(), referer, IpUtil.getIpAddr(), request.getHeader(Constants.AUTHORIZATION));
                        response.setStatus(HttpStatus.FORBIDDEN.value());
                        SaRouter.back("Invalid CSRF Request");
                    }
                }
            }
        };
    }


    @Order(-110)
    public static class MyCorsFilter extends CorsFilter {
        public MyCorsFilter(CorsConfigurationSource configSource) {
            super(configSource);
        }
    }
}
