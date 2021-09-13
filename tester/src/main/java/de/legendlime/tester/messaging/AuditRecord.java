package de.legendlime.tester.messaging;

public class AuditRecord {
	
	String timestamp;
	String method;
	String uri;
	String objectType;
	long objectId;
	String client;
	String sessionId;
	String user;
	String traceId;
	String nodeName;
	String hostName;
	String podName;
	String jsonObject;
	
	public AuditRecord() {
		super();
	}

	public AuditRecord(String method, String uri, String objectType, long objectId, String client, String sessionId,
			String user, String traceId, String nodeName, String hostName, String podName, String jsonObject) {
		super();
		this.method = method;
		this.uri = uri;
		this.objectType = objectType;
		this.objectId = objectId;
		this.client = client;
		this.sessionId = sessionId;
		this.user = user;
		this.traceId = traceId;
		this.nodeName = nodeName;
		this.hostName = hostName;
		this.podName = podName;
		this.jsonObject = jsonObject;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getObjectType() {
		return objectType;
	}

	public void setObjectType(String objectType) {
		this.objectType = objectType;
	}

	public long getObjectId() {
		return objectId;
	}

	public void setObjectId(long objectId) {
		this.objectId = objectId;
	}

	public String getClient() {
		return client;
	}

	public void setClient(String client) {
		this.client = client;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getTraceId() {
		return traceId;
	}

	public void setTraceId(String traceId) {
		this.traceId = traceId;
	}

	public String getNodeName() {
		return nodeName;
	}

	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}

	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	public String getPodName() {
		return podName;
	}

	public void setPodName(String podName) {
		this.podName = podName;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	public String getJsonObject() {
		return jsonObject;
	}

	public void setJsonObject(String jsonObject) {
		this.jsonObject = jsonObject;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((client == null) ? 0 : client.hashCode());
		result = prime * result + ((hostName == null) ? 0 : hostName.hashCode());
		result = prime * result + ((jsonObject == null) ? 0 : jsonObject.hashCode());
		result = prime * result + ((method == null) ? 0 : method.hashCode());
		result = prime * result + ((nodeName == null) ? 0 : nodeName.hashCode());
		result = prime * result + (int) (objectId ^ (objectId >>> 32));
		result = prime * result + ((objectType == null) ? 0 : objectType.hashCode());
		result = prime * result + ((podName == null) ? 0 : podName.hashCode());
		result = prime * result + ((sessionId == null) ? 0 : sessionId.hashCode());
		result = prime * result + ((timestamp == null) ? 0 : timestamp.hashCode());
		result = prime * result + ((traceId == null) ? 0 : traceId.hashCode());
		result = prime * result + ((uri == null) ? 0 : uri.hashCode());
		result = prime * result + ((user == null) ? 0 : user.hashCode());
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
		AuditRecord other = (AuditRecord) obj;
		if (client == null) {
			if (other.client != null)
				return false;
		} else if (!client.equals(other.client))
			return false;
		if (hostName == null) {
			if (other.hostName != null)
				return false;
		} else if (!hostName.equals(other.hostName))
			return false;
		if (jsonObject == null) {
			if (other.jsonObject != null)
				return false;
		} else if (!jsonObject.equals(other.jsonObject))
			return false;
		if (method == null) {
			if (other.method != null)
				return false;
		} else if (!method.equals(other.method))
			return false;
		if (nodeName == null) {
			if (other.nodeName != null)
				return false;
		} else if (!nodeName.equals(other.nodeName))
			return false;
		if (objectId != other.objectId)
			return false;
		if (objectType == null) {
			if (other.objectType != null)
				return false;
		} else if (!objectType.equals(other.objectType))
			return false;
		if (podName == null) {
			if (other.podName != null)
				return false;
		} else if (!podName.equals(other.podName))
			return false;
		if (sessionId == null) {
			if (other.sessionId != null)
				return false;
		} else if (!sessionId.equals(other.sessionId))
			return false;
		if (timestamp == null) {
			if (other.timestamp != null)
				return false;
		} else if (!timestamp.equals(other.timestamp))
			return false;
		if (traceId == null) {
			if (other.traceId != null)
				return false;
		} else if (!traceId.equals(other.traceId))
			return false;
		if (uri == null) {
			if (other.uri != null)
				return false;
		} else if (!uri.equals(other.uri))
			return false;
		if (user == null) {
			if (other.user != null)
				return false;
		} else if (!user.equals(other.user))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "AuditRecord [timestamp=" + timestamp + ", method=" + method + ", uri=" + uri + ", objectType="
				+ objectType + ", objectId=" + objectId + ", client=" + client + ", sessionId=" + sessionId + ", user="
				+ user + ", traceId=" + traceId + ", nodeName=" + nodeName + ", hostName=" + hostName + ", podName="
				+ podName + ", jsonObject=" + jsonObject + "]";
	}
}
