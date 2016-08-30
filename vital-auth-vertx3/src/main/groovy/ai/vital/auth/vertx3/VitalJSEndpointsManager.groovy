package ai.vital.auth.vertx3

import io.vertx.core.Future
import io.vertx.groovy.core.eventbus.Message
import org.slf4j.Logger
import org.slf4j.LoggerFactory;

import ai.vital.auth.handlers.VitalAuthoriseHandler;
import ai.vital.auth.handlers.VitalLoginHandler;
import ai.vital.auth.handlers.VitalLogoutHandler;
import ai.vital.auth.vertx3.AppFilter.Allow
import ai.vital.auth.vertx3.AppFilter.Auth
import ai.vital.auth.vertx3.AppFilter.Deny
import ai.vital.auth.vertx3.AppFilter.Rule
import ai.vital.auth.vertx3.VitalAuthManager.AuthAppBean
import ai.vital.vitalservice.json.VitalServiceJSONMapper;

class VitalJSEndpointsManager {

	private final static Logger log = LoggerFactory.getLogger(VitalJSEndpointsManager.class)
	
	public final static String error_access_denied = 'error_access_denied'
	
	public final static String error_filter_config_invalid = 'error_filter_config_invalid'
	
	public final static String error_invalid_request = 'error_invalid_request'
	
	public final static String error_authentication_required = 'error_authentication_required'
	
	public final static String ENDPOINT_PREFIX = 'endpoint.'
	
	public final static String SESSION_LOGIN = '_sessionLogin'
	
	public final static String SESSION_ID = '_sessionID'
	
	public VitalAuthManager authManager
	
	public VitalJSEndpointsManager(VitalAuthManager authManager) {
		super();
		this.authManager = authManager;
	}

	public void deployEndpoints() {
		
		for(AuthAppBean bean : authManager.beans.values()) {
			
			log.info ("Deploying endpoint for app: ${bean.appID}")
			
			final thisBean = bean
			
			String address = ENDPOINT_PREFIX + bean.appID
			
			authManager.vertx.eventBus().consumer(address) { Message message ->
				
				filterRequest(address, thisBean, message)
				
			}
			
		}
		
	}
	
	public void filterRequest(String address, AuthAppBean bean, Message msg) {
		
		log.info("Filter request: address ${address}, ${bean.appID}")
		
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
							bean.vertx.eventBus().send(bean.authorizeAddress, [appID: bean.appID, sessionID: sessionID]) { Future<Message> authResponse ->
								
								if(!authResponse.succeeded()) {
									log.error(authResponse.cause().localizedMessage, authResponse.cause())
									return
								}
								
								Map authBody = authResponse.result().body()
								
								String authStatus = authBody.get('status')
								
								if(authStatus != 'ok') {
									msg.reply(authBody)
									return
								}
								
	//							message.reply([status: 'ok', sessionID: sessionID, object: VitalServiceJSONMapper.toJSON(object)])
								
								
								//pass the login to handlers to avoid the redundancy of checking the session twice
								body.put(SESSION_LOGIN, authBody.get('object'))
								body.put(SESSION_ID, sessionID)
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
						bean.vertx.eventBus().send(bean.authorizeAddress, [appID: bean.appID, sessionID: sessionID]) { Future<Message> authResponse ->
							
							
							if(!authResponse.succeeded()) {
								log.error(authResponse.cause())
								return
							}
							
							Map authBody = authResponse.result().body()
							
							String authStatus = authBody.get('status')
							
							if(authStatus != 'ok') {
								msg.reply(authBody)
								return
							}
							
//							message.reply([status: 'ok', sessionID: sessionID, object: VitalServiceJSONMapper.toJSON(object)])
							
							
							//pass the login to handlers to avoid the redundancy of checking the session twice
							body.put(SESSION_LOGIN, authBody.get('object'))
							body.put(SESSION_ID, sessionID)
							
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
