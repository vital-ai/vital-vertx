package ai.vital.auth.mod

import org.apache.commons.io.FileUtils;
import org.githubusercontent.defuse.passwordhash.PasswordHash;
import org.vertx.groovy.core.eventbus.Message
import org.vertx.java.core.AsyncResult
import org.vertx.java.core.AsyncResultHandler
import org.vertx.java.core.json.JsonObject

import ai.vital.auth.handlers.VitalAuthoriseHandler;
import ai.vital.domain.CredentialsLogin;
import ai.vital.domain.Login;
import ai.vital.domain.UserSession;
import ai.vital.lucene.disk.service.config.VitalServiceLuceneDiskConfig;
import ai.vital.mock.service.VitalServiceMock;
import ai.vital.service.vertx.VitalServiceMod;
import ai.vital.vitalservice.VitalService;
import ai.vital.vitalservice.admin.VitalServiceAdmin;
import ai.vital.vitalservice.factory.VitalServiceFactory;
import ai.vital.vitalservice.json.VitalServiceJSONMapper;
import ai.vital.vitalservice.query.ResultList
import ai.vital.vitalsigns.model.VitalApp
import ai.vital.vitalsigns.model.VitalSegment;
import ai.vital.vitalsigns.model.VitalServiceAdminKey;
import ai.vital.vitalsigns.model.VitalServiceKey;
import ai.vital.vitalsigns.model.VitalServiceRootKey;
import junit.framework.TestCase;

class VitalAuthModTests extends AbstractVitalServiceVertxTest {

	private File luceneRoot
	
	protected Login login
	
	VitalApp app = VitalApp.withId("app")
	
	@Override
	protected void setUp() throws Exception {
		
		
		//init the service here to avoid
		VitalServiceLuceneDiskConfig cfg = new VitalServiceLuceneDiskConfig()
		VitalServiceRootKey rootKey = new VitalServiceRootKey().generateURI((VitalApp) null)
		cfg.app = app 
		
		VitalServiceAdminKey adminKey = new VitalServiceAdminKey().generateURI((VitalApp) null)
		adminKey.key = "admi-admi-admi"
		
		VitalServiceKey serviceKey = new VitalServiceKey().generateURI((VitalApp) null)
		serviceKey.key = "serv-serv-serv"
		
		
		luceneRoot = File.createTempDir("vitalauth", "lucenedisk")
		
		cfg.rootPath = luceneRoot.absolutePath
		
		rootKey.key = 'root-root-root'
		VitalServiceFactory.initService(cfg, rootKey, null) 
		
		VitalServiceAdmin adminService = VitalServiceFactory.openAdminService(adminKey, cfg)
		
		
		adminService.addApp(app)
		
		VitalSegment loginsSegment = VitalSegment.withId("logins")
		
		VitalSegment sessionsSegment = VitalSegment.withId("sessions")
		
		adminService.addSegment(app, loginsSegment, true)
		
		adminService.addSegment(app, sessionsSegment, true)
		
		adminService.close()
		
		
		//the service should be ready
		VitalService service = VitalServiceFactory.openService(serviceKey, cfg, VitalServiceMod.SERVICE_NAME_PREFIX + app.appID.toString())
		
		login = new Login().generateURI(app)
		login.username = "test"
		login.password = PasswordHash.createHash("pass")
		login.active = true
		login.emailVerified = true
		
		
		service.save(loginsSegment, login, true)
		
		
		super.setUp();
		
		//once service mod is up setup the auth mod
		
		Map modCfgMap = [
			
			apps: [
				"${app.appID.toString()}": [
					auth_enabled: true,
					access: 'service',
					loginsSegment: loginsSegment.segmentID.toString(),
					sessionsSegment: sessionsSegment.segmentID.toString(),
					persistentSessions: true,
					maxSessionsPerUser: 3,
					expirationProlongMargin: 10000,
					filter: [
						[ type: 'allow', method: 'ping' ],
						[ type: 'allow', method: 'callFunction', function: 'vitalauth\\..*' ],
						[ type: 'auth', method: 'listSegments' ],
						[ type: 'deny', method: '.*'] 
					]
				]	
			]
			
		]
		
		JsonObject modCfg = new JsonObject(modCfgMap)
		
		ltp.delayed { ->
		
			ltp.pm.deployModule("vital-ai~vital-auth-mod~0.2.303", modCfg, 1, new AsyncResultHandler<String>() {
				
				public void handle(AsyncResult<String> asyncResult) {
					if (asyncResult.succeeded()) {
						println("Vital Auth Mod deployment ID is " + asyncResult.result());
					} else {
						ltp.ex = asyncResult.cause()
						ltp.ex.printStackTrace()
					}
					
					ltp.resume()
					
				}
			});
		
		}
	
		ltp.waitNow()
		
	}
	
	@Override
	protected void tearDown() throws Exception {
		FileUtils.deleteQuietly(luceneRoot)
		super.tearDown();
	}
	
	public void testAuthFlow() {
		
		doTestAuthFlow1()
		
	}
	
	private void doTestAuthFlow1() {
		
		Map body = null
		
		ltp.delayed { ->
			
			ltp.vertx.eventBus.send(VitalAuthManager.address_login, [appID: app.appID.toString(), type: Login.class.simpleName, username: 'test', password: 'pass']) { Message response ->
				
				body = response.body()
				
				ltp.resume()
				
			}
		}
		
		ltp.waitNow()
		
		assertEquals(body.message, "ok", body.status)
		
		String sessionID = body.sessionID
						
		CredentialsLogin rLogin = VitalServiceJSONMapper.fromJSON(body.object)
						
		assertEquals(login, rLogin)
						
		doTestAuthFlow2(sessionID);
		
		
	}
	
	private doTestAuthFlow2(String sessionID) {
		
		Map body = null
		
		ltp.delayed {
			
			ltp.vertx.eventBus.send(VitalAuthManager.address_authorise, [appID: app.appID.toString(), sessionID: sessionID]) { Message response ->

				body = response.body()
				
				ltp.resume()
				
				
			}
			
		}
		
		ltp.waitNow()

		assertEquals("ok", body.status)
		
		assertEquals(sessionID, body.sessionID)
		
		CredentialsLogin rLogin = VitalServiceJSONMapper.fromJSON(body.object)
				
		doTestAuthFlow3(sessionID)
		
	}
	
	private doTestAuthFlow3(String sessionID) {

		Map body = null
				
		ltp.delayed { ->
			
			ltp.vertx.eventBus.send(VitalAuthManager.address_logout, [appID: app.appID.toString(), type: Login.class.simpleName, sessionID: sessionID]) { Message response ->
				
				body = response.body()
				
				
				ltp.resume()
					
								
			}
		}
		
		ltp.waitNow()
		
		assertEquals("ok", body.status )
		
		doTestAuthFlow4();
		
	}
	
	private doTestAuthFlow4() {
		
		Map body = null
		
		ltp.delayed { ->
			
			ltp.vertx.eventBus.send(VitalAuthManager.address_login, [appID: app.appID.toString(), type: Login.class.simpleName, username: 'test', password: 'passwrong']) { Message response ->
				
				body = response.body()
				
				ltp.resume()
					
								
			}
		}
		
		ltp.waitNow()
		
		assertEquals(VitalAuthManager.error_invalid_password, body.status)
		
		doTestAuthFlow5();
	}
	
	private doTestAuthFlow5() {
		
		Map body = null

		ltp.delayed {
			
			ltp.vertx.eventBus.send(VitalAuthManager.address_authorise, [appID: app.appID.toString(), sessionID: 'Login_111']) { Message response ->

				body = response.body()
				
				ltp.resume()
				
			}
			
		}
		
		ltp.waitNow()
		
		assertEquals(VitalAuthManager.error_denied, body.status)
	
		endpointAccessPing();
			
			
	}
	
	private void endpointAccessPing() {
		
		println "TESTING PING"
		
		//ping is allowed without authentication
		Map body = null
		
		String serviceAddress = VitalJSEndpointsManager.ENDPOINT_PREFIX + app.appID.toString()
		
		ltp.delayed {
			
			ltp.vertx.eventBus.send(serviceAddress, [method: 'ping', sessionID: null, args: []]) { Message response ->
				
				body = response.body()
				
				
				ltp.resume()
				
			}
			
		}
		
		ltp.waitNow()
		
		assertEquals(body.message, 'ok', body.status)
		
		
		println "TESTING LIST SEGMENTS"
		
		ltp.delayed {
			
			ltp.vertx.eventBus.send(serviceAddress, [method: 'listSegments', sessionID: null, args: []]) { Message response ->
				
				body = response.body()
				
				ltp.resume()
				
			}
			
		}
		
		ltp.waitNow()
		
		assertEquals(body.message, VitalJSEndpointsManager.error_authentication_required, body.status)
		
		
		println "TESTING LIST SEGMENTS WITH BROKEN SESSION"
		
		ltp.delayed {
			
			ltp.vertx.eventBus.send(serviceAddress, [method: 'listSegments', sessionID: 'Login_fake!', args: []]) { Message response ->
				
				body = response.body()
				
				ltp.resume()
				
			}
			
		}
		
		ltp.waitNow()
		
		assertEquals(body.message, VitalAuthManager.error_denied, body.status)
		
		
		println "AUTHENTICATION"
		
		//authenticate and access the method
		
		ltp.delayed { ->
			
			ltp.vertx.eventBus.send(VitalAuthManager.address_login, [appID: app.appID.toString(), type: Login.class.simpleName, username: 'test', password: 'pass']) { Message response ->
				
				body = response.body()
				
				ltp.resume()
				
			}
		}
		
		ltp.waitNow()
		
		assertEquals(body.message, "ok", body.status)
		
		String sessionID = body.sessionID
		
		println "TESTING PING WITH VALID SESSION"
		
		ltp.delayed {
			
			ltp.vertx.eventBus.send(serviceAddress, [method: 'ping', sessionID: sessionID, args: []]) { Message response ->
				
				body = response.body()
				
				
				ltp.resume()
				
			}
			
		}
		
		ltp.waitNow()
		
		assertEquals(body.message, 'ok', body.status)
		
		
		println "TESTING LIST SEGMENTS WITH VALID SESSION"
		
		ltp.delayed {
			
			ltp.vertx.eventBus.send(serviceAddress, [method: 'listSegments', sessionID: sessionID, args: []]) { Message response ->
				
				body = response.body()
				
				ltp.resume()
				
			}
			
		}
		
		ltp.waitNow()
		
		assertEquals(body.message, 'ok', body.status)
		
		
		println "TESTING vitalauth.authorise call"
		
		ltp.delayed {
			
			ltp.vertx.eventBus.send(serviceAddress, [method: 'callFunction', sessionID: sessionID, args: [VitalAuthoriseHandler.function_authorise, [sessionID: sessionID]]]) { Message response ->
				
				body = response.body()
				
				ltp.resume()
				
			}
			
		}
		
		ltp.waitNow()
		
		assertEquals(body.message, 'ok', body.status)
		
		ResultList rl = VitalServiceJSONMapper.fromJSON(body.response)
		
		assertEquals(2, rl.results.size())
		
		assertTrue(rl.results[0].graphObject instanceof Login)
		
		assertTrue(rl.results[1].graphObject instanceof UserSession)
		
		
		
	}
	
	
}
