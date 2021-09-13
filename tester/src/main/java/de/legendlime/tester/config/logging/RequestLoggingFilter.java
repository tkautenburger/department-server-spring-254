package de.legendlime.tester.config.logging;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.WebUtils;

import io.opentracing.Tracer;


@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter implements Filter {

	public static final int MAX_PAYLOAD_LENGTH = 2000;
	public static final String TRACE_ID = "x-trace-id";
	public static final String REQUEST_PREFIX = "REQUEST : ";
	private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);
	
	private Tracer tracer;
	private ApplicationContext applicationContext;
	
	public RequestLoggingFilter(Tracer tracer, ApplicationContext applicationContext) {
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
		logger.debug("Incoming Trace-ID: {}", traceId);
		logger.debug(createRequestLogMessage(httpServletRequest));

		if (buildSpan && tracer.activeSpan() != null)
			tracer.activeSpan().finish();
		
		filterChain.doFilter(httpServletRequest, servletResponse);
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void destroy() {
	}
	
	protected String createRequestLogMessage(HttpServletRequest request) {
		StringBuilder msg = new StringBuilder();
		
		msg.append(REQUEST_PREFIX);
		msg.append("method=");
		msg.append(request.getMethod());
		msg.append(", ");
		msg.append("uri=");
		msg.append(request.getRequestURI());
		
		String queryString = request.getQueryString();
		if (queryString != null) {
			msg.append('?').append(queryString);
		}		msg.append(", ");
		
		msg.append("client=");
		msg.append(request.getRemoteAddr());
		msg.append(", ");
		
		HttpSession session = request.getSession(false);
		if (session != null) {
			msg.append(", session=").append(session.getId());
		}
		
		String user = request.getRemoteUser();
		if (user != null) {
			msg.append(", user=").append(user);
		}
		
		msg.append(", ");
		msg.append("headers=[");
		Enumeration<String> names = request.getHeaderNames();
		while (names.hasMoreElements()) {
			String name = names.nextElement();
			msg.append(name);
			msg.append(":");
			msg.append(request.getHeader(name));
			msg.append(", ");
		}	
		msg.append("]");
		
		String payload = getMessagePayload(request);
		if (payload != null) {
			msg.append(", payload=").append(payload);
		}
		return msg.toString();
	}
	
	protected String getMessagePayload(HttpServletRequest request) {
		ContentCachingRequestWrapper wrapper =
				WebUtils.getNativeRequest(request, ContentCachingRequestWrapper.class);
		if (wrapper != null) {
			byte[] buf = wrapper.getContentAsByteArray();
			if (buf.length > 0) {
				int length = Math.min(buf.length, MAX_PAYLOAD_LENGTH);
				try {
					return new String(buf, 0, length, wrapper.getCharacterEncoding());
				}
				catch (UnsupportedEncodingException ex) {
					return "[unknown]";
				}
			}
		}
		return null;
	}

}