package ai.vital.auth.handlers

import ai.vital.domain.CredentialsLogin
import ai.vital.domain.UserSession
import ai.vital.service.vertx3.handler.VertxAwareAsyncCallFunctionHandler
import ai.vital.vitalservice.VitalStatus
import ai.vital.vitalservice.json.VitalServiceJSONMapper
import ai.vital.vitalservice.query.ResultElement
import ai.vital.vitalservice.query.ResultList
import ai.vital.vitalsigns.model.VitalApp
import ai.vital.vitalsigns.model.VitalOrganization
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message

class VitalAuthoriseHandler extends VertxAwareAsyncCallFunctionHandler {

	public final static String function_authorise = 'vitalauth.authorise'
	
	String authoriseAddress
	
	@Override
	public void callFunction(VitalOrganization organization, VitalApp app,
			String function, Map<String, Object> params, Map<String, Object> sessionParams, Closure closure)
			throws Exception {

				
		try {
			
			if(!authoriseAddress) throw new Exception("Internal error - No authoriseAddress")
			
			if(app == null) { throw new RuntimeException("App must not be null") }
			
			String sessionID = params.get('sessionID')
			if(sessionID == null) throw new RuntimeException("No 'sessionID' param")
			
			String appID = app.appID.toString()
			
			vertx.eventBus().send(authoriseAddress, [appID: appID, sessionID: sessionID]) { Future<Message> response ->
				
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
				
					CredentialsLogin login = VitalServiceJSONMapper.fromJSON(body.object)
					rl.results.add(new ResultElement(login, 1D))
					
					UserSession session = new UserSession().generateURI(app)
					session.sessionID = body.sessionID
					rl.results.add(new ResultElement(session, 1D))
				}
				
				closure(rl)
				
			}
			
			

		} catch(Exception e) {
		
			ResultList rl = new ResultList()
			rl.status = VitalStatus.withError(e.localizedMessage)
			closure(rl)
			
		}
		
		//how to determine the login type

	}

}
