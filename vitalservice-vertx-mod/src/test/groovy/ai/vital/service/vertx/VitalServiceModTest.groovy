package ai.vital.service.vertx

import junit.framework.TestCase

import org.apache.commons.lang3.SerializationUtils;
import org.vertx.groovy.core.Vertx;
import org.vertx.groovy.core.eventbus.Message;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.json.JsonObject
import org.vertx.java.platform.PlatformLocator
import org.vertx.java.platform.PlatformManager

import ai.vital.service.vertx.binary.PayloadMessage
import ai.vital.service.vertx.binary.ResponseMessage;
import ai.vital.service.vertx.json.VitalServiceJSONMapper;
import ai.vital.vitalservice.VitalStatus;
import ai.vital.vitalsigns.java.VitalJavaSerializationUtils;


class VitalServiceModTest extends AbstractVitalServiceVertxTest {

	public void testJsonTransport() {
		
		Map r = null
		
		ltp.delayed { ->
			ltp.vertx.eventBus.send(VitalServiceMod.ADDRESS, ['method': 'ping', 'args': []]) { Message response ->
				
				r = response.body
			
				ltp.resume()	
			}
		}
		
		ltp.waitNow()
		
		assertNotNull(r)
		
		assertEquals("ok", r.status)
		
		def status = VitalServiceJSONMapper.fromJSON(r.response)
		
		assertTrue(status instanceof VitalStatus)
		
		assertEquals(VitalStatus.Status.ok, status.status)
		
		r = null
		
		ltp.delayed { ->
			ltp.vertx.eventBus.send(VitalServiceMod.ADDRESS, ['method': 'ping2', 'args': []]) { Message response ->
				
				r = response.body
			
				ltp.resume()
			}
		}
		
		ltp.waitNow()
		
		assertNotNull(r)
		
		assertEquals("error", r.status)
		
		assertTrue(r.exception)
		
	}
	
	public void testBinaryTransport() {
		
		PayloadMessage payload = new PayloadMessage("ping", [])  
		
		def res = null
		
		ltp.delayed { ->
			ltp.vertx.eventBus.send(VitalServiceMod.ADDRESS, SerializationUtils.serialize(payload)) { Message response ->
				
				res = VitalJavaSerializationUtils.deserialize( response.body() )
			
				ltp.resume()	
			}
		}
				
		ltp.waitNow()
		
		assertTrue(res instanceof ResponseMessage)
		
		assertNotNull(res.response)
		assertNull(res.exceptionMessage)
		assertNull(res.exceptionType)
		
		
		res = null
		
		ltp.delayed { ->
			//call non-existing method or some other exception
			ltp.vertx.eventBus.send(VitalServiceMod.ADDRESS, SerializationUtils.serialize(new PayloadMessage("pingXXX", []))) { Message response ->
				
				res = VitalJavaSerializationUtils.deserialize( response.body() )
				
				ltp.resume()
			}
		}
		
		ltp.waitNow()
		
		assertTrue(res instanceof ResponseMessage)
		assertNull(res.response)
		assertNotNull(res.exceptionMessage)
		assertNotNull(res.exceptionType)
		
		println "DONE"
		
	}
	
}
