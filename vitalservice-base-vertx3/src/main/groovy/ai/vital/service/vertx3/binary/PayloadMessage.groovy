package ai.vital.service.vertx3.binary

class PayloadMessage implements Serializable {
	
	private static final long serialVersionUID = 123L;

	String method
	
	List args
	
	Map<String, Object> sessionParams

	public PayloadMessage() {
		super();
	}


	public PayloadMessage(String method, List args) {
		super();
		this.method = method;
		this.args = args;
	}
	
}
