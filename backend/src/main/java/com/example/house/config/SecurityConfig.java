package com.example.house.config;

  import org.springframework.context.annotation.Bean;
  import org.springframework.context.annotation.Configuration;
  import org.springframework.security.config.annotation.web.builders.HttpSecurity;
  import org.springframework.security.config.http.SessionCreationPolicy;
  import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
  import org.springframework.security.crypto.password.PasswordEncoder;
  import org.springframework.security.web.SecurityFilterChain;
  import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
  import org.springframework.security.config.Customizer;
  import org.springframework.web.cors.CorsConfiguration;
  import org.springframework.web.cors.CorsConfigurationSource;
  import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
  import java.util.List;
import com.example.house.filter.MdcLoggingFilter;
import com.example.house.security.JwtAuthenticationFilter;
  import lombok.RequiredArgsConstructor;
  
  @Configuration
  @RequiredArgsConstructor
  public class SecurityConfig {

	  private final JwtAuthenticationFilter jwtAuthenticationFilter; 
	  private final MdcLoggingFilter mdcLoggingFilter;
	  
      @Bean
      public PasswordEncoder passwordEncoder() {
          return new BCryptPasswordEncoder();
      }

      @Bean
      public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
          http
              .csrf(csrf -> csrf.disable())
              .cors(Customizer.withDefaults())
              .sessionManagement(session ->session
                 .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
              .authorizeHttpRequests(auth -> auth
                  .requestMatchers("/api/auth/**","/error").permitAll()
                  .anyRequest().authenticated()
              )
              .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
              .addFilterAfter(mdcLoggingFilter, JwtAuthenticationFilter.class);
          return http.build();
      }
      
      @Bean
      public CorsConfigurationSource corsConfigurationSource() {
          CorsConfiguration config = new CorsConfiguration();
          config.setAllowedOriginPatterns(List.of(
              "http://localhost:5173",
              "https://myhouse-mu.vercel.app",
              "https://*.vercel.app"
          ));
          config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
          config.setAllowedHeaders(List.of("*"));
          config.setAllowCredentials(true);

          UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
          source.registerCorsConfiguration("/**", config);
          return source;
      }
  }
