package ai.vital.service.vertx3.handler

class Subscription {

	Set<String> streamNames = new HashSet<String>()
	
	String sessionID
	
	//last refresh time
	long timestamp
	
}
