package ai.vital.service.vertx.binary

class ResponseMessage implements Serializable {

	private static final long serialVersionUID = 123L;
	
	String exceptionMessage
	
	String exceptionType
	
	Serializable response

	@Override
	public String toString() {
		return exceptionMessage != null ? "Exception ${exceptionType}: ${exceptionMessage}" : "OK ${response}"
	}
		
}
