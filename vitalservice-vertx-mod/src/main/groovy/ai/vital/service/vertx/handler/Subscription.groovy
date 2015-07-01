package ai.vital.service.vertx.handler

class Subscription {

	Set<String> streamNames = new HashSet<String>()
	
	String sessionID
	
	//last refresh time
	long timestamp
	
}
