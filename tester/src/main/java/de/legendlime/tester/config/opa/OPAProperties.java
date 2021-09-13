package de.legendlime.tester.config.opa;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
@Configuration
@ConfigurationProperties(prefix = "legendlime.opa")
public class OPAProperties {
	
	private boolean enabled = false;
	private String opaUrl;
	private String policyVersion = "1";
	
	public boolean isEnabled() {
		return enabled;
	}
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	public String getOpaUrl() {
		return opaUrl;
	}
	public void setOpaUrl(String opaUrl) {
		this.opaUrl = opaUrl;
	}
	public String getPolicyVersion() {
		return policyVersion;
	}
	public void setPolicyVersion(String policyVersion) {
		this.policyVersion = policyVersion;
	}	

}
