package ai.vital.service.admin.vertx

import groovy.lang.Closure;
import junit.framework.TestCase;

import org.vertx.java.core.AsyncResult
import org.vertx.java.core.AsyncResultHandler
import org.vertx.java.core.json.JsonObject

import ai.vital.service.vertx.test.LocalTestPlatform;


/**
 * Base class for vitalservice admin vertx module tests, provides a local vertx platform
 * @author Derek
 *
 */
abstract class AbstractVitalServiceAdminVertxTest extends TestCase {

	LocalTestPlatform ltp
	
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
		
		JsonObject cfg = new JsonObject()
		cfg.putString("key", "admi-admi-admi")
		
		ltp.pm.deployModule("vital-ai~vitalserviceadmin-vertx-mod~0.2.303", cfg, 1, new AsyncResultHandler<String>() {
			public void handle(AsyncResult<String> asyncResult) {
				if (asyncResult.succeeded()) {
					println("Vital Service Admin deployment ID is " + asyncResult.result());
				} else {
					ltp.ex = asyncResult.cause()
					ltp.ex.printStackTrace()
				}
				
				ltp.resume()
				
			}
		});
		
	}
	
	
	@Override
	protected void tearDown() throws Exception {

		if(ltp != null) ltp.destroyPlatform()
		
		ltp = null
		
	}
	
}
