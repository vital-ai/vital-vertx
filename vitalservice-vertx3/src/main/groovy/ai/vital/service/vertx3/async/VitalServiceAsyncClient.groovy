package ai.vital.service.vertx3.async

import groovy.json.JsonOutput
import groovy.json.JsonSlurper;
import groovy.lang.Closure
import io.vertx.core.Future;
import io.vertx.core.Handler
import io.vertx.groovy.core.Vertx
import io.vertx.groovy.core.buffer.Buffer
import io.vertx.groovy.core.eventbus.Message
import io.vertx.groovy.core.http.HttpClient
import io.vertx.groovy.core.http.WebSocket
import io.vertx.groovy.core.http.WebSocketFrame;

import org.apache.commons.lang3.SerializationUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory;

import ai.vital.query.Node
import ai.vital.service.vertx3.binary.PayloadMessage
import ai.vital.service.vertx3.binary.ResponseMessage
import ai.vital.service.vertx3.VitalServiceVertx3;
import ai.vital.vitalservice.ServiceOperations
import ai.vital.vitalservice.VitalStatus
import ai.vital.vitalservice.json.VitalServiceJSONMapper;

import static ai.vital.vitalservice.VitalServiceConstants.NO_TRANSACTION;
import ai.vital.vitalservice.query.VitalPathQuery
import ai.vital.vitalservice.query.VitalQuery
import ai.vital.vitalsigns.java.VitalJavaSerializationUtils
import ai.vital.vitalsigns.json.JSONSerializer;
import ai.vital.vitalsigns.meta.GraphContext
import ai.vital.vitalsigns.model.GraphObject
import ai.vital.vitalsigns.model.VITAL_Event
import ai.vital.vitalsigns.model.VitalApp;
import ai.vital.vitalsigns.model.VitalSegment
import ai.vital.vitalsigns.model.VitalTransaction
import ai.vital.vitalsigns.model.container.GraphObjectsIterable
import ai.vital.vitalsigns.model.property.URIProperty

/**
 * Asynchronous version of vital service on vertx
 * All closures are called with {@link ai.vital.service.vertx.binary.ResponseMessage}
 * @author Derek
 *
 */
class VitalServiceAsyncClient extends VitalServiceAsyncClientBase {

	VitalServiceAsyncClient(Vertx vertx, VitalApp app) {
		super(vertx)
		if(app == null) throw new NullPointerException("Null app")
		String appID = app.appID?.toString()
		if(appID == null) throw new NullPointerException("Null appID")
		if( ! VitalServiceVertx3.registeredServices.containsKey(appID) ) throw new RuntimeException("Vitalservice handler for app: ${appID} not found")
		this.address = VitalServiceVertx3.ADDRESS_PREFIX + appID
	}

	
	protected VitalServiceAsyncClient() {}
	
	@Override
	protected void impl(Closure closure, String method, List args) {
	
		vertx.eventBus().send(this.address, SerializationUtils.serialize(new PayloadMessage(method, args))) { Future<Message> response ->
		
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
