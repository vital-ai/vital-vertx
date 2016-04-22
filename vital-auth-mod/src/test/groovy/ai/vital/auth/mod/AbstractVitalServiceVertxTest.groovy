package ai.vital.auth.mod

import groovy.lang.Closure;
import junit.framework.TestCase;

import org.vertx.groovy.core.Vertx;
import org.vertx.java.core.AsyncResult
import org.vertx.java.core.AsyncResultHandler
import org.vertx.java.core.json.JsonObject
import org.vertx.java.platform.PlatformLocator;
import org.vertx.java.platform.PlatformManager;

import ai.vital.service.vertx.test.LocalTestPlatform;

import ai.vital.service.vertx.test.LocalTestPlatform;


/**
 * Base class for vitalservice vertx module tests, provides a local vertx platform
 * @author Derek
 *
 */
abstract class AbstractVitalServiceVertxTest extends TestCase {

	LocalTestPlatform ltp
	
//	public void testNop() {}
	
	@Override
	protected void setUp() throws Exception {

		ltp = LocalTestPlatform.newInstance { LocalTestPlatform ltp ->
			
			deployModules(ltp)
			
		}
		
	}
	
	/**
	 * Deploys all modules, must call resume() when everything's complete, all exceptions should be caught saved into ex variable
	 *
	 */
	void deployModules(LocalTestPlatform ltp) {
		
		Map cfgMap = [
			services: [[
				key: 'aaaa-aaaa-aaaa',
				profile: 'default'
			]]
		]
		
		JsonObject cfg = new JsonObject(cfgMap);
		
		ltp.pm.deployModule("vital-ai~vitalservice-vertx-mod~0.2.303", cfg, 1, new AsyncResultHandler<String>() {
			public void handle(AsyncResult<String> asyncResult) {
				if (asyncResult.succeeded()) {
					println("Vital Service deployment ID is " + asyncResult.result());
				} else {
					ltp.ex = asyncResult.cause()
					ltp.ex.printStackTrace()
				}
				
				ltp.resume()
				
			}
		});
	
//		ltp.waitNow()
	
	}
	
	
	@Override
	protected void tearDown() throws Exception {

		if(ltp != null) {
			ltp.destroyPlatform()
		}
		ltp = null
		
	}

}
