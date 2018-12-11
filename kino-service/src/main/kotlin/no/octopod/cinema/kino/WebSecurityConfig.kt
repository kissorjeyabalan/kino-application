package no.octopod.cinema.kino

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails

//Copied from acruri82 testing and development, websecurityconfig

@Configuration
@EnableWebSecurity
class WebSecurityConfig: WebSecurityConfigurerAdapter() {

    override fun configure(http: HttpSecurity) {
        http
            .httpBasic()
            .and()
            .authorizeRequests()
            .antMatchers(HttpMethod.GET, "/theaters/**").permitAll()
            .antMatchers(HttpMethod.GET, "/shows/**").permitAll()
            .antMatchers("/theaters/**").hasRole("ADMIN")
            .antMatchers("/shows/**").hasRole("ADMIN")
            .anyRequest().denyAll()
            .and()
            .csrf().disable()
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.NEVER)
    }
}

