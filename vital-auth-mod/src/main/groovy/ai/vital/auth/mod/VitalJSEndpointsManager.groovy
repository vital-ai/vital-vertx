package ai.vital.auth.mod

import org.vertx.groovy.core.eventbus.Message;

import ai.vital.auth.handlers.VitalAuthoriseHandler;
import ai.vital.auth.handlers.VitalLoginHandler;
import ai.vital.auth.handlers.VitalLogoutHandler;
import ai.vital.auth.mod.AppFilter.Allow;
import ai.vital.auth.mod.AppFilter.Auth
import ai.vital.auth.mod.AppFilter.Deny;
import ai.vital.auth.mod.AppFilter.Rule
import ai.vital.auth.mod.VitalAuthManager.AuthAppBean;
import ai.vital.vitalservice.json.VitalServiceJSONMapper;

class VitalJSEndpointsManager {

	public final static String error_access_denied = 'error_access_denied'
	
	public final static String error_filter_config_invalid = 'error_filter_config_invalid'
	
	public final static String error_invalid_request = 'error_invalid_request'
	
	public final static String error_authentication_required = 'error_authentication_required'
	
	public final static String ENDPOINT_PREFIX = 'endpoint.'
	
	public VitalAuthManager authManager
	
	public VitalJSEndpointsManager(VitalAuthManager authManager) {
		super();
		this.authManager = authManager;
	}

	public void deployEndpoints() {
		
		for(AuthAppBean bean : authManager.beans.values()) {
			
			authManager.container.logger.info ("Deploying endpoint for app: ${bean.appID}")
			
			String address = ENDPOINT_PREFIX + bean.appID
			
			authManager.vertx.eventBus.registerHandler(address) { Message message ->
				
				filterRequest(bean, message)
				
			}
			
		}
		
	}
	
	public void filterRequest(AuthAppBean bean, Message msg) {
		
		Map jsonResponse = null
		
		AppFilter filter = bean.filter
		
		try {
					
			Object bodyObj = msg.body()
		
			if(!(bodyObj instanceof Map)) throw new RuntimeException("Expected body to be a map")
			
			Map body = bodyObj

			
			if(body.login != null) throw new RuntimeException("Login object must not be set!")
			
			String method = body.get("method")
			
			if(! method) throw new RuntimeException("No method param")
			
			List args = body.get("args")
				
			if(args == null) throw new RuntimeException("Args list cannot be null")
			
			String scriptName = null;
			
			String sessionID = body.get('sessionID')
			
			
			//inject appID into auth call
			
			if(method == 'callFunction') {
				scriptName = args.size() >= 2 ? args[args.size() - 2] : null
				
				//enforce the app for authentication
				
				if(bean._supportsAdmin()) {
					
					if( scriptName == VitalAuthoriseHandler.function_authorise ) {
						
						args[0] = VitalServiceJSONMapper.toJSON(bean.app)
						
					}
					
				}
				
//				if(scriptName == VitalAuthoriseHandler.function_authorise || scriptName == VitalLoginHandler.function_login
//					|| scriptName == VitalLogoutHandler.function_logout) {
//					//override the appid 
//				}
				
			}

			for(Rule rule : filter.rules) {
				
				if(rule.methodMatch(method, scriptName)) {
					
					if(rule instanceof Allow) {
						
						//pass it down the service instance
						if(sessionID != null) {
							
							//auth
							bean.vertx.eventBus.send(bean.authorizeAddress, [appID: bean.appID, sessionID: sessionID]) { Message authResponse ->
								
								Map authBody = authResponse.body()
								
								String authStatus = authBody.get('status')
								
								if(authStatus != 'ok') {
									msg.reply(authBody)
									return
								}
								
	//							message.reply([status: 'ok', sessionID: sessionID, object: VitalServiceJSONMapper.toJSON(object)])
								
								
								//pass the login to handlers to avoid the redundancy of checking the session twice
								body.put('login', authBody.get('object'))
								
								//authorized
								bean._passMessage(msg)
								return
								
							}
							
							
						} else {
						
							bean._passMessage(msg)
							
							
						}
						return
						
					} else if(rule instanceof Deny) {
					
						//send msg reply immediately
						msg.reply([status: error_access_denied, message: (String) "Access to method ${method} ${scriptName ? (' (script name : ' + scriptName + ')') : ''} is denied"])
						return
					
						
					} else if(rule instanceof Auth) {
		
						if(sessionID == null) {
							msg.reply([status: error_authentication_required, message: (String) "${scriptName ? (' (script name : ' + scriptName + ')') : ''} requires authentication"])
							return
						}
						
						//auth
						bean.vertx.eventBus.send(bean.authorizeAddress, [appID: bean.appID, sessionID: sessionID]) { Message authResponse ->
							
							Map authBody = authResponse.body()
							
							String authStatus = authBody.get('status')
							
							if(authStatus != 'ok') {
								msg.reply(authBody)
								return
							}
							
//							message.reply([status: 'ok', sessionID: sessionID, object: VitalServiceJSONMapper.toJSON(object)])
							
							
							//pass the login to handlers to avoid the redundancy of checking the session twice
							body.put('login', authBody.get('object'))
							
							//authorized
							bean._passMessage(msg)
							return
							
						}
								
					} else {
						throw new RuntimeException("Unimplemented rule: ${rule?.class.canonicalName}")
					}
					
					return
					
				}
				
			}
			
			msg.reply([status: error_filter_config_invalid, message: "the filters configuration is incomplete, no rule for method: ${method} ${scriptName ? (' (script name : ' + scriptName + ')') : ''}"]);
						
		} catch(Exception e) {
		
			msg.reply([status: error_invalid_request, message: e.localizedMessage])
		
		
		}
		
					
	}
	
}
