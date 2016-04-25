package ai.vital.service.admin.vertx

import io.vertx.core.AsyncResult
import io.vertx.core.json.JsonObject
import junit.framework.TestCase
import ai.vital.service.admin.vertx3.VitalServiceAdminVertx3
import ai.vital.service.vertx3.test.LocalTestPlatform

/**
 * Base class for vitalservice admin vertx tests, provides a local vertx platform
 * @author Derek
 *
 */
abstract class AbstractVitalServiceAdminVertxTest extends TestCase {

	LocalTestPlatform ltp
	
	@Override
	protected void setUp() throws Exception {

		ltp = LocalTestPlatform.newInstance { LocalTestPlatform ltp ->
			
			deployVerticle(ltp)
			
		}
		
	}
	
	/**
	 * Deploys all verticles, must call resume() when everything's complete, all exceptions should be caught saved into ex variable
	 *
	 */
	void deployVerticle(LocalTestPlatform ltp) {
		
		JsonObject cfg = new JsonObject()
		cfg.put("key", "admi-admi-admi")
		
		ltp.vertx.deployVerticle("groovy:" + VitalServiceAdminVertx3.class.canonicalName, [config: cfg, worker: true, instances: 1, config: cfg]) { AsyncResult<String> asyncResult ->
			
			if (asyncResult.succeeded()) {
				println("Vital Service Admin deployment ID is " + asyncResult.result());
			} else {
				ltp.ex = asyncResult.cause()
				ltp.ex.printStackTrace()
			}
			
			ltp.resume()
				
		}
		
	}
	
	
	@Override
	protected void tearDown() throws Exception {

		if(ltp != null) ltp.destroyPlatform()
		
		ltp = null
		
	}
	
}
