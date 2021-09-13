package de.legendlime.tester.config.opa;

import java.io.Serializable;

public class OPAResult implements Serializable {
	
	private static final long serialVersionUID = 1L;
	private String version;
	private String role;

	public OPAResult() {
		super();
	}

	public OPAResult(String version, String role) {
		super();
		this.version = version;
		this.role = role;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((role == null) ? 0 : role.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OPAResult other = (OPAResult) obj;
		if (role == null) {
			if (other.role != null)
				return false;
		} else if (!role.equals(other.role))
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "OPARole [version=" + version + ", role=" + role + "]";
	}


}
