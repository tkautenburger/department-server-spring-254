package de.legendlime.tester.config.ratelimit;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.google.common.util.concurrent.RateLimiter;

@Component
@Order(1)
public class RateLimitingFilter implements Filter {
	
	@Autowired
	RateLimiter rateLimiter;

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		
		if (!rateLimiter.tryAcquire()) {
			HttpServletResponse httpResponse = (HttpServletResponse) response;
			httpResponse.sendError(429, "Too Many Requests");
			return;
		}
	    chain.doFilter(request, response);		
	}

}
