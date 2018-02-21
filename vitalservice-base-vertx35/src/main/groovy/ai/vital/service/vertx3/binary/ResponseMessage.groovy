package ai.vital.service.vertx3.binary

import java.io.Serializable;

class ResponseMessage implements Serializable {

	private static final long serialVersionUID = 123L;
	
	String exceptionMessage
	
	String exceptionType
	
	Serializable response

	@Override
	public String toString() {
		return exceptionMessage != null ? "Exception ${exceptionType}: ${exceptionMessage}" : "OK ${response}"
	}


	public ResponseMessage(Serializable response) {
		super();
		this.response = response;
	}

	public ResponseMessage() {
		super();
	}

	public ResponseMessage(String exceptionMessage, String exceptionType) {
		super();
		this.exceptionMessage = exceptionMessage;
		this.exceptionType = exceptionType;
	}
	
	
}
