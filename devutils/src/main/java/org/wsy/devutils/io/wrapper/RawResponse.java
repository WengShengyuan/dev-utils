package org.wsy.devutils.io.wrapper;

public class RawResponse {
	
	private int httpCode;
	private String responseContent;
	private String reasonPhase;
	public int getHttpCode() {
		return httpCode;
	}
	public void setHttpCode(int httpCode) {
		this.httpCode = httpCode;
	}
	public String getReasonPhase() {
		return reasonPhase;
	}
	public void setReasonPhase(String reasonPhase) {
		this.reasonPhase = reasonPhase;
	}
	public String getResponseContent() {
		return responseContent;
	}
	public void setResponseContent(String responseContent) {
		this.responseContent = responseContent;
	}
}
