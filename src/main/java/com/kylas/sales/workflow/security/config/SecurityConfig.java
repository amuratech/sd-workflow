package com.kylas.sales.workflow.security.config;

import com.kylas.sales.workflow.security.jwt.JwtTokenRequestFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

  @Autowired
  private JwtTokenRequestFilter jwtTokenRequestFilter;

  @Override
  public void configure(WebSecurity web) {
    web.ignoring()
        .antMatchers(
            "/v2/api-docs",
            "/configuration/**",
            "/swagger-resources/**",
            "/swagger-ui.html",
            "/webjars/**",
            "/api-docs/**");
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http.csrf()
        .and()
        .csrf()
        .disable()
        .authorizeRequests()
        .antMatchers(
            "/v2/api-docs",
            "/configuration/ui",
            "/swagger-resources/**",
            "/configuration/security",
            "/swagger-ui.html",
            "/webjars/**",
            "/actuator/**")
        .permitAll()
        .antMatchers(HttpMethod.OPTIONS)
        .permitAll()
        .anyRequest()
        .authenticated()
        .and()
        .sessionManagement()
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS);

    http.addFilterBefore(jwtTokenRequestFilter, UsernamePasswordAuthenticationFilter.class);
  }
}
