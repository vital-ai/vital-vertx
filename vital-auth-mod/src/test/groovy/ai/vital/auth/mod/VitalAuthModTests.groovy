package ai.vital.auth.mod

import org.apache.commons.io.FileUtils;
import org.githubusercontent.defuse.passwordhash.PasswordHash;
import org.vertx.groovy.core.eventbus.Message
import org.vertx.java.core.AsyncResult
import org.vertx.java.core.AsyncResultHandler
import org.vertx.java.core.json.JsonObject

import ai.vital.domain.CredentialsLogin;
import ai.vital.domain.Login;
import ai.vital.lucene.disk.service.config.VitalServiceLuceneDiskConfig;
import ai.vital.mock.service.VitalServiceMock;
import ai.vital.service.vertx.AbstractVitalServiceVertxTest;
import ai.vital.service.vertx.VitalServiceMod;
import ai.vital.vitalservice.VitalService;
import ai.vital.vitalservice.admin.VitalServiceAdmin;
import ai.vital.vitalservice.factory.VitalServiceFactory;
import ai.vital.vitalservice.json.VitalServiceJSONMapper;
import ai.vital.vitalsigns.model.VitalApp
import ai.vital.vitalsigns.model.VitalSegment;
import ai.vital.vitalsigns.model.VitalServiceAdminKey;
import ai.vital.vitalsigns.model.VitalServiceKey;
import ai.vital.vitalsigns.model.VitalServiceRootKey;
import junit.framework.TestCase;

class VitalAuthModTests extends AbstractVitalServiceVertxTest {

	private File luceneRoot
	
	protected Login login
	
	@Override
	protected void setUp() throws Exception {
		
		VitalApp app = VitalApp.withId("app")
		
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
		VitalService service = VitalServiceFactory.openService(serviceKey, cfg, VitalServiceMod.SERVICE_NAME)
		
		login = new Login().generateURI(app)
		login.username = "test"
		login.password = PasswordHash.createHash("pass")
		login.active = true
		login.emailVerified = true
		
		
		service.save(loginsSegment, login, true)
		
		
		super.setUp();
		
		//once service mod is up setup the auth mod
		
		JsonObject modCfg = new JsonObject([
			loginsSegment: loginsSegment.segmentID.toString(),
			persistentSessions: true,
			maxSessionsPerUser: 3,
			expirationProlongMargin: 10000
		])
		
		ltp.delayed { ->
		
			ltp.pm.deployModule("vital-ai~vital-auth-mod~0.2.300", modCfg, 1, new AsyncResultHandler<String>() {
				
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
			
			ltp.vertx.eventBus.send(VitalAuthManager.address_login, [type: Login.class.simpleName, username: 'test', password: 'pass']) { Message response ->
				
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
			
			ltp.vertx.eventBus.send(VitalAuthManager.address_authorise, [sessionID: sessionID]) { Message response ->

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
			
			ltp.vertx.eventBus.send(VitalAuthManager.address_logout, [type: Login.class.simpleName, sessionID: sessionID]) { Message response ->
				
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
			
			ltp.vertx.eventBus.send(VitalAuthManager.address_login, [type: Login.class.simpleName, username: 'test', password: 'passwrong']) { Message response ->
				
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
			
			ltp.vertx.eventBus.send(VitalAuthManager.address_authorise, [sessionID: 'Login_111']) { Message response ->

				body = response.body()
				
				ltp.resume()
				
			}
			
		}
		
		ltp.waitNow()
		
		assertEquals(VitalAuthManager.error_denied, body.status)
		
	}
}
