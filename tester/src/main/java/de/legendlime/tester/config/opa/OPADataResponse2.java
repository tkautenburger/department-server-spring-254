package de.legendlime.tester.config.opa;

import java.util.ArrayList;
import java.util.List;

public class OPADataResponse2 {
	
    private List<OPAResult> result = new ArrayList<>();

    public OPADataResponse2() {
    }

	public List<OPAResult> getResult() {
		return result;
	}

	public void setResult(List<OPAResult> result) {
		this.result = result;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.result == null) ? 0 : this.result.hashCode());
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
		OPADataResponse2 other = (OPADataResponse2) obj;
		if (result == null) {
			if (other.result != null)
				return false;
		} else if (!result.equals(other.result))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "[result=" + result + "]";
	}

}
