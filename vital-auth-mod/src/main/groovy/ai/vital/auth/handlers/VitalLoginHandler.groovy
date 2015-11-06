package ai.vital.auth.handlers

import groovy.lang.Closure

import java.util.Map;

import org.vertx.groovy.core.eventbus.Message;

import ai.vital.domain.CredentialsLogin;
import ai.vital.domain.UserSession;
import ai.vital.service.vertx.handler.AsyncCallFunctionHandler;
import ai.vital.service.vertx.handler.VertxAwareAsyncCallFunctionHandler;
import ai.vital.vitalservice.VitalStatus;
import ai.vital.vitalservice.json.VitalServiceJSONMapper;
import ai.vital.vitalservice.query.ResultElement
import ai.vital.vitalservice.query.ResultList;
import ai.vital.vitalsigns.model.VitalApp;
import ai.vital.vitalsigns.model.VitalOrganization;

class VitalLoginHandler extends VertxAwareAsyncCallFunctionHandler {

	public final static String function_login = 'vitalauth.login'
	
	String loginAddress
	
	@Override
	public void callFunction(VitalOrganization organization, VitalApp app,
			String function, Map<String, Object> params, Closure closure)
			throws Exception {

				
		try {
			
			if(!loginAddress) throw new Exception("Internal error - No loginAddress")
			
			String type = params.get('loginType')
			if(!type) throw new RuntimeException("No 'loginType' param")
			
			String username = params.get('username')
			if(username == null) throw new RuntimeException("No 'username' param")
			
			String password = params.get('password')
			if(password == null) throw new RuntimeException("No 'password' param")
			
			if(app == null) { throw new RuntimeException("App must not be null") }
			
			String appID = app.appID.toString()
			
			vertx.eventBus.send(loginAddress, [appID: appID, type: type, username: username, password: password]) { Message response ->
				
				ResultList rl = new ResultList()
				
				Map body = response.body()
				
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
