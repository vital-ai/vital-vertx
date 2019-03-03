package ai.vital.auth.vertx3

import ai.vital.auth.vertx3.VitalAuthManager
import ai.vital.service.vertx3.VitalServiceVertx3;
import ai.vital.service.vertx3.test.LocalTestPlatform
import groovy.lang.Closure;
import io.vertx.core.AsyncResult
import io.vertx.core.json.JsonObject
import junit.framework.TestCase;


/**
 * Base class for vitalservice vertx tests, provides a local vertx platform
 * @author Derek
 *
 */
abstract class AbstractVitalServiceVertxTest extends TestCase {

	LocalTestPlatform ltp
	
//	public void testNop() {}
	
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
		
		Map cfgMap = [
			services: [[
				key: 'aaaa-aaaa-aaaa',
				profile: 'default'
			]]
		]
		
		JsonObject cfg = new JsonObject(cfgMap);
		
		ltp.vertx.deployVerticle("groovy:" + VitalServiceVertx3.class.canonicalName, [config: cfg, worker: true, instances: 1, config: cfg]) { AsyncResult<String> asyncResult ->
			if (asyncResult.succeeded()) {
				println("Vital Service deployment ID is " + asyncResult.result());
			} else {
				ltp.ex = asyncResult.cause()
				ltp.ex.printStackTrace()
			}
			
			ltp.resume()
				
		}
	
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
