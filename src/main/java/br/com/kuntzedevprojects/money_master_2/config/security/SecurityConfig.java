package br.com.kuntzedevprojects.money_master_2.config.security;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import java.util.List;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import br.com.kuntzedevprojects.money_master_2.config.onboarding.OnboardingCompletionFilter;
import br.com.kuntzedevprojects.money_master_2.config.properties.CorsProperties;
import br.com.kuntzedevprojects.money_master_2.config.properties.SecurityHardeningProperties;
import br.com.kuntzedevprojects.money_master_2.config.rate.RateLimitFilter;
import br.com.kuntzedevprojects.money_master_2.repositories.UserFinancialProfileRepository;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	private static final String[] PUBLIC_POST_ENDPOINTS = { "/auth/login", "/auth/logout", "/auth/register",
			"/auth/refresh", "/auth/resend-confirmation" };

	private static final String[] PUBLIC_GET_ENDPOINTS = { "/auth/confirm-email", "/themes/active", "/users/*/avatar",
			"/actuator/health", "/actuator/info" };

	@SuppressWarnings("removal")
	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationConverter jwtAuthenticationConverter,
			OnboardingCompletionFilter onboardingCompletionFilter, RateLimitFilter rateLimitFilter,
			SecurityHardeningProperties hardeningProperties) throws Exception {
		SecurityHardeningProperties.Headers headerProperties = hardeningProperties.getHeaders();

		return http.csrf(AbstractHttpConfigurer::disable).cors(Customizer.withDefaults())
				.sessionManagement(session -> session.sessionCreationPolicy(STATELESS)).headers(headers -> {
					headers.contentSecurityPolicy(
							csp -> csp.policyDirectives(headerProperties.getContentSecurityPolicy()));

					headers.frameOptions(frame -> frame.deny());

					headers.contentTypeOptions(Customizer.withDefaults());

					headers.referrerPolicy(
							referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER));

					headers.httpStrictTransportSecurity(hsts -> {
						if (headerProperties.isHstsEnabled()) {
							hsts.includeSubDomains(true).maxAgeInSeconds(headerProperties.getHstsMaxAgeSeconds());
						} else {
							hsts.disable();
						}
					});

					headers.permissionsPolicy(
							permissions -> permissions.policy(headerProperties.getPermissionsPolicy()));
				})
				.authorizeHttpRequests(authorize -> authorize.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers(HttpMethod.POST, PUBLIC_POST_ENDPOINTS).permitAll()
						.requestMatchers(HttpMethod.GET, PUBLIC_GET_ENDPOINTS).permitAll()
						.requestMatchers("/actuator/**").hasRole("ADMIN").anyRequest().authenticated())
				.oauth2ResourceServer(
						oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
				.addFilterBefore(rateLimitFilter, BearerTokenAuthenticationFilter.class)
				.addFilterAfter(onboardingCompletionFilter, BearerTokenAuthenticationFilter.class).build();
	}

	@Bean
	FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(RateLimitFilter filter) {
		FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>(filter);
		registration.setEnabled(false);
		return registration;
	}

	@Bean
	OnboardingCompletionFilter onboardingCompletionFilter(UserFinancialProfileRepository profileRepository) {
		return new OnboardingCompletionFilter(profileRepository);
	}

	@Bean
	JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(new JwtAuthoritiesConverter());
		return converter;
	}

	@SuppressWarnings("deprecation")
	@Bean
	DaoAuthenticationProvider authenticationProvider(CustomUserDetailsService userDetailsService,
			PasswordEncoder passwordEncoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
		provider.setUserDetailsService(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		return provider;
	}

	@Bean
	AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
			throws Exception {
		return authenticationConfiguration.getAuthenticationManager();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(12);
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
		validateCors(properties);

		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOriginPatterns(properties.getAllowedOriginPatterns());
		configuration.setAllowedMethods(properties.getAllowedMethods());
		configuration.setAllowedHeaders(properties.getAllowedHeaders());
		configuration.setExposedHeaders(properties.getExposedHeaders());
		configuration.setAllowCredentials(properties.isAllowCredentials());
		configuration.setMaxAge(properties.getMaxAgeSeconds());

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	private void validateCors(CorsProperties properties) {
		List<String> patterns = properties.getAllowedOriginPatterns();
		if (!properties.isAllowCredentials() || patterns == null) {
			return;
		}
		boolean unsafeWildcard = patterns.stream().map(String::trim)
				.anyMatch(pattern -> "*".equals(pattern) || "http://*".equals(pattern) || "https://*".equals(pattern));
		if (unsafeWildcard) {
			throw new IllegalStateException("CORS inseguro: não use wildcard '*' com allowCredentials=true.");
		}
	}
}
