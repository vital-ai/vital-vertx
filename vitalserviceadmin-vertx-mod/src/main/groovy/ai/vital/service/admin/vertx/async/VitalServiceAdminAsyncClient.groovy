package ai.vital.service.admin.vertx.async

import groovy.lang.Closure;

import java.security.Provider.ServiceKey;

import org.apache.commons.lang3.SerializationUtils;
import org.vertx.groovy.core.Vertx
import org.vertx.groovy.core.eventbus.Message

import ai.vital.query.Node;
import ai.vital.service.admin.vertx.VitalServiceAdminMod;
import ai.vital.service.vertx.VitalServiceMod;
import ai.vital.service.vertx.binary.PayloadMessage
import ai.vital.service.vertx.binary.ResponseMessage;
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
class VitalServiceAdminAsyncClient {

	Vertx vertx
	
	String overriddenAddress = null;
	
	VitalServiceAdminAsyncClient(Vertx vertx) {
		if(vertx == null) throw new NullPointerException("Null Vertx instance")
		this.vertx = vertx
	}
	
	private void impl(Closure closure, String method, List args) {
		
		String address = overriddenAddress ? overriddenAddress : VitalServiceAdminMod.ADDRESS
		
		vertx.eventBus.send(address, SerializationUtils.serialize(new PayloadMessage(method, args))) { Message response ->
		
			ResponseMessage res = VitalJavaSerializationUtils.deserialize( response.body() )
		
			closure(res)
		
		}
		
	}
	
	
	void addApp(VitalApp app, Closure closure) {
		impl(closure, 'addApp', [app])
	}
	
	void addSegment(VitalApp app, VitalSegment segment, boolean createIfNotExists, Closure closure) {
		impl(closure, 'addSegment', [app, segment, null, createIfNotExists])
	}
	
	void addSegment(VitalApp app, VitalSegment segment, VitalProvisioning provisioning, boolean createIfNotExists, Closure closure) {
		impl(closure, 'addSegment', [app, segment, provisioning, createIfNotExists])
	}

	void addVitalServiceKey(VitalApp app, VitalServiceKey vitalServiceKey, Closure closure) {
		impl(closure, 'addVitalServiceKey', [app, vitalServiceKey])
	}
	
	//	bulkExport(App, VitalSegment, OutputStream)
	//	bulkImport(App, VitalSegment, InputStream)
		
	void callFunction(VitalApp app, String function, Map<String, Object> arguments, Closure closure) {
		impl(closure, 'callFunction', [app, function, arguments])
	}
	
	void close(Closure closure) {
		impl(closure, 'close', [])
	}
	
	void commitTransaction(VitalTransaction transaction, Closure closure) {
		impl(closure, 'commitTransaction', [transaction])
	}
	
	void createTransaction(Closure closure) {
		impl(closure, 'createTransaction', [])
	}

	void delete(VitalApp app, List<URIProperty> uris, Closure closure) {
		impl(closure, 'delete', [NO_TRANSACTION, app, uris]) 
	}
	
	void delete(VitalTransaction transaction, VitalApp app, List<URIProperty> uris, Closure closure) {
		impl(closure, 'delete', [transaction, app, uris]) 
	}
	
	void delete(VitalApp app, URIProperty uri, Closure closure) {
		impl(closure, 'delete', [NO_TRANSACTION, app, uri])
	}
	
	void delete(VitalTransaction transaction, VitalApp app, URIProperty uri, Closure closure) {
		impl(closure, 'delete', [transaction, app, uri])
	}

	void deleteExpanded(VitalApp app, List<URIProperty> uris, VitalPathQuery pathQuery, Closure closure) {
		impl(closure, 'deleteExpanded', [NO_TRANSACTION, uris, pathQuery])
	}
	
	void deleteExpanded(VitalTransaction transaction, VitalApp app, List<URIProperty> uris, VitalPathQuery pathQuery, Closure closure) {
		impl(closure, 'deleteExpanded', [transaction, uris, pathQuery])
	}
	
	void deleteExpanded(VitalApp app, URIProperty uri, Closure closure) {
		impl(closure, 'deleteExpanded', [NO_TRANSACTION, app, uri])
	}
	
	void deleteExpanded(VitalTransaction transaction, VitalApp app, URIProperty uri, Closure closure) {
		impl(closure, 'deleteExpanded', [transaction, app, uri])
	}
	
	
	void deleteExpanded(VitalApp app, URIProperty uri, VitalPathQuery pathQuery, Closure closure) {
		impl(closure, 'deleteExpanded', [NO_TRANSACTION, app, uri, pathQuery])
	}
	
	void deleteExpanded(VitalTransaction transaction, VitalApp app, URIProperty uri, VitalPathQuery pathQuery, Closure closure) {
		impl(closure, 'deleteExpanded', [transaction, app, uri, pathQuery])
	}
	
	void deleteExpandedObject(VitalApp app, GraphObject graphObject, Closure closure) {
		impl(closure, 'deleteExpandedObject', [NO_TRANSACTION, app, graphObject])
	}
	
	void deleteExpandedObject(VitalTransaction transaction, VitalApp app, GraphObject graphObject, Closure closure) {
		impl(closure, 'deleteExpandedObject', [transaction, app, graphObject])
	}
	
	void deleteExpandedObjects(VitalApp app, List<GraphObject> graphObjects, VitalPathQuery pathQuery, Closure closure) {
		impl(closure, 'deleteExpandedObjects', [NO_TRANSACTION, app, graphObjects, pathQuery])
	}
	
	void deleteExpandedObjects(VitalTransaction transaction, VitalApp app, List<GraphObject> graphObjects, VitalPathQuery pathQuery, Closure closure) {
		impl(closure, 'deleteExpandedObjects', [transaction, app, graphObjects, pathQuery])
	}
	
	//VitalStatus
	void deleteFile(VitalApp app, URIProperty uri, String name, Closure closure) {
		impl(closure, 'deleteFile', [app, uri, name])
	}
	
	void deleteObject(VitalApp app, GraphObject graphObject, Closure closure) {
		impl(closure, 'deleteObject', [NO_TRANSACTION, app, graphObject])
	}
	
	void deleteObject(VitalTransaction transaction, VitalApp app, GraphObject graphObject, Closure closure) {
		impl(closure, 'deleteObject', [transaction, app, graphObject])
	}
	
	void deleteObjects(VitalApp app, List<GraphObject> graphObjects, Closure closure) {
		impl(closure, 'deleteObjects', [NO_TRANSACTION, app, graphObjects])
	}
	
	void deleteObjects(VitalTransaction transaction, VitalApp app, List<GraphObject> graphObjects, Closure closure) {
		impl(closure, 'deleteObjects', [transaction, app, graphObjects])
	}
	
	void doOperations(VitalApp app, ServiceOperations serviceOps, Closure closure) {
		impl(closure, 'doOperations', [app, serviceOps])
	}
	
	//VitalStatus
	void downloadFile(VitalApp app, URIProperty uri, String name, String localFilePath, boolean closeOutputStream, Closure closure) {
		impl(closure, "downloadFile", [app, uri, name, localFilePath, closeOutputStream])
	}
	
	//VitalStatus
	void fileExists(VitalApp app, URIProperty uri, String name, Closure closure) {
		impl(closure, 'fileExists', [app, uri, name])
	}
	
	void generateURI(VitalApp app, Class<? extends GraphObject> clazz, Closure closure) {
		impl(closure, 'generateURI', [app, clazz])
	}
	
	void get(VitalApp app, GraphContext graphContext, List<URIProperty> uris, Closure closure) {
		impl(closure, 'get', [app, graphContext, uris])
	}
	
	void get(VitalApp app, GraphContext graphContext, List<URIProperty> uris, boolean cache, Closure closure) {
		impl(closure, 'get', [app, graphContext, uris, cache])
	}
	
	
//	void get(App app, GraphContext graphContext, List<URIProperty> uris, List<GraphObjectsIterable> containers, Closure closure) {
//		impl(closure, 'get', [app, graphContext, uris, containers])
//	}
	
	
	void get(VitalApp app, GraphContext graphContext, URIProperty uri, Closure closure) {
		impl(closure, 'get', [app, graphContext, uri])
	} 
	
	void get(VitalApp app, GraphContext graphContext, URIProperty uri, boolean cache, Closure closure) {
		impl(closure, 'get', [app, graphContext, uri, cache])
	}
	
//	void get(App app, GraphContext graphContext, URIProperty uri, List<GraphObjectsIterable> containers, Closure closure) {
//		impl(closure, 'get', [app, graphContext, uri, containers])
//	}
	
	
	void getApp(String appID, Closure closure) {
		impl(closure, 'getApp', [appID])
	}
	
	void getEndpointType(Closure closure) {
		impl(closure, 'getEndpointType', [])
	}
	
	void getExpanded(VitalApp app, URIProperty uri, boolean cache, Closure closure) {
		impl(closure, 'getExpanded', [app, uri, cache])
	}
	
	void getExpanded(VitalApp app, URIProperty uri, VitalPathQuery pathQuery, boolean cache, Closure closure) {
		impl(closure, 'getExpanded', [app, uri, pathQuery, cache])
	}
	
	//String
	void getName(Closure closure) {
		impl(closure, 'getName', [])
	}
	
	void getOrganization(Closure closure) {
		impl(closure, 'getOrganization', [])
	}
	
	//VitalSegment
	void getSegment(VitalApp app, String segmentID, Closure closure) {
		impl(closure, 'getSegment', [app, segmentID])
	}
	
	void getTransactions(Closure closure) {
		impl(closure, 'getTransactions', [])
	}
	
	void insert(VitalApp app, VitalSegment segment, GraphObject graphObject, Closure closure) {
		impl(closure, 'insert', [NO_TRANSACTION, app, segment, graphObject])
	}
	
	void insert(VitalTransaction transaction, VitalApp app, VitalSegment segment, GraphObject graphObject, Closure closure) {
		impl(closure, 'insert', [transaction, app, segment, graphObject])
	}
	
	void insert(VitalApp app, VitalSegment segment, List<GraphObject> graphObjects, Closure closure) {
		impl(closure, 'insert', [NO_TRANSACTION, app, segment, graphObjects])
	}
	
	void insert(VitalTransaction transaction, VitalApp app, VitalSegment segment, List<GraphObject> graphObjects, Closure closure) {
		impl(closure, 'insert', [transaction, app, segment, graphObjects])
	}
	
	void listApps(Closure closure) {
		impl(closure, 'listApps', [])
	} 
	
	void listDatabaseConnections(VitalApp app, Closure closure) {
		impl(closure, 'listDatabaseConnections', [app])
	}
	
	//ResultList
	void listFiles(VitalApp app, String filePath, Closure closure) {
		impl(closure, 'listFiles', [app, filePath])
	}
	
	void listSegments(VitalApp app, Closure closure) {
		impl(closure, 'listSegments', [app])
	}
	
	//ResultList
	void listSegmentsWithConfig(VitalApp app, Closure closure) {
		impl(closure, 'listSegmentsWithConfig', [app])
	}
	
	void listVitalServiceKeys(VitalApp app, Closure closure) {
		impl(closure, 'listVitalServiceKeys', [app])
	}
	
	void ping(Closure closure) {
		impl(closure, 'ping', [])
	}
	
//	ai.vital.vitalservice.admin.VitalServiceAdmin.pipeline(VitalApp, Closure<?>)
	
	void query(VitalApp app, VitalQuery query, Closure closure) {
		impl(closure, 'query', [app, query])
	}
	
//	queryContainers(VitalQuery, List<GraphObjectsIterable>, Closure closure)
	
	void queryLocal(VitalApp app, VitalQuery query, Closure closure) {
		impl(closure, 'queryLocal', [app, query])
	}
	
	void removeApp(VitalApp app, Closure closure) {
		impl(closure, 'removeApp',[app])
	}
	
	//VitalStatus
	void removeDatabaseConnection(VitalApp app, String databaseName, Closure closure) {
		impl(closure, 'removeDatabaseConnection', [app, databaseName])
	}
	
	void removeSegment(VitalApp app, VitalSegment segment, boolean deleteData, Closure closure) {
		impl(closure, 'removeSegment', [app, segment, deleteData])	
	} 
	
	void removeVitalServiceKey(VitalApp app, VitalServiceKey serviceKey, Closure closure) {
		impl(closure, 'removeVitalServiceKey', [app, serviceKey])
	}
	
	void rollbackTransaction(VitalTransaction transaction, Closure closure) {
		impl(closure, 'rollbackTransaction', [transaction])
	}
	
	void save(VitalApp app, GraphObject graphObject, Closure closure) {
		impl(closure, 'save', [NO_TRANSACTION, app, graphObject])
	}
	
	void save(VitalTransaction transaction, VitalApp app, GraphObject graphObject, Closure closure) {
		impl(closure, 'save', [transaction, app, graphObject])
	}
	
	void save(VitalApp app, List<GraphObject> graphObjects, Closure closure) {
		impl(closure, 'save', [NO_TRANSACTION, app, graphObjects])
	}
	
	void save(VitalTransaction transaction, VitalApp app, List<GraphObject> graphObjects, Closure closure) {
		impl(closure, 'save', [transaction, app, graphObjects])
	}
	
	void save(VitalApp app, VitalSegment segment, GraphObject graphObject, boolean create, Closure closure) {
		impl(closure, 'save', [NO_TRANSACTION, app, segment, graphObject, create])
	}
	
	void save(VitalTransaction transaction, VitalApp app, VitalSegment segment, GraphObject graphObject, boolean create, Closure closure) {
		impl(closure, 'save', [transaction, app, segment, graphObject, create])
	}
	
	void save(VitalApp app, VitalSegment segment, List<GraphObject> graphObjects, boolean create, Closure closure) {
		impl(closure, 'save', [NO_TRANSACTION, app, segment, graphObjects, create])
	}
	
	void save(VitalTransaction transaction, VitalApp app, VitalSegment segment, List<GraphObject> graphObjects, boolean create, Closure closure) {
		impl(closure, 'save', [transaction, app, segment, graphObjects, create])
	}
	
	void sendEvent(VitalApp app, VITAL_Event event, boolean waitForDelivery, Closure closure) {
		impl(closure, 'sendEvent', [app, event, waitForDelivery])
	}
	
	
	void sendEvents(VitalApp app, List<VITAL_Event> events, boolean waitForDelivery, Closure closure) {
		impl(closure, 'sendEvents', [app, events, waitForDelivery])
	}
	
	//VitalStatus
	void uploadFile(VitalApp app, URIProperty uri, String name, String localFilePath, boolean overwrite, Closure closure) {
		impl(closure, "uploadFile", [app, uri, name, localFilePath, overwrite])
	}
	
	void validate(Closure closure) {
		impl(closure, 'validate', [])
	}
	
}
