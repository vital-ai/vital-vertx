package ai.vital.service.admin.vertx3.async

import groovy.lang.Closure
import io.vertx.core.AsyncResult;
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message
import java.security.Provider.ServiceKey;

import org.apache.commons.lang3.SerializationUtils;

import ai.vital.query.Node
import ai.vital.service.admin.vertx3.VitalServiceAdminVertx3;
import ai.vital.service.vertx3.binary.PayloadMessage
import ai.vital.service.vertx3.binary.ResponseMessage
import ai.vital.vitalservice.ServiceOperations
import ai.vital.vitalservice.query.VitalPathQuery
import ai.vital.vitalservice.query.VitalQuery
import ai.vital.vitalsigns.java.VitalJavaSerializationUtils;
import ai.vital.vitalsigns.meta.GraphContext
import ai.vital.vitalsigns.model.GraphObject
import ai.vital.vitalsigns.model.VITAL_Event
import ai.vital.vitalsigns.model.VitalApp
import ai.vital.vitalsigns.model.VitalProvisioning;
import ai.vital.vitalsigns.model.VitalSegment
import ai.vital.vitalsigns.model.VitalServiceKey
import ai.vital.vitalsigns.model.VitalTransaction
import ai.vital.vitalsigns.model.container.GraphObjectsIterable
import ai.vital.vitalsigns.model.property.URIProperty
import static ai.vital.vitalservice.VitalServiceConstants.NO_TRANSACTION

/**
 * Asynchronous version of vital service on vertx
 * All closures are called with {@link ai.vital.service.vertx.binary.ResponseMessage}
 * @author Derek
 *
 */
class VitalServiceAdminAsyncClient extends VitalServiceAdminAsyncClientBase {

	String overriddenAddress = null;
	
	VitalServiceAdminAsyncClient(Vertx vertx) {
		super(vertx)
	}
	
	@Override
	protected void impl(Closure closure, String method, List args) {
		
		String address = overriddenAddress ? overriddenAddress : VitalServiceAdminVertx3.ADDRESS
		
		vertx.eventBus().send(address, SerializationUtils.serialize(new PayloadMessage(method, args))) { Future<Message> response ->
		
			ResponseMessage res = null
			
			if(response.succeeded()) {
				
				res = VitalJavaSerializationUtils.deserialize( response.result().body() )
				
			} else {
			
				res = new ResponseMessage()
				Throwable t = response.cause()
				if(t != null) {
					res.exceptionType = t.getClass().getCanonicalName()
					res.exceptionMessage = t.getLocalizedMessage()
				} else {
					res.exceptionType = 'unknown_exception'
					res.exceptionMessage = 'unknown error'
				}
				
			
			}
		
			closure(res)
			
		}
		
	}
	
}
