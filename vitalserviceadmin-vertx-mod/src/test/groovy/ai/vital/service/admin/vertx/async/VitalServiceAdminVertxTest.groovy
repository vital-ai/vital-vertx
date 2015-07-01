package ai.vital.service.admin.vertx.async

import ai.vital.service.admin.vertx.AbstractVitalServiceAdminVertxTest;
import ai.vital.service.vertx.binary.ResponseMessage;

class VitalServiceAdminVertxTest extends AbstractVitalServiceAdminVertxTest {

	VitalServiceAdminAsyncClient asyncClient
	
	@Override
	protected void setUp() throws Exception {
		
		super.setUp();
		
		asyncClient = new VitalServiceAdminAsyncClient(ltp.vertx)
		 
	}
	
	public void testVitalServiceAdminAsyncClient() {
		
		ResponseMessage r = null
		
		ltp.delayed { ->
			asyncClient.ping { ResponseMessage m ->
				
				r = m
				ltp.resume()
			
			}
		}
		
		ltp.waitNow()
		
		assertNotNull(r)
		assertNotNull(r.response)
		
		
		r = null
		ltp.delayed {
			asyncClient.listApps { ResponseMessage m ->
				
				r = m
				ltp.resume() 
				
			}
		}
		
		ltp.waitNow()
		
		assertNotNull(r)
		assertNotNull(r.response)
		assertTrue(r.response instanceof List)
		
	}
	
}
