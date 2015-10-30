package ai.vital.auth.mod.test

import java.util.HashMap;
import java.util.Map;

import org.githubusercontent.defuse.passwordhash.PasswordHash;
import org.vertx.groovy.core.eventbus.EventBus
import org.vertx.groovy.platform.Verticle
import org.vertx.groovy.core.eventbus.Message;

import ai.vital.auth.mod.VitalAuthManager;
import ai.vital.service.vertx.VitalServiceMod;
import ai.vital.service.vertx.json.VitalServiceJSONMapper;
import ai.vital.vitalservice.VitalService;
import ai.vital.vitalservice.query.ResultList;
import ai.vital.vitalservice.query.VitalSelectQuery;
import ai.vital.vitalservice.segment.VitalSegment;
import ai.vital.vitalsigns.datatype.VitalURI;
import ai.vital.vitalsigns.global.GlobalHashTable;
import ai.vital.vitalsigns.meta.GraphContext;
import ai.vital.vitalsigns.model.GraphObject;
import ai.vitalcustomer.app.domain.SuperAdminLogin;

class VitalAuthManagerTest extends Verticle {

	@Override
	public Object start() {

		_1_testSuccessfulLogin(true)
		
		return new Object()
	}
	
	private String sessionID
	
	static long sec = 1000L
	
	private void _1_testSuccessfulLogin(boolean first) {
		
		println "Testing successful login..."
		
		vertx.eventBus.send(VitalAuthManager.address_login, [type: SuperAdminLogin.class.simpleName, username: 'admin1', password: 'password']) { Message message ->
		
			println "Successful login response: " + message.body()

			String sid = sessionID != null ? sessionID : message.body().get('sessionID')
			
			println "Session ? ${sessionID ? 'using previous session value' : 'using old session value'}"		
			
			vertx.eventBus.send(VitalAuthManager.address_authorise, [sessionID: sid]) { Message authResponse ->
				
				println "Auth response: " + authResponse.body 
				
				sessionID = message.body().get('sessionID');
				
				if(first) { 
					println "Call it again to see changed sessionID"
					_1_testSuccessfulLogin(false)
				} else {
					_2_testFailLogin();
				}
			}
			
			
		}
		
	}
	private void _2_testFailLogin() {
	
		vertx.eventBus.send(VitalAuthManager.address_login, [type: SuperAdminLogin.class.simpleName, password: 'pass']) { Message message ->
			
			println "Missing username response: ${message.body}"
			
			vertx.eventBus.send(VitalAuthManager.address_login, [type: SuperAdminLogin.class.simpleName, username: 'admin1']) { Message message2 ->
				
				println "Missing password response: ${message2.body}"
				
				vertx.eventBus.send(VitalAuthManager.address_login, [type: SuperAdminLogin.class.simpleName, username: 'admin1', password: 'pass']) { Message message3 ->
					
					println "Incorrect password response: ${message3.body}"
					
					vertx.eventBus.send(VitalAuthManager.address_login, [type: SuperAdminLogin.class.simpleName, username: 'adminx', password: 'pass']) { Message message4 ->
						
						println "Invalid user response : ${message4.body}"
						
						_3_testSessionProlongWithExpiration()
						
					}
				}
				
			}
		}
			
	}
	
	private void _3_testSessionProlongWithExpiration() {
		
		//session time is set to 10 seconds, 5 second prolong time, immediate authorze won't refresh session
		
		println "\nTESTING SESSION EXPIRATION/REFRESH"
		
		vertx.eventBus.send(VitalAuthManager.address_login, [type: SuperAdminLogin.class.simpleName, username: 'admin1', password: 'password']) { Message message ->
			
			println "Successful login response: " + message.body()
			
			String sid = message.body().get('sessionID')
			
			vertx.eventBus.send(VitalAuthManager.address_authorise, [sessionID: sid]) { Message authResponse ->
				
				println "Instant authorize response: " + authResponse .body()
				
				println "Wait 11 seconds..."
				Thread.sleep(11 * sec)
				vertx.eventBus.send(VitalAuthManager.address_authorise, [sessionID: sid]) { Message auth2Response ->
					println "SHOULD EXPIRE response: " + auth2Response .body()
					
					_4_testSessionProlongWithExpiration();
					
				}
				
				
			}
			
		}
		
	}
	
	private void _4_testSessionProlongWithExpiration() {
		
		println "\nTESTING SESSION EXPIRATION/REFRESH"
		
		vertx.eventBus.send(VitalAuthManager.address_login, [type: SuperAdminLogin.class.simpleName, username: 'admin1', password: 'password']) { Message message ->
			
			println "Successful login response: " + message.body()
			
			println "Wait 7 seconds to refresh session exp date (prolong = 5seconds)"
			
			Thread.sleep(7 * sec)
			
			String sid = message.body().get('sessionID')
			
			vertx.eventBus.send(VitalAuthManager.address_authorise, [sessionID: sid]) { Message authResponse ->
				
				println "after 7 seconds response: " + authResponse .body()
				
				println "Wait 8 seconds..."
				
				Thread.sleep(8 * sec)
				
				vertx.eventBus.send(VitalAuthManager.address_authorise, [sessionID: sid]) { Message auth2Response ->
					
					println "SHOULD BE OK response: " + auth2Response .body()
					
				}
				
			}
			
		}
		
	}
	
}
