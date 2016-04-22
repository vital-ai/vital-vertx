package ai.vital.service.vertx3.handler

import java.util.Map

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import io.vertx.groovy.core.Vertx
import io.vertx.groovy.core.eventbus.Message

import ai.vital.query.querybuilder.VitalBuilder
import ai.vital.service.vertx3.binary.PayloadMessage;
import ai.vital.service.vertx3.binary.ResponseMessage;
import ai.vital.vitalservice.ServiceOperations
import ai.vital.vitalservice.VitalService
import ai.vital.vitalservice.VitalServiceConstants;
import ai.vital.vitalservice.VitalStatus
import ai.vital.vitalservice.factory.VitalServiceFactory
import ai.vital.vitalservice.query.VitalPathQuery
import ai.vital.vitalservice.query.VitalQuery
import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.java.VitalJavaSerializationUtils;
import ai.vital.vitalsigns.meta.GraphContext
import ai.vital.vitalsigns.model.GraphObject
import ai.vital.vitalsigns.model.VITAL_Event
import ai.vital.vitalsigns.model.VitalSegment;
import ai.vital.vitalsigns.model.VitalTransaction;
import ai.vital.vitalsigns.model.property.URIProperty


class VitalServiceHandler extends AbstractVitalServiceHandler {

	def static vitalBuilder = new VitalBuilder()
	
	private final static Logger log = LoggerFactory.getLogger(VitalServiceHandler.class)
	
	VitalService service
	
	public VitalServiceHandler(Vertx vertx, VitalService _service) {
		super(vertx);
		this.service = _service
	}

	protected VitalTransaction transaction(VitalTransaction t) {
		if(t == null || t.URI.equals(VitalServiceConstants.NO_TRANSACTION.URI)) return null
		return t
	}
	
	protected void handleMethod(String method, Object[] a, Map<String, Object> sessionParams, Closure closure) {
		
		Object response = null
			
		if(method == 'bulkExport') {
				
			unsupported(method)
				
		} else if(method == 'bulkImport') {
			
			unsupported(method)
			
		} else if(method == 'callFunction') {
			
			checkParams(method, a, true, String.class, Map.class)
			
			response = callFunctionLogic(service.getOrganization(), service.getApp(), a[0], a[1], sessionParams, closure)
						
			if(response == null) response = service.callFunction(a[0], a[1])
				 
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
				! checkParams(method, a, false, VitalTransaction.class, URIProperty.class) &&
				! checkParams(method, a, false, VitalTransaction.class, List.class)
			) {
				throw new RuntimeException("${method} expects VitalTransaction and either a ${URIProperty.class.simpleName} or a list of ${URIProperty.class.simpleName} objects")
			}
				
			response = service.delete(transaction(a[0]), a[1])
				
		} else if(method == 'deleteExpanded') {
			
			if( checkParams(method, a, false, VitalTransaction.class, URIProperty.class) ) {
				response = service.deleteExpanded(transaction(a[0]), a[1])
			} else if(checkParams(method, a, false, VitalTransaction.class, List.class) ) {
				response = service.deleteExpanded(transaction(a[0]), a[1])
			} else if(checkParams(method, a, false, VitalTransaction.class, URIProperty.class, VitalPathQuery.class) || checkParams(method, a, false, VitalTransaction.class, URIProperty.class, VitalPathQuery.class)) {
				queryFilter(a, 1)
				response = service.deleteExpanded(transaction(a[0]), a[1], a[2])
			} else {
				throw new RuntimeException("${method} expects VitalTransaction and either a ${URIProperty.class.simpleName} or a list of ${URIProperty.class.simpleName} objects or an ${URIProperty.class.simpleName},${VitalPathQuery.class.simpleName} ")
			}
				
		} else if(method == 'deleteExpandedObject') {
		
		
			checkParams(method, a, false, VitalTransaction.class, GraphObject.class)
				
			response = service.deleteExpandedObject(transaction(a[0]), a[1])
			
		} else if(method == 'deleteExpandedObjects') {
		
			queryFilter(a, 1)
		
			checkParams(method, a, false, VitalTransaction.class, List.class, VitalPathQuery.class)
			
			response = service.deleteExpandedObjects(transaction(a[0]), a[1], a[2])
			
		} else if(method == 'deleteFile') {
		
			checkParams(method, a, true, URIProperty.class, String.class)

			response = service.deleteFile(a[0], a[1])
			
		} else if(method == 'deleteObject') {
		
			checkParams(method, a, true, VitalTransaction.class, GraphObject.class)
			
			response = service.deleteObject(transaction(a[0]), a[1])
			
		} else if(method == 'deleteObjects') {
		
			checkParams(method, a, true, VitalTransaction.class, List.class)
			
			response = service.deleteObjects(transaction(a[0]), a[1])
			
		} else if(method == 'doOperations') {
			
			checkParams(method, a, true, ServiceOperations.class)
		
			response = service.doOperations(a[0])
			
		} else if(method == 'downloadFile') {
		
			//stream replaced with localFilePath
			checkParams(method, a, true, URIProperty.class, String.class, String.class, Boolean.class)
			
			OutputStream fos = null
			
			try {
				
				fos = new BufferedOutputStream(new FileOutputStream(new File(a[2])))
				
				//always close output stream
				response = service.downloadFile(a[0], a[1], fos, true)
				
			} finally {
				IOUtils.closeQuietly(fos)
			}
			
		} else if(method == 'fileExists') {
			
			checkParams(method, a, true, URIProperty.class, String.class)

			response = service.fileExists(a[0], a[1])
			
		} else if(method == 'generateURI') {
			
			if( checkParams(method, a, false, Class.class) ) {
				
				response = service.generateURI(a[0])
				
			} else if(checkParams(method, a, false, String.class)) {
			
				String classURI = a[0]
				
				Class c = VitalSigns.get().getClass(URIProperty.withString(classURI))
				if(c == null) throw new RuntimeException("Class not found for URI: " + classURI)
				
				response = service.generateURI(c)
			
			} else {
				throw new RuntimeException("${method} expects either a class object or class URI string")
			}
			
		} else if(method == 'get') {
		
			if(
				checkParams(method, a, false, GraphContext.class, List.class) ||
				checkParams(method, a, false, String.class, List.class) ||
				checkParams(method, a, false, GraphContext.class, URIProperty.class) ||
				checkParams(method, a, false, String.class, URIProperty.class)) {
				
				//convert string to GraphContext enum
				if(a[0] instanceof String) {
					a[0] = GraphContext.valueOf(a[0])
				}
				
				response = service.get(a[0], a[1])
				
			} else if(
				checkParams(method, a, false, GraphContext.class, List.class, Boolean.class) ||
				checkParams(method, a, false, String.class, List.class, Boolean.class) ||
				checkParams(method, a, false, GraphContext.class, List.class, List.class) ||
				checkParams(method, a, false, String.class, List.class, List.class) ||
				checkParams(method, a, false, GraphContext.class, URIProperty.class, Boolean.class) ||
				checkParams(method, a, false, String.class, URIProperty.class, Boolean.class) ||
				checkParams(method, a, false, GraphContext.class, URIProperty.class, List.class) ||
				checkParams(method, a, false, String.class, URIProperty.class, List.class)
			) {
			
				//convert string to GraphContext enum
				if(a[0] instanceof String) {
					a[0] = GraphContext.valueOf(a[0])
				}
				
				response = service.get(a[0], a[1], a[2])
			} else {
				throw new RuntimeException("${method} expects different parameters, see API")
			}
			} else if(method == 'getApp') {
		
			checkParams(method, a, true)
			
			response = service.getApp()
			
		} else if(method == 'getDefaultSegmentName') {
		
			checkParams(method, a, true)
			
			response = service.getDefaultSegmentName()
			
		} else if(method == 'getEndpointType') {
		
			checkParams(method, a, true)
			
			response = service.getEndpointType()
			
		} else if(method == 'getExpanded') {			
		
			if( checkParams(method, a, false, URIProperty.class, Boolean.class) ) {
				
				response = service.getExpanded(a[0], a[1])
				
			} else if( checkParams(method, a, false, URIProperty.class, VitalPathQuery.class, Boolean.class) ) {
			
				queryFilter(a, 1)
			
				response = service.getExpanded(a[0], a[1], a[2])
			
			} else {
			
				throw new RuntimeException("${method} expects URIProperty, Boolean or URIProperty, VitalPathQuery, Boolean")
			
			}
							
		} else if(method == 'getOrganization') {
		
			checkParams(method, a, true)
			
			response = service.getOrganization()				
		
		} else if(method == 'getSegment') {
		
			checkParams(method, a, true, String.class)
			
			response = service.getSegment(a[0])
					
		} else if(method == 'getTransactions') {
			
			checkParams(method, a, true)
		
			response = service.getTransactions()
			
		} else if(method == 'insert') {
		
			if( checkParams(method, a, false, VitalTransaction.class, VitalSegment.class, GraphObject.class) 
				|| checkParams(method, a, false, VitalTransaction.class, VitalSegment.class, List.class) ) {
				
				response = service.insert(transaction(a[0]), a[1], a[2])
			} else {
			
				throw new RuntimeException("${method} expects a transaction, VitalSegment.class and either GraphObject or list of GraphObjects")
			
			}
		} else if(method == 'listFiles') {
		
			checkParams(method, a, true, String.class)
				
			response = service.listFiles(a[0])
			
		} else if(method == 'listDatabaseConnections') {
		
			checkParams(method, a, true)
			
			response = service.listDatabaseConnections()
		
		} else if(method == 'listSegments') {
		
			checkParams(method, a, true)
		
			response = service.listSegments()
			
		} else if(method == 'listSegmentsWithConfig') {
		
			checkParams(method, a, true)
			
			response = service.listSegmentsWithConfig()
		
		} else if(method == 'ping') {
		
			checkParams(method, a, true)
		
			response = service.ping()
			
		} else if(method == 'query') {
		
			queryFilter(a, 0)
		
			checkParams(method, a, true, VitalQuery.class)
				
			response = service.query(a[0])
				
		} else if(method == 'queryContainers') {
		
			queryFilter(a, 0)
		
			checkParams(method, a, true, VitalQuery.class, List.class)
			
			response = service.queryContainers(a[0], a[1])
		
		} else if(method == 'queryLocal') {
		
			queryFilter(a, 0)
		
			checkParams(method, a, true, VitalQuery.class)
			
			response = service.queryLocal(a[0])
			
		} else if(method == 'rollbackTransaction') {
		
			checkParams(method, a, true, VitalTransaction.class)
		
			response = service.rollbackTransaction(a[0])
		
		} else if(method == 'save') {
		
			if(
				checkParams(method, a, false, VitalTransaction.class, GraphObject.class) ||
				checkParams(method, a, false, VitalTransaction.class, List.class)
				
			){
				response = service.save(transaction(a[0]), a[1])
			} else if(
				checkParams(method, a, false, VitalTransaction.class, VitalSegment.class, GraphObject.class, Boolean.class) ||
				checkParams(method, a, false, VitalTransaction.class, VitalSegment.class, List.class, Boolean.class)
			){
				response = service.save(transaction(a[0]), a[1], a[2], a[3])
			} else {
				throw new RuntimeException("${method} expects VitalTransaction, GraphObject or list of GraphObjects, or ( VitalTransaction, VitalSegment GraphObject|List, boolean) quad")
			}
			
		} else if(method == 'sendEvent') {
		
			checkParams(method, a, true, VITAL_Event.class, Boolean.class)
			
			response = service.sendEvent(a[0], a[1])
		
		} else if(method == 'sendEvents') {
		
			checkParams(method, a, true, List.class, Boolean.class)
			
			response = service.sendEvents(a[0], a[1])
			
		} else if(method == 'setDefaultSegmentName') {
		
			checkParams(method, a, true, String.class)
			
			service.setDefaultSegmentName(a[0])
			
			response = VitalStatus.withOK()
			
		} else if(method == 'uploadFile') {
		
			//String
			checkParams(method, a, true, URIProperty.class, String.class, String.class, Boolean.class)
				
			//stream replaced with localFilePath
			InputStream fis = null
			
			try {
				
				fis = new BufferedInputStream(new FileInputStream(new File(a[2])))
				
				//always close output stream
				response = service.uploadFile(a[0], a[1], fis, a[3])
				
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
