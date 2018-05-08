package com.hdw.upms.shiro;

import at.pollux.thymeleaf.shiro.dialect.ShiroDialect;
import com.google.code.kaptcha.Constants;
import com.google.code.kaptcha.servlet.KaptchaServlet;
import com.hdw.upms.shiro.cache.RedisCacheManager;
import com.hdw.upms.shiro.cache.RedisSessionDAO;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.codec.Base64;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.session.mgt.ExecutorServiceSessionValidationScheduler;
import org.apache.shiro.session.mgt.SessionManager;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.CookieRememberMeManager;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.servlet.SimpleCookie;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import javax.servlet.Filter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Description : Apache Shiro 核心通过 Filter 来实现，就好像SpringMvc 通过DispachServlet
 * 来主控制一样。 既然是使用 Filter 一般也就能猜到，是通过URL规则来进行过滤和权限校验，所以我们需要定义一系列关于URL的规则和访问权限。
 */

@Configuration
@Order(1)
public class ShiroConfig {

	@Value("${hdw.upms.loginUrl}")
	private String loginUrl;

	@Value("${hdw.upms.successUrl}")
	private String successUrl;

	@Value("${hdw.upms.unauthorizedUrl}")
	private String unauthorizedUrl;


	@Autowired
	private RedisSessionDAO sessionDAO;

	@Bean
	public RedisCacheManager redisCacheManager() {
		return new RedisCacheManager();
	}

	// 配置kaptcha图片验证码框架提供的Servlet,,这是个坑,很多人忘记注册(注意)
	@Bean
	public ServletRegistrationBean kaptchaServlet() {
		ServletRegistrationBean servlet = new ServletRegistrationBean(new KaptchaServlet(), "/kaptcha.jpg");
		servlet.addInitParameter(Constants.KAPTCHA_SESSION_CONFIG_KEY, Constants.KAPTCHA_SESSION_KEY);// session key
		servlet.addInitParameter(Constants.KAPTCHA_TEXTPRODUCER_FONT_SIZE, "50");// 字体大小
		servlet.addInitParameter(Constants.KAPTCHA_BORDER, "no");
		servlet.addInitParameter(Constants.KAPTCHA_BORDER_COLOR, "105,179,90");
		servlet.addInitParameter(Constants.KAPTCHA_TEXTPRODUCER_FONT_SIZE, "45");
		servlet.addInitParameter(Constants.KAPTCHA_TEXTPRODUCER_CHAR_LENGTH, "4");
		servlet.addInitParameter(Constants.KAPTCHA_TEXTPRODUCER_FONT_NAMES, "宋体,楷体,微软雅黑");
		servlet.addInitParameter(Constants.KAPTCHA_TEXTPRODUCER_FONT_COLOR, "blue");
		servlet.addInitParameter(Constants.KAPTCHA_IMAGE_WIDTH, "125");
		servlet.addInitParameter(Constants.KAPTCHA_IMAGE_HEIGHT, "60");
		// 可以设置很多属性,具体看com.google.code.kaptcha.Constants
		// kaptcha.border 是否有边框 默认为true 我们可以自己设置yes，no
		// kaptcha.border.color 边框颜色 默认为Color.BLACK
		// kaptcha.border.thickness 边框粗细度 默认为1
		// kaptcha.producer.impl 验证码生成器 默认为DefaultKaptcha
		// kaptcha.textproducer.impl 验证码文本生成器 默认为DefaultTextCreator
		// kaptcha.textproducer.char.string 验证码文本字符内容范围 默认为abcde2345678gfynmnpwx
		// kaptcha.textproducer.char.length 验证码文本字符长度 默认为5
		// kaptcha.textproducer.font.names 验证码文本字体样式 默认为new Font("Arial", 1, fontSize),
		// new Font("Courier", 1, fontSize)
		// kaptcha.textproducer.font.size 验证码文本字符大小 默认为40
		// kaptcha.textproducer.font.color 验证码文本字符颜色 默认为Color.BLACK
		// kaptcha.textproducer.char.space 验证码文本字符间距 默认为2
		// kaptcha.noise.impl 验证码噪点生成对象 默认为DefaultNoise
		// kaptcha.noise.color 验证码噪点颜色 默认为Color.BLACK
		// kaptcha.obscurificator.impl 验证码样式引擎 默认为WaterRipple
		// kaptcha.word.impl 验证码文本字符渲染 默认为DefaultWordRenderer
		// kaptcha.background.impl 验证码背景生成器 默认为DefaultBackground
		// kaptcha.background.clear.from 验证码背景颜色渐进 默认为Color.LIGHT_GRAY
		// kaptcha.background.clear.to 验证码背景颜色渐进 默认为Color.WHITE
		// kaptcha.image.width 验证码图片宽度 默认为200
		// kaptcha.image.height 验证码图片高度 默认为50
		return servlet;
	}

	// 注入异常处理类
	@Bean
	public ShiroExceptionResolver shiroExceptionResolver() {
		return new ShiroExceptionResolver();
	}

	/**
	 * ShiroFilterFactoryBean 处理拦截资源文件问题。 注意：单独一个ShiroFilterFactoryBean配置是或报错的，以为在
	 * 初始化ShiroFilterFactoryBean的时候需要注入：SecurityManager Filter Chain定义说明
	 * 1、一个URL可以配置多个Filter，使用逗号分隔 2、当设置多个过滤器时，全部验证通过，才视为通过 3、部分过滤器可指定参数，如perms，roles
	 */
	@Bean
	public ShiroFilterFactoryBean shiroFilter(SecurityManager securityManager) {

		ShiroFilterFactoryBean shiroFilterFactoryBean = new ShiroFilterFactoryBean();
		// 必须设置 SecurityManager
		shiroFilterFactoryBean.setSecurityManager(securityManager);
		// 验证码过滤器
		Map<String, Filter> filtersMap = shiroFilterFactoryBean.getFilters();
		KaptchaFilter kaptchaFilter = new KaptchaFilter();
		filtersMap.put("kaptchaFilter", kaptchaFilter);
		
		filtersMap.put("user", ajaxSessionFilter());
		
		// 实现自己规则roles,这是为了实现or的效果
		// RoleFilter roleFilter = new RoleFilter();
		// filtersMap.put("roles", roleFilter);
		shiroFilterFactoryBean.setFilters(filtersMap);
		// 拦截器
		Map<String, String> filterChainDefinitionMap = new LinkedHashMap<String, String>();
		// 配置退出过滤器,其中的具体的退出代码Shiro已经替我们实现了
		filterChainDefinitionMap.put("/logout", "logout");
		// 配置记住我或认证通过可以访问的地址
		filterChainDefinitionMap.put("/index", "user");
		filterChainDefinitionMap.put("/", "user");
		filterChainDefinitionMap.put("/login", "kaptchaFilter");
		// 开放的静态资源
		filterChainDefinitionMap.put("/favicon.ico", "anon");// 网站图标
		filterChainDefinitionMap.put("/static/**", "anon");// 配置static文件下资源能被访问
		filterChainDefinitionMap.put("/css/**", "anon");
		filterChainDefinitionMap.put("/font/**", "anon");
		filterChainDefinitionMap.put("/img/**", "anon");
		filterChainDefinitionMap.put("/js/**", "anon");
		filterChainDefinitionMap.put("/plugins/**", "anon");
		filterChainDefinitionMap.put("/kaptcha.jpg", "anon");// 图片验证码(kaptcha框架)
		filterChainDefinitionMap.put("/api/**", "anon");// API接口

		// swagger接口文档
		filterChainDefinitionMap.put("/v2/api-docs", "anon");
		filterChainDefinitionMap.put("/webjars/**", "anon");
		filterChainDefinitionMap.put("/swagger-resources/**", "anon");
		filterChainDefinitionMap.put("/swagger-ui.html", "anon");
		filterChainDefinitionMap.put("/doc.html", "anon");

		// 其他的
		filterChainDefinitionMap.put("/solr/**", "anon");
		filterChainDefinitionMap.put("/test/**", "anon");
		filterChainDefinitionMap.put("/**", "authc");

		// 如果不设置默认会自动寻找Web工程根目录下的"/login.jsp"页面
		shiroFilterFactoryBean.setLoginUrl(loginUrl);
		// 登录成功后要跳转的链接
		shiroFilterFactoryBean.setSuccessUrl(successUrl);
		// 未授权界面，不生效(详情原因看MyExceptionResolver)
		shiroFilterFactoryBean.setUnauthorizedUrl(unauthorizedUrl);
		shiroFilterFactoryBean.setFilterChainDefinitionMap(filterChainDefinitionMap);

		return shiroFilterFactoryBean;
	}

	/**
	 * ajax session超时时处理
	 *
	 * @return
	 */
	@Bean
	public ShiroAjaxSessionFilter ajaxSessionFilter() {
		ShiroAjaxSessionFilter shiroAjaxSessionFilter = new ShiroAjaxSessionFilter();
		return shiroAjaxSessionFilter;
	}

	@Bean
	public SecurityManager securityManager() {
		DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
		// 设置realm.
		securityManager.setRealm(shiroDBRealm());
		// 注入Session管理器
	     securityManager.setSessionManager(sessionManager());
		// 注入缓存管理器
		securityManager.setCacheManager(redisCacheManager());
		// 注入记住我管理器;
		securityManager.setRememberMeManager(rememberMeManager());
		return securityManager;
	}

	/**
	 * 身份认证realm; (这个需要自己写，账号密码校验；权限等)
	 */
	@Bean
	public ShiroDBRealm shiroDBRealm() {
		ShiroDBRealm shiroDBRealm = new ShiroDBRealm();
		shiroDBRealm.setCredentialsMatcher(hashedCredentialsMatcher());
		shiroDBRealm.setCacheManager(redisCacheManager());
		// 启用身份验证缓存，即缓存AuthenticationInfo信息，默认false
		shiroDBRealm.setAuthenticationCachingEnabled(true);
		// 缓存AuthenticationInfo信息的缓存名称
		shiroDBRealm.setAuthenticationCacheName("authenticationCache");
		// 缓存AuthorizationInfo信息的缓存名称
		shiroDBRealm.setAuthorizationCacheName("authorizationCache");
		return shiroDBRealm;
	}

	/**
	 * 凭证匹配器 （由于我们的密码校验交给Shiro的SimpleAuthenticationInfo进行处理了
	 * 所以我们需要修改下doGetAuthenticationInfo中的代码; @return
	 */
	@Bean
	public HashedCredentialsMatcher hashedCredentialsMatcher() {
		HashedCredentialsMatcher hashedCredentialsMatcher = new HashedCredentialsMatcher();
		hashedCredentialsMatcher.setHashAlgorithmName("md5");// 散列算法:这里使用MD5算法;
		hashedCredentialsMatcher.setHashIterations(2);// 散列的次数，比如散列两次，相当于md5(md5(""));
		hashedCredentialsMatcher.setStoredCredentialsHexEncoded(true);// 表示是否存储散列后的密码为16进制，需要和生成密码时的一样，默认是base64；
		return hashedCredentialsMatcher;
	}

	/**
	 * 开启shiro aop注解支持. 使用代理方式; 所以需要开启代码支持;
	 *
	 * @param securityManager
	 * @return
	 */
	@Bean
	public AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor(SecurityManager securityManager) {
		AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor = new AuthorizationAttributeSourceAdvisor();
		authorizationAttributeSourceAdvisor.setSecurityManager(securityManager);
		return authorizationAttributeSourceAdvisor;
	}

	/**
	 * cookie对象;
	 *
	 * @return
	 */
	@Bean
	public SimpleCookie rememberMeCookie() {
		// 这个参数是cookie的名称，对应前端的checkbox的name = rememberMe
		SimpleCookie simpleCookie = new SimpleCookie("rememberMe");
		simpleCookie.setHttpOnly(true);
		// 记住我cookie生效时间7天 ,单位秒
		simpleCookie.setMaxAge(60*60*24*7);
		return simpleCookie;
	}

	/**
	 * cookie管理对象;
	 *
	 * @return
	 */
	@Bean
	public CookieRememberMeManager rememberMeManager() {
		CookieRememberMeManager cookieRememberMeManager = new CookieRememberMeManager();
		cookieRememberMeManager.setCookie(rememberMeCookie());
		cookieRememberMeManager.setCipherKey(Base64.decode("5aaC5qKm5oqA5pyvAAAAAA=="));
		return cookieRememberMeManager;
	}

	@Bean(name = "sessionManager")
    public SessionManager sessionManager() {
		DefaultWebSessionManager sessionManager = new DefaultWebSessionManager();
		sessionManager.setGlobalSessionTimeout(18000000);
		sessionManager.setSessionDAO(sessionDAO);
		// url中是否显示session Id
		sessionManager.setSessionIdUrlRewritingEnabled(false);
		// 删除失效的session
		sessionManager.setDeleteInvalidSessions(true);
		sessionManager.setSessionValidationSchedulerEnabled(true);
		sessionManager.setSessionValidationInterval(18000000);
		sessionManager.setSessionValidationScheduler(getExecutorServiceSessionValidationScheduler());
		
		sessionManager.getSessionIdCookie().setName("session-z-id");
		sessionManager.getSessionIdCookie().setPath("/");
		sessionManager.getSessionIdCookie().setMaxAge(60 * 60 * 24 * 7);
        sessionManager.getSessionIdCookie().setHttpOnly(true);
		return sessionManager;
	}

	@Bean(name = "sessionValidationScheduler")
	public ExecutorServiceSessionValidationScheduler getExecutorServiceSessionValidationScheduler() {
		ExecutorServiceSessionValidationScheduler scheduler = new ExecutorServiceSessionValidationScheduler();
		scheduler.setInterval(900000);
		return scheduler;
	}

	@Bean(name = "shiroDialect")
	public ShiroDialect shiroDialect() {
		return new ShiroDialect();
	}

}