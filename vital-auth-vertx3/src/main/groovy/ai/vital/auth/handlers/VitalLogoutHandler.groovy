package ai.vital.auth.handlers

import groovy.lang.Closure

import io.vertx.groovy.core.Future
import io.vertx.groovy.core.eventbus.Message
import java.util.Map;

import ai.vital.service.vertx3.handler.VertxAwareAsyncCallFunctionHandler
import ai.vital.vitalservice.VitalStatus;
import ai.vital.vitalservice.query.ResultList
import ai.vital.vitalsigns.model.VitalApp;
import ai.vital.vitalsigns.model.VitalOrganization;

class VitalLogoutHandler extends VertxAwareAsyncCallFunctionHandler {

	public final static String function_logout = 'vitalauth.logout'
	
	String logoutAddress
	
	@Override
	public void callFunction(VitalOrganization organization, VitalApp app,
			String function, Map<String, Object> params, Map<String, Object> sessionParams, Closure closure)
			throws Exception {

				
		try {
			
			if(!logoutAddress) throw new Exception("Internal error - No logoutAddress")
			
			if(app == null) { throw new RuntimeException("App must not be null") }
			
			String sessionID = params.get('sessionID')
			if(sessionID == null) throw new RuntimeException("No 'sessionID' param")
			
			String appID = app.appID.toString()
			
			vertx.eventBus().send(logoutAddress, [appID: appID, sessionID: sessionID]) { Future<Message> response ->
				
				ResultList rl = new ResultList()
				
				if(!response.succeeded()) {
					
					Throwable t = response.cause()
					if(t != null) {
						rl.status = VitalStatus.withError( t.getClass().getCanonicalName() + t.getLocalizedMessage())
					} else {
						rl.status = VitalStatus.withError( 'unknown_exception / unknown error' )
					}
					
					closure(rl)
					return
					
				}
				
				Map body = response.result().body()
				
				String status = body.status
				
				if(status != 'ok') {
					
					rl.status = VitalStatus.withError(status + ' ' + body.message)
					
				} else {
				
					rl.status = VitalStatus.withOKMessage(body.message)
				
				}
				
				closure(rl)
				
			}
			
			

		} catch(Exception e) {
		
			ResultList rl = new ResultList()
			rl.status = VitalStatus.withError(e.localizedMessage)
			closure(rl)
			
		}
		
	}

}
