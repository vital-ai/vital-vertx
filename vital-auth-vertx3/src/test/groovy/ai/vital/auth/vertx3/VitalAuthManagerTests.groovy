package ai.vital.auth.vertx3

import org.apache.commons.io.FileUtils
import org.githubusercontent.defuse.passwordhash.PasswordHash;

import ai.vital.auth.handlers.VitalAuthoriseHandler;
import ai.vital.auth.vertx3.VitalAuthManager
import ai.vital.auth.vertx3.VitalJSEndpointsManager;
import ai.vital.domain.CredentialsLogin
import ai.vital.domain.Login
import ai.vital.domain.UserSession
import ai.vital.domain.UserSession_PropertiesHelper
import ai.vital.lucene.disk.service.config.VitalServiceLuceneDiskConfig
import ai.vital.query.querybuilder.VitalBuilder
import ai.vital.service.vertx3.VitalServiceVertx3;
import ai.vital.vitalservice.VitalService
import ai.vital.vitalservice.admin.VitalServiceAdmin
import ai.vital.vitalservice.factory.VitalServiceFactory
import ai.vital.vitalservice.json.VitalServiceJSONMapper;
import ai.vital.vitalservice.query.ResultList
import ai.vital.vitalsigns.model.VitalApp
import ai.vital.vitalsigns.model.VitalSegment
import ai.vital.vitalsigns.model.VitalServiceAdminKey
import ai.vital.vitalsigns.model.VitalServiceKey
import ai.vital.vitalsigns.model.VitalServiceRootKey
import groovy.json.JsonOutput;
import io.vertx.core.AsyncResult
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject
import io.vertx.groovy.core.eventbus.Message

class VitalAuthManagerTests extends AbstractVitalServiceVertxTest {
	
	private File luceneRoot
	
	protected Login login
	
	VitalApp app = VitalApp.withId("app")
	
	VitalService service = null
		
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
		service = VitalServiceFactory.openService(serviceKey, cfg, VitalServiceVertx3.SERVICE_NAME_PREFIX + app.appID.toString())
		
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
					session_timeout: 1800000,
					cachedLoginsLRUSize: 10,
					cachedLoginsTTL: 1000,
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
		
			ltp.vertx.deployVerticle("groovy:" + VitalAuthManager.class.canonicalName, [config: modCfg, worker: false, instances: 1]) { AsyncResult<String> asyncResult ->
				
				if (asyncResult.succeeded()) {
					println("Vital Auth Vertx3 deployment ID is " + asyncResult.result());
				} else {
					ltp.ex = asyncResult.cause()
					ltp.ex.printStackTrace()
				}
					
				ltp.resume()
					
			}
		
		}
	
		ltp.waitNow()
		
	}
	
	@Override
	protected void tearDown() throws Exception {
		FileUtils.deleteQuietly(luceneRoot)
		super.tearDown();
	}
	
	public void testAuthFlow() {
		
		println "Testing auth flow"
		
		doTestAuthFlow1()
		
	}
	
	private void doTestAuthFlow1() {
		
		Map body = null
		
		ltp.delayed { ->
			
			println "sending login request"
			
			ltp.vertx.eventBus().send(VitalAuthManager.address_login, [appID: app.appID.toString(), type: Login.class.simpleName, username: 'test', password: 'pass']) { Future<Message> response ->
			
				if(response.succeeded()) {
					body = response.result().body()
				} else {
					System.err.println( response.cause() )
				}
			
				ltp.resume()
					
				println "body received"
			
				ltp.resume()
				
			}
		}
		
		ltp.waitNow()
		
		assertNotNull(body)
		
		assertEquals(body.message, "ok", body.status)
		
		String sessionID = body.sessionID
						
		CredentialsLogin rLogin = VitalServiceJSONMapper.fromJSON(body.object)
						
		assertEquals(login, rLogin)
						
		doTestAuthFlow2(sessionID);
		
		
	}
	
	private doTestAuthFlow2(String sessionID) {
		
		Map body = null
		
		ltp.delayed {
			
			ltp.vertx.eventBus().send(VitalAuthManager.address_authorise, [appID: app.appID.toString(), sessionID: sessionID]) { Future<Message> response ->

				if(response.succeeded()) {
					body = response.result().body()
				} else {
					System.err.println( response.cause() )
				}
				
				ltp.resume()
				
				
			}
			
		}
		
		ltp.waitNow()

		assertNotNull(body)
		
		assertEquals("ok", body.status)
		
		assertEquals(sessionID, body.sessionID)
		
		CredentialsLogin rLogin = VitalServiceJSONMapper.fromJSON(body.object)
				
		doTestAuthFlow3(sessionID)
		
	}
	
	private doTestAuthFlow3(String sessionID) {

		Map body = null
				
		ltp.delayed { ->
			
			ltp.vertx.eventBus().send(VitalAuthManager.address_logout, [appID: app.appID.toString(), type: Login.class.simpleName, sessionID: sessionID]) { Future<Message> response ->
				
				if(response.succeeded()) {
					body = response.result().body()
				} else {
					System.err.println( response.cause() )
				}
				
				ltp.resume()
								
			}
		}
		
		
		ltp.waitNow()
		
		assertNotNull(body)
		
		assertEquals("ok", body.status )
		
		doTestAuthFlow4();
		
	}
	
	private doTestAuthFlow4() {
		
		Map body = null
		
		ltp.delayed { ->
			
			ltp.vertx.eventBus().send(VitalAuthManager.address_login, [appID: app.appID.toString(), type: Login.class.simpleName, username: 'test', password: 'passwrong']) { Future<Message> response ->
				
				if(response.succeeded()) {
					body = response.result().body()
				} else {
					System.err.println( response.cause() )
				}
				
				ltp.resume()
					
								
			}
		}
		
		ltp.waitNow()
		
		assertNotNull(body)
		
		assertEquals(VitalAuthManager.error_invalid_password, body.status)
		
		doTestAuthFlow5();
	}
	
	private doTestAuthFlow5() {
		
		Map body = null

		ltp.delayed {
			
			ltp.vertx.eventBus().send(VitalAuthManager.address_authorise, [appID: app.appID.toString(), sessionID: 'Login_111']) {Future<Message> response ->

				if(response.succeeded()) {
					body = response.result().body()
				} else {
					System.err.println( response.cause() )
				}
				
				ltp.resume()
				
			}
			
		}
		
		ltp.waitNow()
		
		assertNotNull(body)
		
		assertEquals(VitalAuthManager.error_denied, body.status)
	
		endpointAccessPing();
			
			
	}
	
	private void endpointAccessPing() {
		
		println "TESTING PING"
		
		//ping is allowed without authentication
		Map body = null
		
		String serviceAddress = VitalJSEndpointsManager.ENDPOINT_PREFIX + app.appID.toString()
		
		ltp.delayed {
			
			ltp.vertx.eventBus().send(serviceAddress, [method: 'ping', sessionID: null, args: []]) { Future<Message> response ->
				
				if(response.succeeded()) {
					body = response.result().body()
				} else {
					System.err.println( response.cause() )
				}
				
				ltp.resume()
				
			}
			
		}
		
		ltp.waitNow()
		
		assertNotNull(body)
		
		assertEquals(body.message, 'ok', body.status)
		
		
		println "TESTING LIST SEGMENTS"
		
		ltp.delayed {
			
			ltp.vertx.eventBus().send(serviceAddress, [method: 'listSegments', sessionID: null, args: []]) { Future<Message> response ->
				
				if(response.succeeded()) {
					body = response.result().body()
				} else {
					System.err.println( response.cause() )
				}
				
				ltp.resume()
				
			}
			
		}
		
		ltp.waitNow()
		
		assertNotNull(body)
		
		assertEquals(body.message, VitalJSEndpointsManager.error_authentication_required, body.status)
		
		
		println "TESTING LIST SEGMENTS WITH BROKEN SESSION"
		
		body = null
		
		ltp.delayed {
			
			ltp.vertx.eventBus().send(serviceAddress, [method: 'listSegments', sessionID: 'Login_fake!', args: []]) { Future<Message> response ->
				
				if(response.succeeded()) {
					body = response.result().body()
				} else {
					System.err.println( response.cause() )
				}
				
				ltp.resume()
				
			}
			
		}
		
		ltp.waitNow()
		
		assertNotNull(body)
		
		assertEquals(body.message, VitalAuthManager.error_denied, body.status)
		
		
		println "AUTHENTICATION"
		
		//authenticate and access the method
		
		body = null
		
		ltp.delayed { ->
			
			ltp.vertx.eventBus().send(VitalAuthManager.address_login, [appID: app.appID.toString(), type: Login.class.simpleName, username: 'test', password: 'pass']) { Future<Message> response ->
				
				if(response.succeeded()) {
					body = response.result().body()
				} else {
					System.err.println( response.cause() )
				}
				
				ltp.resume()
				
			}
		}
		
		ltp.waitNow()
		
		assertNotNull(body)
		
		assertEquals(body.message, "ok", body.status)
		
		String sessionID = body.sessionID
		
		println "TESTING PING WITH VALID SESSION"
		
		body = null
		
		ltp.delayed {
			
			ltp.vertx.eventBus().send(serviceAddress, [method: 'ping', sessionID: sessionID, args: []]) { Future<Message> response ->
				
				if(response.succeeded()) {
					body = response.result().body()
				} else {
					System.err.println( response.cause() )
				}
				
				ltp.resume()
				
			}
			
		}
		
		ltp.waitNow()
		
		assertNotNull(body)
		
		assertEquals(body.message, 'ok', body.status)
		
		
		println "TESTING LIST SEGMENTS WITH VALID SESSION"
		
		body = null
		
		ltp.delayed {
			
			ltp.vertx.eventBus().send(serviceAddress, [method: 'listSegments', sessionID: sessionID, args: []]) { Future<Message> response ->
				
				if(response.succeeded()) {
					body = response.result().body()
				} else {
					System.err.println( response.cause() )
				}
				
				ltp.resume()
				
			}
			
		}
		
		ltp.waitNow()
		
		assertNotNull(body)
		
		assertEquals(body.message, 'ok', body.status)
		
		
		println "TESTING vitalauth.authorise call"
		
		body = null
		
		ltp.delayed {
			
			ltp.vertx.eventBus().send(serviceAddress, [method: 'callFunction', sessionID: sessionID, args: [VitalAuthoriseHandler.function_authorise, [sessionID: sessionID]]]) { Future<Message> response ->
				
				if(response.succeeded()) {
					body = response.result().body()
				} else {
					System.err.println( response.cause() )
				}
				
				ltp.resume()
				
			}
			
		}
		
		ltp.waitNow()
		
		assertNotNull(body)
		
		assertEquals(body.message, 'ok', body.status)
		
		println JsonOutput.toJson(body.response)
		
		ResultList rl = VitalServiceJSONMapper.fromJSON(body.response)
		
		assertEquals(2, rl.results.size())
		
		assertTrue(rl.results[0].graphObject instanceof Login)
		
		assertTrue(rl.results[1].graphObject instanceof UserSession)
		
		doTestSessionExpiration()
		
	}
	
	private void doTestSessionExpiration() {

		
		Map body = null
		
		ltp.delayed { ->
			
			println "sending login request"
			
			ltp.vertx.eventBus().send(VitalAuthManager.address_login, [appID: app.appID.toString(), type: Login.class.simpleName, username: 'test', password: 'pass']) { Future<Message> response ->
				
				println "body received"
			
				if(response.succeeded()) {
					body = response.result().body()
				} else {
					System.err.println( response.cause() )
				}
				
				ltp.resume()
				
			}
		}
		
		ltp.waitNow()
		
		assertEquals(body.message, "ok", body.status)
		
		String sessionID = body.sessionID
						
		CredentialsLogin rLogin = VitalServiceJSONMapper.fromJSON(body.object)
						
		assertEquals(login, rLogin)
						

		body = null
		
		ltp.delayed {
			
			ltp.vertx.eventBus().send(VitalAuthManager.address_authorise, [appID: app.appID.toString(), sessionID: sessionID]) { Future<Message> response ->

				if(response.succeeded()) {
					body = response.result().body()
				} else {
					System.err.println( response.cause() )
				}
				
				ltp.resume()
				
				
			}
			
		}
		
		ltp.waitNow()

		assertEquals("ok", body.status)
		
		assertEquals(sessionID, body.sessionID)
		
		body = null
		
		//expire the session now
		ResultList rl = service.query(new VitalBuilder().query{
			SELECT {
				
				value segments: ['*']
				
				node_constraint { UserSession.class }
				
				node_constraint { ((UserSession_PropertiesHelper)UserSession.props()).sessionID.equalTo(sessionID) }
				
			}
		}.toQuery())
		
		UserSession session = rl.first()
		session.timestamp = System.currentTimeMillis() - 24L*3600L*1000L
		service.save(session)
		
		body = null
		
		Thread.sleep(1000)
		
		ltp.delayed {
			
			ltp.vertx.eventBus().send(VitalAuthManager.address_authorise, [appID: app.appID.toString(), sessionID: sessionID]) { Future<Message> response ->

				if(response.succeeded()) {
					body = response.result().body()
				} else {
					System.err.println( response.cause() )
				}
				
				ltp.resume()
				
				
			}
			
		}
		
		ltp.waitNow()
	
		assertEquals("error_denied", body.status)
		
	}
	
}
