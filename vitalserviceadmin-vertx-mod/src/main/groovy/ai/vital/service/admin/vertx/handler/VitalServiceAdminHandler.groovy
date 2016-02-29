package ai.vital.service.admin.vertx.handler

import java.util.Map;

import groovy.lang.Closure

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.vertx.groovy.core.Vertx;
import org.vertx.groovy.core.eventbus.Message

import ai.vital.query.querybuilder.VitalBuilder
import ai.vital.service.vertx.binary.PayloadMessage;
import ai.vital.service.vertx.binary.ResponseMessage;
import ai.vital.service.vertx.handler.AbstractVitalServiceHandler;
import ai.vital.vitalservice.ServiceOperations
import ai.vital.vitalservice.VitalStatus
import ai.vital.vitalservice.admin.VitalServiceAdmin;
import ai.vital.vitalservice.factory.VitalServiceFactory
import ai.vital.vitalservice.query.VitalPathQuery
import ai.vital.vitalservice.query.VitalQuery
import ai.vital.vitalsigns.java.VitalJavaSerializationUtils;
import ai.vital.vitalsigns.meta.GraphContext
import ai.vital.vitalsigns.model.GraphObject
import ai.vital.vitalsigns.model.VITAL_Event
import ai.vital.vitalsigns.model.VitalApp;
import ai.vital.vitalsigns.model.VitalProvisioning;
import ai.vital.vitalsigns.model.VitalSegment;
import ai.vital.vitalsigns.model.VitalServiceKey;
import ai.vital.vitalsigns.model.VitalTransaction;
import ai.vital.vitalsigns.model.property.URIProperty
import static ai.vital.vitalservice.VitalServiceConstants.NO_TRANSACTION

class VitalServiceAdminHandler extends AbstractVitalServiceHandler {

	def static vitalBuilder = new VitalBuilder()
	
	private final static Logger log = LoggerFactory.getLogger(VitalServiceAdminHandler.class)
	
	VitalServiceAdmin service
	
	public VitalServiceAdminHandler(Vertx vertx, VitalServiceAdmin _service) {
		super(vertx);
		this.service = _service
	}

	protected VitalTransaction transaction(VitalTransaction t) {
		if(t == null || t.URI.equals(NO_TRANSACTION.URI)) return null
		return t
	}
	
	protected void handleMethod(String method, Object[] a, Map<String, Object> sessionParams, Closure closure) {

		Object response = null
	
		if(method == 'addApp') {
			
			checkParams(method, a, true, VitalApp.class)
			
			response = service.addApp(a[0])
			
		} else if(method == 'addSegment') {
		
			checkParams(method, a, true, VitalApp.class, VitalSegment.class, VitalProvisioning.class, Boolean.class)

			response = service.addSegment(a[0], a[1], a[2], a[3])
			
		} else if(method == 'addVitalServiceKey') {
		
			checkParams(method, a, true, VitalApp.class, VitalServiceKey.class)
		
			response = service.addVitalServiceKey(a[0], a[1])
		
		} else if(method == 'bulkExport') {
			
			unsupported(method)
			
		} else if(method == 'bulkImport') {
		
			unsupported(method)
			
		} else if(method == 'callFunction') {
			
			checkParams(method, a, true, VitalApp.class, String.class, Map.class)
			
			response = callFunctionLogic(service.getOrganization(), a[0], a[1], a[2], sessionParams, closure)
			
			if(response == null) response = service.callFunction(a[0], a[1], a[2])
			 
		} else if(method == 'close') {
		
			throw new RuntimeException("Method not allowed: ${method}")
			
		} else if(method == 'commitTransaction') {
		
			checkParams(method, a, true, VitalTransaction.class)	
		
			response = service.commitTransaction(a[0])
			
		} else if(method == 'createTransaction') {
		
			checkParams(method, a, true)
			
			response = service.createTransaction()	
		
		} else if(method == 'delete') {
		
			if(
				! checkParams(method, a, false, VitalTransaction.class, VitalApp.class, URIProperty.class) &&
				! checkParams(method, a, false, VitalTransaction.class, VitalApp.class, List.class)
			) {
				throw new RuntimeException("${method} expects VitalTransaction, VitalApp and either a ${URIProperty.class.simpleName} or a list of ${URIProperty.class.simpleName} objects")
			}
			
			response = service.delete(transaction(a[0]), a[1], a[2])
			
		} else if(method == 'deleteExpanded') {
		
			if( checkParams(method, a, false, VitalTransaction.class, VitalApp.class, URIProperty.class) ) {
				response = service.deleteExpanded(transaction(a[0]), a[1], a[2])
			} else if(checkParams(method, a, false, VitalTransaction.class, VitalApp.class, List.class) ) {
				response = service.deleteExpanded(transaction(a[0]), a[1], a[2])
			} else if(checkParams(method, a, false, VitalTransaction.class, VitalApp.class, URIProperty.class, VitalPathQuery.class)
			|| checkParams(method,a, false, VitalTransaction.class, VitalApp.class, URIProperty.class, String.class)
			) {
				queryFilter(a, 2)
				response = service.deleteExpanded(transaction(a[0]), a[1], a[2], a[3])
			} else {
				throw new RuntimeException("${method} expects VitalTransaction, VitalApp and either a ${URIProperty.class.simpleName} or a list of ${URIProperty.class.simpleName} objects or an ${URIProperty.class.simpleName},${VitalPathQuery.class.simpleName} ")
			}

			
		} else if(method == 'deleteExpandedObject') {
		
			checkParams(method, a, false, VitalTransaction.class, VitalApp.class, GraphObject.class)
				
			response = service.deleteExpandedObject(transaction(a[0]), a[1], a[2])
			
		} else if(method == 'deleteExpandedObjects') {

			queryFilter(a, 2)
					
			checkParams(method, a, false, VitalTransaction.class, VitalApp.class, List.class, VitalPathQuery.class)
			
			response = service.deleteExpandedObjects(transaction(a[0]), a[1], a[2], a[3])
			
		} else if(method == 'deleteFile') {
		
			checkParams(method, a, true, VitalApp.class, URIProperty.class, String.class)
				
			response = service.deleteFile(a[0], a[1], a[2])
				
		} else if(method == 'deleteObject') {
		
			checkParams(method, a, true, VitalTransaction.class, VitalApp.class, GraphObject.class)
			
			response = service.deleteObject(transaction(a[0]), a[1], a[2])
			
		} else if(method == 'deleteObjects') {
		
			checkParams(method, a, true, VitalTransaction.class, VitalApp.class, List.class)
			
			response = service.deleteObjects(transaction(a[0]), a[1], a[2])
			
		} else if(method == 'doOperations') {
			
			checkParams(method, a, true, VitalApp.class, ServiceOperations.class)
		
			response = service.doOperations(a[0], a[1])
			
		} else if(method == 'downloadFile') {

			//stream replaced with localFilePath
			checkParams(method, a, true, VitalApp.class, URIProperty.class, String.class, String.class, Boolean.class)
			
			OutputStream fos = null
			
			try {
				
				fos = new BufferedOutputStream(new FileOutputStream(new File(a[3])))
				
				//always close output stream
				response = service.downloadFile(a[0], a[1], a[2], fos, true)
				
			} finally {
				IOUtils.closeQuietly(fos)
			}
			
			
		} else if(method == 'fileExists') {
		
			checkParams(method, a, true, VitalApp.class, URIProperty.class, String.class)
				
			response = service.fileExists(a[0], a[1], a[2])
			
		} else if(method == 'generateURI') {
		
			checkParams(method, a, true, VitalApp.class, Class.class)
			
			response = service.generateURI(a[0], a[1])
			
		} else if(method == 'get') {
		
			if(
				checkParams(method, a, false, VitalApp.class, GraphContext.class, List.class) ||
				checkParams(method, a, false, VitalApp.class, GraphContext.class, URIProperty.class) ) {
				
				response = service.get(a[0], a[1], a[2])
				
			} else if(
				checkParams(method, a, false, VitalApp.class, GraphContext.class, List.class, Boolean.class) ||
				checkParams(method, a, false, VitalApp.class, GraphContext.class, List.class, List.class) ||
				checkParams(method, a, false, VitalApp.class, GraphContext.class, URIProperty.class, Boolean.class) ||
				checkParams(method, a, false, VitalApp.class, GraphContext.class, URIProperty.class, List.class)
			) {
			
				response = service.get(a[0], a[1], a[2], a[3])
			} else {
				throw new RuntimeException("${method} expects different parameters, see API")
			}
			
		} else if(method == 'getApp') {

			checkParams(method, a, true, String.class)
			
			response = service.getApp(a[0])
		
		} else if(method == 'getEndpointType') {
		
			checkParams(method, a, true)
			
			response = service.getEndpointType()
			
			
		} else if(method == 'getExpanded') {			
		
			if( checkParams(method, a, false, VitalApp.class, URIProperty.class, Boolean.class) ) {
				
				response = service.getExpanded(a[0], a[1], a[2])
				
			} else if( checkParams(method, a, false, VitalApp.class, URIProperty.class, VitalPathQuery.class, Boolean.class) ) {
			
				queryFilter(a, 2)
			
				response = service.getExpanded(a[0], a[1], a[2], a[3])
			
			} else {
			
				throw new RuntimeException("${method} expects VitalTransaction, VitalApp, URIProperty, Boolean or URIProperty, VitalPathQuery, Boolean")
			
			}				

		} else if(method == 'getName') {

			response = service.getName()		
			
		} else if(method == 'getOrganization') {
		
			checkParams(method, a, true)
			
			response = service.getOrganization()				
			
		} else if(method == 'getSegment') {
		
			checkParams(method, a, true, VitalApp.class, String.class)
		
			response = service.getSegment(a[0], a[1])
			
		} else if(method == 'getTransactions') {
		
			checkParams(method, a, true)
		
			response = service.getTransactions()
			
		} else if(method == 'insert') {
		
			if( checkParams(method, a, false, VitalTransaction.class, VitalApp.class, VitalSegment.class, GraphObject.class) 
				|| checkParams(method, a, false, VitalTransaction.class, VitalApp.class, VitalSegment.class, List.class) ) {
				
				response = service.insert(transaction(a[0]), a[1], a[2], a[3])
			} else {
			
				throw new RuntimeException("${method} expects VitalTransaaction, VitalApp, VitalSegment and either GraphObject or list of GraphObjects")
			
			}

		} else if(method == 'listApps') {
		
			checkParams(method, a, true)
			
			response = service.listApps()
		
		} else if(method == 'listDatabaseConnections') {
		
			checkParams(method, a, true, VitalApp.class)
		
			response = service.listDatabaseConnections(a[0])
			
		} else if(method == 'listFiles') {
		
			checkParams(method, a, true, VitalApp.class, String.class)
				
			response = service.listFiles(a[0], a[1])
		
		} else if(method == 'listSegments') {
		
			checkParams(method, a, true, VitalApp.class)
		
			response = service.listSegments(a[0])
		
		} else if(method == 'listSegmentsWithConfig') {
		
			checkParams(method, a, true, VitalApp.class)
		
			response = service.listSegmentsWithConfig(a[0])	
		
		} else if(method == 'listVitalServiceKeys') {
		
			checkParams(method, a, true, VitalApp.class)
		
			response = service.listVitalServiceKeys(a[0])
			
		} else if(method == 'ping') {
		
			checkParams(method, a, true)
		
			response = service.ping()

		} else if(method == 'query') {
		
			queryFilter(a, 1)
			
			checkParams(method, a, true, VitalApp.class, VitalQuery.class)
			
			response = service.query(a[0], a[1])
			
		} else if(method == 'queryContainers') {
		
			queryFilter(a, 1)
		
			checkParams(method, a, true, VitalApp.class, VitalQuery.class, List.class)
			
			response = service.queryContainers(a[0], a[1], a[2])
		
		} else if(method == 'queryLocal') {

			queryFilter(a, 1)
					
			checkParams(method, a, true, VitalApp.class, VitalQuery.class)
			
			response = service.queryLocal(a[0], a[1])
			
		} else if(method == 'removeApp') {
		
			checkParams(method, a, true, VitalApp.class)
			
			response = service.removeApp(a[0])
		
		} else if(method == 'removeDatabaseConnection') {
		
			checkParams(method, a, true, VitalApp.class, String.class)
		
			response = service.removeDatabaseConnection(a[0], a[1])
			
		} else if(method == 'removeSegment') {
		
			checkParams(method, a, true, VitalApp.class, VitalSegment.class, Boolean.class)
			
			response = service.removeSegment(a[0], a[1], a[2])
		
		} else if(method == 'removeVitalServiceKey') {
		
			checkParams(method, a, true, VitalApp.class, VitalServiceKey.class)
		
			response = service.removeVitalServiceKey(a[0], a[1])
			
		} else if(method == 'rollbackTransaction') {
		
			checkParams(method, a, true, VitalTransaction.class)
		
			response = service.rollbackTransaction(a[0])
		
		} else if(method == 'save') {
		
			if(
				checkParams(method, a, false, VitalTransaction.class, VitalApp.class, GraphObject.class) ||
				checkParams(method, a, false, VitalTransaction.class, VitalApp.class, List.class)
				
			){
				response = service.save(transaction(a[0]), a[1], a[2])
			} else if(
				checkParams(method, a, false, VitalTransaction.class, VitalApp.class, VitalSegment.class, GraphObject.class, Boolean.class) ||
				checkParams(method, a, false, VitalTransaction.class, VitalApp.class, VitalSegment.class, List.class, Boolean.class)
			){
				response = service.save(transaction(a[0]), a[1], a[2], a[3], a[4])
			} else {
				throw new RuntimeException("${method} expects VitalTransaction VitaApp, GraphObject or list of GraphObjects, or ( VitalTransaction, Vitalapp, VitalSegment GraphObject|List, boolean)")
			}
			
		} else if(method == 'sendEvent') {
		
			checkParams(method, a, true, VitalApp.class, VITAL_Event.class, Boolean.class)
			
			response = service.sendEvent(a[0], a[1], a[2])
		
		} else if(method == 'sendEvents') {
		
			checkParams(method, a, true, VitalApp.class, List.class, Boolean.class)
			
			response = service.sendEvents(a[0], a[1], a[2])
			
		} else if(method == 'uploadFile') {

			//String
			checkParams(method, a, true, VitalApp.class, URIProperty.class, String.class, String.class, Boolean.class)
				
			//stream replaced with localFilePath
			InputStream fis = null
			
			try {
				
				fis = new BufferedInputStream(new FileInputStream(new File(a[3])))
				
				//always close output stream
				response = service.uploadFile(a[0], a[1], a[2], fis, a[4])
				
			} finally {
				IOUtils.closeQuietly(fis)
			}
		
		} else if(method == 'validate') {
		
			checkParams(method, a, true)
			
			response = service.validate()
				
		} else {
			throw new NoSuchMethodException("No method in vitalservice: ${method}")
		}
		
		closure(response)
			
	}
	
}
