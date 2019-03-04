package ai.vital.service.admin.vertx

import io.vertx.core.Future
import io.vertx.core.eventbus.Message
import org.apache.commons.lang3.SerializationUtils

import ai.vital.service.admin.vertx3.VitalServiceAdminVertx3
import ai.vital.service.vertx3.binary.PayloadMessage
import ai.vital.service.vertx3.binary.ResponseMessage
import ai.vital.vitalservice.VitalStatus
import ai.vital.vitalservice.json.VitalServiceJSONMapper
import ai.vital.vitalsigns.java.VitalJavaSerializationUtils



class VitalServiceAdminModTest extends AbstractVitalServiceAdminVertxTest {

	public void testJsonTransport() {
		
		Map r = null
		
		ltp.delayed { ->
			ltp.vertx.eventBus().send(VitalServiceAdminVertx3.ADDRESS, ['method': 'ping', 'args': []]) { Future<Message> response ->
				
				if(response.succeeded()) {
					r = response.result().body()
				} else {
					System.err.println( response.cause() )
				}
			
				ltp.resume()	
					
			}
		}
		
		ltp.waitNow()
		
		assertNotNull(r)
		
		assertEquals(r.message, "ok", r.status)
		
		def status = VitalServiceJSONMapper.fromJSON(r.response)
		
		assertTrue(status instanceof VitalStatus)
		
		assertEquals(VitalStatus.Status.ok, status.status)
		
		r = null
		
		ltp.delayed { ->
			ltp.vertx.eventBus().send(VitalServiceAdminVertx3.ADDRESS, ['method': 'ping2', 'args': []]) { Future<Message> response ->
				
				if(response.succeeded()) {
					r = response.result().body()
				} else {
					System.err.println( response.cause() )
				}
			
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
			ltp.vertx.eventBus().send(VitalServiceAdminVertx3.ADDRESS, SerializationUtils.serialize(payload)) { Future<Message> response ->
				
				if(response.succeeded()) {
					res = VitalJavaSerializationUtils.deserialize( response.result().body() )
				} else {
					System.err.println( response.cause() )
				}
			
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
			ltp.vertx.eventBus().send(VitalServiceAdminVertx3.ADDRESS, SerializationUtils.serialize(new PayloadMessage("pingXXX", []))) { Future<Message> response ->
				
				if(response.succeeded()) {
					res = VitalJavaSerializationUtils.deserialize( response.result().body() )
				} else {
					System.err.println( response.cause() )
				}
			
				ltp.resume()
				
			}
		}
		
		ltp.waitNow()
		
		assertTrue(res instanceof ResponseMessage)
		assertNull(res.response)
		assertNotNull(res.exceptionMessage)
		assertNotNull(res.exceptionType)
		
		
		
		res = null
		
		ltp.delayed { ->
			
			
			ltp.vertx.eventBus().send(VitalServiceAdminVertx3.ADDRESS, SerializationUtils.serialize(new PayloadMessage("listApps", []))) { Future<Message> response ->
				
				if(response.succeeded()) {
					res = VitalJavaSerializationUtils.deserialize( response.result().body() )
				} else {
					System.err.println( response.cause() )
				}
			
				ltp.resume()
				
			}
		}
		
		ltp.waitNow()
		
		assertTrue(res instanceof ResponseMessage)
		assertNotNull(res.response)
		assertTrue(res.response instanceof List)
		
		
		println "DONE"
		
	}
	
}
