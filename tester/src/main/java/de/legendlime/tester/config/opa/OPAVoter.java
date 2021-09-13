package de.legendlime.tester.config.opa;

import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.FilterInvocation;

import de.legendlime.tester.config.client.RestTemplateBean;

public class OPAVoter implements AccessDecisionVoter<Object> {
  
	private static final Logger LOG = LoggerFactory.getLogger(AccessDecisionVoter.class);
    private RestTemplateBean restTemplateBean;	
    private String opaUrl;
    private OPAProperties opaProperties;

    public OPAVoter(String opaUrl, RestTemplateBean restTemplateBean, OPAProperties opaProperties) {
        this.opaUrl = opaUrl;
        this.restTemplateBean = restTemplateBean;
        this.opaProperties = opaProperties;
    }

    @Override
    public boolean supports(ConfigAttribute attribute) {
        return true;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return true;
    }

   @Override
    public int vote(Authentication authentication, Object obj, Collection<ConfigAttribute> attrs) {

        if (!(obj instanceof FilterInvocation)) {
            return ACCESS_ABSTAIN;
        }

        FilterInvocation filter = (FilterInvocation) obj;
        Map<String, String> headers = new HashMap<String, String>();

        for (Enumeration<String> headerNames = filter.getRequest().getHeaderNames(); headerNames.hasMoreElements();) {
            String header = headerNames.nextElement();
            headers.put(header, filter.getRequest().getHeader(header));
        }

        String[] path = filter.getRequest().getRequestURI().replaceAll("^/|/$", "").split("/");

        Map<String, Object> input = new HashMap<String, Object>();
        input.put("auth", authentication);
        input.put("method", filter.getRequest().getMethod());
        input.put("path", path);
        input.put("headers", headers);       
/*
        HttpEntity<?> request = new HttpEntity<>(new OPADataRequest(input));
        OPADataResponse response = restTemplateBean.getRestTemplate()
        		.postForObject(this.opaUrl, request, OPADataResponse.class);

        if (!response.getResult()) {
            return ACCESS_DENIED;
        }
*/
        HttpEntity<?> request = new HttpEntity<>(new OPADataRequest(input));
        OPADataResponse2 response = restTemplateBean.getRestTemplate()
        		.postForObject(this.opaUrl, request, OPADataResponse2.class);
        
        if (response.getResult() == null || response.getResult().isEmpty()) {
        	LOG.debug("Access denied. Empty result in OPA response.");
            return ACCESS_DENIED;
        }
        LOG.debug("Access granted for authorities: {}", response.getResult());
        // add authorities and policy version to response header
        filter.getResponse().addHeader("policy-authority", response.getResult().get(0).getRole());
        filter.getResponse().addHeader("policy-version", response.getResult().get(0).getVersion());

		// store latest policy version in OPA properties
		this.opaProperties.setPolicyVersion(response.getResult().get(0).getVersion());

		return ACCESS_GRANTED;
    }

}
