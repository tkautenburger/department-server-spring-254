package de.legendlime.tester.config.logging;

import java.io.IOException;
import java.util.Collection;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import io.opentracing.Tracer;


@Order(Ordered.LOWEST_PRECEDENCE)
public class ResponseLoggingFilter implements Filter {

	public static final String TRACE_ID = "x-trace-id";
	public static final String RESPONSE_PREFIX = "RESPONSE : ";
	private static final Logger logger = LoggerFactory.getLogger(ResponseLoggingFilter.class);

	private Tracer tracer;
	private ApplicationContext applicationContext;
	
	public ResponseLoggingFilter(Tracer tracer, ApplicationContext applicationContext) {
		this.tracer = tracer;
		this.applicationContext = applicationContext;
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
			throws IOException, ServletException {

		HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
		boolean buildSpan = false;
		String traceId = httpServletRequest.getHeader(TRACE_ID);
		if (traceId == null || traceId.isEmpty()) {
			if (tracer.activeSpan() != null)
				traceId = tracer.activeSpan().context().toTraceId();
			else {
				traceId = tracer.buildSpan(applicationContext.getId()).start().context().toTraceId();
				buildSpan = true;
			}
		}
		// Add trace ID to the response
		HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
		if (httpServletResponse.getHeader(TRACE_ID) == null)
			// replace all whitespaces from trace id to avoid CRLF header attach
			httpServletResponse.addHeader(TRACE_ID, traceId.replaceAll("\\s",""));

		if (buildSpan && tracer.activeSpan() != null)
			tracer.activeSpan().finish();
		
		filterChain.doFilter(httpServletRequest, httpServletResponse);
		logger.debug(createResponseLogMessage(httpServletResponse));
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void destroy() {
	}
	
	protected String createResponseLogMessage(HttpServletResponse response) {
		StringBuilder msg = new StringBuilder();
		
		msg.append(RESPONSE_PREFIX);
		msg.append("HTTP status=");
		msg.append(response.getStatus());
		msg.append(", ");
		msg.append("headers=[");
		Collection<String> names = response.getHeaderNames();
		names.forEach( name -> {
			msg.append(name);
			msg.append(":");
			msg.append(response.getHeader(name));
			msg.append(", ");
		});
		msg.append("]");
		
		return msg.toString();
	}
}
