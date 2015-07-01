package ai.vital.service.admin.vertx.async

import org.apache.commons.lang3.SerializationUtils;
import org.vertx.groovy.core.Vertx
import org.vertx.groovy.core.eventbus.Message

import ai.vital.service.admin.vertx.VitalServiceAdminMod;
import ai.vital.service.vertx.VitalServiceMod;
import ai.vital.service.vertx.binary.PayloadMessage
import ai.vital.service.vertx.binary.ResponseMessage;
import ai.vital.vitalservice.ServiceOperations
import ai.vital.vitalservice.Transaction
import ai.vital.vitalservice.model.App
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
class VitalServiceAdminAsyncClient {

	Vertx vertx
	
	VitalServiceAdminAsyncClient(Vertx vertx) {
		if(vertx == null) throw new NullPointerException("Null Vertx instance")
		this.vertx = vertx
	}
	
	private void impl(Closure closure, String method, List args) {
		
		vertx.eventBus.send(VitalServiceAdminMod.ADDRESS, SerializationUtils.serialize(new PayloadMessage(method, args))) { Message response ->
		
			ResponseMessage res = VitalJavaSerializationUtils.deserialize( response.body() )
		
			closure(res)
		
		}
		
	}
	
	
	void addApp(App app, Closure closure) {
		impl(closure, 'addApp', [app])
	}
	
	void addSegment(App app, VitalSegment segment, boolean createIfNotExists, Closure closure) {
		impl(closure, 'addSegment', [app, segment, createIfNotExists])
	} 

	//	bulkExport(App, VitalSegment, OutputStream)
	//	bulkImport(App, VitalSegment, InputStream)
		
	void callFunction(App app, String function, Map<String, Object> arguments, Closure closure) {
		impl(closure, 'callFunction', [app, function, arguments])
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

	void delete(App app, List<URIProperty> uris, Closure closure) {
		impl(closure, 'delete', [app, uris]) 
	}
	
	void delete(App app, URIProperty uri, Closure closure) {
		impl(closure, 'delete', [app, uri])
	}

	void deleteExpanded(App app, List<URIProperty> uris, VitalPathQuery pathQuery, Closure closure) {
		impl(closure, 'deleteExpanded', [uris, pathQuery])
	}
	
	void deleteExpanded(App app, URIProperty uri, Closure closure) {
		impl(closure, 'deleteExpanded', [app, uri])
	}
	
	
	void deleteExpanded(App app, URIProperty uri, VitalPathQuery pathQuery, Closure closure) {
		impl(closure, 'deleteExpanded', [app, uri, pathQuery])
	}
	
	void deleteExpandedObject(App app, GraphObject graphObject, Closure closure) {
		impl(closure, 'deleteExpandedObject', [app, graphObject])
	}
	
	void deleteExpandedObjects(App app, List<GraphObject> graphObjects, VitalPathQuery pathQuery, Closure closure) {
		impl(closure, 'deleteExpandedObjects', [app, graphObjects, pathQuery])
	}
	
//	void deleteFile(App app, URIProperty uri, String name, Closure closure) {
//		impl(closure, 'deleteFile', [app, uri, name])
//	}
	
	void deleteObject(App app, GraphObject graphObject, Closure closure) {
		impl(closure, 'deleteObject', [app, graphObject])
	}
	
	void deleteObjects(App app, List<GraphObject> graphObjects, Closure closure) {
		impl(closure, 'deleteObjects', [app, graphObjects])
	}
	
	void doOperations(App app, ServiceOperations serviceOps, Closure closure) {
		impl(closure, 'doOperations', [app, serviceOps])
	}
	
//	void downloadFile(App app, URIProperty uri, String name, OutputStream outputStream, boolean closeOutputStream, Closure closure)
	
//	void fileExists(App app, URIProperty uri, String name, Closure closure) {
//		impl(closure, 'fileExists', [app, uri, name])
//	}
	
	void generateURI(App app, Class<? extends GraphObject> clazz, Closure closure) {
		impl(closure, 'generateURI', [app, clazz])
	}
	
	void get(App app, GraphContext graphContext, List<URIProperty> uris, Closure closure) {
		impl(closure, 'get', [app, graphContext, uris])
	}
	
	void get(App app, GraphContext graphContext, List<URIProperty> uris, boolean cache, Closure closure) {
		impl(closure, 'get', [app, graphContext, uris, cache])
	}
	
	
//	void get(App app, GraphContext graphContext, List<URIProperty> uris, List<GraphObjectsIterable> containers, Closure closure) {
//		impl(closure, 'get', [app, graphContext, uris, containers])
//	}
	
	
	void get(App app, GraphContext graphContext, URIProperty uri, Closure closure) {
		impl(closure, 'get', [app, graphContext, uri])
	} 
	
	void get(App app, GraphContext graphContext, URIProperty uri, boolean cache, Closure closure) {
		impl(closure, 'get', [app, graphContext, uri, cache])
	}
	
//	void get(App app, GraphContext graphContext, URIProperty uri, List<GraphObjectsIterable> containers, Closure closure) {
//		impl(closure, 'get', [app, graphContext, uri, containers])
//	}
	
	void getEndpointType(Closure closure) {
		impl(closure, 'getEndpointType', [])
	}
	
	void getExpanded(App app, URIProperty uri, boolean cache, Closure closure) {
		impl(closure, 'getExpanded', [app, uri, cache])
	}
	
	void getExpanded(App app, URIProperty uri, VitalPathQuery pathQuery, boolean cache, Closure closure) {
		impl(closure, 'getExpanded', [app, uri, pathQuery, cache])
	}
	
	void getOrganization(Closure closure) {
		impl(closure, 'getOrganization', [])
	}
	
	void getTransactions(Closure closure) {
		impl(closure, 'getTransactions', [])
	}
	
	void insert(App app, VitalSegment segment, GraphObject graphObject, Closure closure) {
		impl(closure, 'insert', [app, segment, graphObject])
	}
	
	void insert(App app, VitalSegment segment, List<GraphObject> graphObjects, Closure closure) {
		impl(closure, 'insert', [app, segment, graphObjects])
	}
	
	void listApps(Closure closure) {
		impl(closure, 'listApps', [])
	} 
	
//	void listFiles(App app, String filePath, Closure closure) {
//		impl(closure, 'listFiles', [app, filePath])
//	}
	
	void listSegments(App app, Closure closure) {
		impl(closure, 'listSegments', [app])
	}
	
	void ping(Closure closure) {
		impl(closure, 'ping', [])
	}
	
	void query(App app, VitalQuery query, Closure closure) {
		impl(closure, 'query', [app, query])
	}
	
//	queryContainers(VitalQuery, List<GraphObjectsIterable>, Closure closure)
	
	void queryLocal(App app, VitalQuery query, Closure closure) {
		impl(closure, 'queryLocal', [app, query])
	}
	
	void removeApp(App app, Closure closure) {
		impl(closure, 'removeApp',[app])
	}
	
	void removeSegment(App app, VitalSegment segment, boolean deleteData, Closure closure) {
		impl(closure, 'removeSegment', [app, segment, deleteData])	
	} 
	
	void rollbackTransaction(Transaction transaction, Closure closure) {
		impl(closure, 'rollbackTransaction', [transaction])
	}
	
	void save(App app, GraphObject graphObject, Closure closure) {
		impl(closure, 'save', [app, graphObject])
	}
	
	void save(App app, List<GraphObject> graphObjects, Closure closure) {
		impl(closure, 'save', [app, graphObjects])
	}
	
	void save(App app, VitalSegment segment, GraphObject graphObject, boolean create, Closure closure) {
		impl(closure, 'save', [app, segment, graphObject, create])
	}
	
	void save(App app, VitalSegment segment, List<GraphObject> graphObjects, boolean create, Closure closure) {
		impl(closure, 'save', [app, segment, graphObjects, create])
	}
	
	void sendEvent(App app, VITAL_Event event, boolean waitForDelivery, Closure closure) {
		impl(closure, 'sendEvent', [app, event, waitForDelivery])
	}
	
	
	void sendEvents(App app, List<VITAL_Event> events, boolean waitForDelivery, Closure closure) {
		impl(closure, 'sendEvents', [app, events, waitForDelivery])
	}
	
	void setTransaction(Transaction transaction, Closure closure) {
		impl(closure, 'setTransaction', [transaction])
	}
	
//	uploadFile(App, URIProperty, String, InputStream, boolean, Closure closure)
	
	void validate(Closure closure) {
		impl(closure, 'validate', [])
	}
	
}
