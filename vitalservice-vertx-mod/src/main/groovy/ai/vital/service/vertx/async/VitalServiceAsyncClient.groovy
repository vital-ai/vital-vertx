package ai.vital.service.vertx.async

import org.apache.commons.lang3.SerializationUtils;
import org.vertx.groovy.core.Vertx
import org.vertx.groovy.core.eventbus.Message

import ai.vital.service.vertx.VitalServiceMod;
import ai.vital.service.vertx.binary.PayloadMessage
import ai.vital.service.vertx.binary.ResponseMessage;
import ai.vital.vitalservice.ServiceOperations
import ai.vital.vitalservice.Transaction
import ai.vital.vitalservice.query.VitalPathQuery
import ai.vital.vitalservice.query.VitalQuery
import ai.vital.vitalservice.segment.VitalSegment
import ai.vital.vitalsigns.java.VitalJavaSerializationUtils;
import ai.vital.vitalsigns.meta.GraphContext
import ai.vital.vitalsigns.model.GraphObject
import ai.vital.vitalsigns.model.VITAL_Event
import ai.vital.vitalsigns.model.container.GraphObjectsIterable
import ai.vital.vitalsigns.model.property.URIProperty

/**
 * Asynchronous version of vital service on vertx
 * All closures are called with {@link ai.vital.service.vertx.binary.ResponseMessage}
 * @author Derek
 *
 */
class VitalServiceAsyncClient {

	Vertx vertx
	
	VitalServiceAsyncClient(Vertx vertx) {
		if(vertx == null) throw new NullPointerException("Null Vertx instance")
		this.vertx = vertx
	}
	
	private void impl(Closure closure, String method, List args) {
		
		vertx.eventBus.send(VitalServiceMod.ADDRESS, SerializationUtils.serialize(new PayloadMessage(method, args))) { Message response ->
		
		ResponseMessage res = VitalJavaSerializationUtils.deserialize( response.body() )
		
		closure(res)
		
		}
		
	}
	
	
//	bulkExport(VitalSegment, OutputStream)
//	bulkImport(VitalSegment, InputStream)
	
	void callFunction(String function, Map<String, Object> arguments, Closure closure) {
		impl(closure, 'callFunction', [function, arguments])
	}
	
	void close(Closure closure) {
		impl(closure, 'close', [])
	}
	
	void commitTransaction(Transaction transaction, Closure closure) {
		impl(closure, 'commitTransaction', [transaction])
	}
	
	void createTransaction(Closure closure) {
		impl(closure, 'createTransaction', [])
	}

	void delete(List<URIProperty> uris, Closure closure) {
		impl(closure, 'delete', [uris]) 
	}
	
	void delete(URIProperty uri, Closure closure) {
		impl(closure, 'delete', [uri])
	}

	void deleteExpanded(List<URIProperty> uris, VitalPathQuery pathQuery, Closure closure) {
		impl(closure, 'deleteExpanded', [uris, pathQuery])
	}
	
	void deleteExpanded(URIProperty uri, Closure closure) {
		impl(closure, 'deleteExpanded', [uri])
	}
	
	
	void deleteExpanded(URIProperty uri, VitalPathQuery pathQuery, Closure closure) {
		impl(closure, 'deleteExpanded', [uri, pathQuery])
	}
	
	void deleteExpandedObject(GraphObject graphObject, Closure closure) {
		impl(closure, 'deleteExpandedObject', [graphObject])
	}
	
	void deleteExpandedObjects(List<GraphObject> graphObjects, VitalPathQuery pathQuery, Closure closure) {
		impl(closure, 'deleteExpandedObjects', [graphObjects, pathQuery])
	}
	
//	void deleteFile(URIProperty uri, String name, Closure closure) {
//		impl(closure, 'deleteFile', [uri, name])
//	}
	
	void deleteObject(GraphObject graphObject, Closure closure) {
		impl(closure, 'deleteObject', [graphObject])
	}
	
	void deleteObjects(List<GraphObject> graphObjects, Closure closure) {
		impl(closure, 'deleteObjects', [graphObjects])
	}
	
	void doOperations(ServiceOperations serviceOps, Closure closure) {
		impl(closure, 'doOperations', [serviceOps])
	}
	
//	void downloadFile(URIProperty uri, String name, OutputStream outputStream, boolean closeOutputStream, Closure closure)
	
//	void fileExists(URIProperty uri, String name, Closure closure) {
//		impl(closure, 'fileExists', [uri, name])
//	}
	
	void generateURI(Class<? extends GraphObject> clazz, Closure closure) {
		impl(closure, 'generateURI', [clazz])
	}
	
	void get(GraphContext graphContext, List<URIProperty> uris, Closure closure) {
		impl(closure, 'get', [graphContext, uris])
	}
	
	void get(GraphContext graphContext, List<URIProperty> uris, boolean cache, Closure closure) {
		impl(closure, 'get', [graphContext, uris, cache])
	}
	
	
//	void get(GraphContext graphContext, List<URIProperty> uris, List<GraphObjectsIterable> containers, Closure closure) {
//		impl(closure, 'get', [graphContext, uris, containers])
//	}
	
	
	void get(GraphContext graphContext, URIProperty uri, Closure closure) {
		impl(closure, 'get', [graphContext, uri])
	} 
	
	void get(GraphContext graphContext, URIProperty uri, boolean cache, Closure closure) {
		impl(closure, 'get', [graphContext, uri, cache])
	}
	
//	void get(GraphContext graphContext, URIProperty uri, List<GraphObjectsIterable> containers, Closure closure) {
//		impl(closure, 'get', [graphContext, uri, containers])
//	}
	
	void getApp(Closure closure) {
		impl(closure, 'getApp', [])
	}
	
	void getDefaultSegmentName(Closure closure) {
		impl(closure, 'getDefaultSegmentName', [])
	}
	
	void getEndpointType(Closure closure) {
		impl(closure, 'getEndpointType', [])
	}
	
	void getExpanded(URIProperty uri, boolean cache, Closure closure) {
		impl(closure, 'getExpanded', [uri, cache])
	}
	
	void getExpanded(URIProperty uri, VitalPathQuery pathQuery, boolean cache, Closure closure) {
		impl(closure, 'getExpanded', [uri, pathQuery, cache])
	}
	
	void getOrganization(Closure closure) {
		impl(closure, 'getOrganization', [])
	}
	
	void getTransactions(Closure closure) {
		impl(closure, 'getTransactions', [])
	}
	
	void insert(VitalSegment segment, GraphObject graphObject, Closure closure) {
		impl(closure, 'insert', [segment, graphObject])
	}
	
	void insert(VitalSegment segment, List<GraphObject> graphObjects, Closure closure) {
		impl(closure, 'insert', [segment, graphObjects])
	}
	
//	void listFiles(String filePath, Closure closure) {
//		impl(closure, 'listFiles', [filePath])
//	}
	
	void listSegments(Closure closure) {
		impl(closure, 'listSegments', [])
	}
	
	void ping(Closure closure) {
		impl(closure, 'ping', [])
	}
	
	void query(VitalQuery query, Closure closure) {
		impl(closure, 'query', [query])
	}
	
//	queryContainers(VitalQuery, List<GraphObjectsIterable>, Closure closure)
	
	void queryLocal(VitalQuery query, Closure closure) {
		impl(closure, 'queryLocal', [query])
	}
	
	void rollbackTransaction(Transaction transaction, Closure closure) {
		impl(closure, 'rollbackTransaction', [transaction])
	}
	
	void save(GraphObject graphObject, Closure closure) {
		impl(closure, 'save', [graphObject])
	}
	
	void save(List<GraphObject> graphObjects, Closure closure) {
		impl(closure, 'save', [graphObjects])
	}
	
	void save(VitalSegment segment, GraphObject graphObject, boolean create, Closure closure) {
		impl(closure, 'save', [segment, graphObject, create])
	}
	
	void save(VitalSegment segment, List<GraphObject> graphObjects, boolean create, Closure closure) {
		impl(closure, 'save', [segment, graphObjects, create])
	}
	
	void sendEvent(VITAL_Event event, boolean waitForDelivery, Closure closure) {
		impl(closure, 'sendEvent', [event, waitForDelivery])
	}
	
	
	void sendEvents(List<VITAL_Event> events, boolean waitForDelivery, Closure closure) {
		impl(closure, 'sendEvents', [events, waitForDelivery])
	}
	
	void setDefaultSegmentName(String defaultSegment, Closure closure) {
		impl(closure, 'setDefaultSegmentName', [defaultSegment])
	}
	
	void setTransaction(Transaction transaction, Closure closure) {
		impl(closure, 'setTransaction', [transaction])
	}
	
//	uploadFile(URIProperty, String, InputStream, boolean, Closure closure)
	
	void validate(Closure closure) {
		impl(closure, 'validate', [])
	}
	
}
