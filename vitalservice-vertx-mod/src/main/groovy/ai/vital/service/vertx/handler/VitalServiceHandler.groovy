package ai.vital.service.vertx.handler

import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.vertx.groovy.core.Vertx;
import org.vertx.groovy.core.eventbus.Message

import ai.vital.query.querybuilder.VitalBuilder
import ai.vital.service.vertx.binary.PayloadMessage;
import ai.vital.service.vertx.binary.ResponseMessage;
import ai.vital.service.vertx.json.VitalServiceJSONMapper
import ai.vital.vitalservice.ServiceOperations
import ai.vital.vitalservice.Transaction
import ai.vital.vitalservice.VitalService
import ai.vital.vitalservice.VitalStatus
import ai.vital.vitalservice.factory.VitalServiceFactory
import ai.vital.vitalservice.query.VitalPathQuery
import ai.vital.vitalservice.query.VitalQuery
import ai.vital.vitalservice.segment.VitalSegment
import ai.vital.vitalsigns.java.VitalJavaSerializationUtils;
import ai.vital.vitalsigns.meta.GraphContext
import ai.vital.vitalsigns.model.GraphObject
import ai.vital.vitalsigns.model.VITAL_Event
import ai.vital.vitalsigns.model.property.URIProperty

class VitalServiceHandler extends AbstractVitalServiceHandler {

	def static vitalBuilder = new VitalBuilder()
	
	private final static Logger log = LoggerFactory.getLogger(VitalServiceHandler.class)
	
	public VitalServiceHandler(Vertx vertx) {
		super(vertx);
	}

	protected Object handleMethod(String method, Object[] a) {
		
		Object response = null
			
		VitalService service = VitalServiceFactory.getVitalService()
			
		if(method == 'bulkExport') {
				
			unsupported(method)
				
		} else if(method == 'bulkImport') {
			
			unsupported(method)
			
		} else if(method == 'callFunction') {
			
			checkParams(method, a, true, String.class, Map.class)
			
			response = callFunctionLogic(service.getOrganization(), service.getApp(), a[0], a[1])
						
			if(response == null) response = service.callFunction(a[0], a[1])
				 
		} else if(method == 'close') {
		
			throw new RuntimeException("Method not allowed: ${method}")
				
		} else if(method == 'commitTransaction') {
			
			checkParams(method, a, true, Transaction.class)	
			
			service.commitTransaction(a[0])
				
		} else if(method == 'createTransaction') {
			
			checkParams(method, a, true)
				
			response = service.createTransaction()	
			
		} else if(method == 'delete') {
			
			if(
				! checkParams(method, a, false, URIProperty.class) &&
				! checkParams(method, a, false, List.class)
			) {
				throw new RuntimeException("${method} expects either a ${URIProperty.class.simpleName} or a list of ${URIProperty.class.simpleName} objects")
			}
				
			response = service.delete(a[0])
				
		} else if(method == 'deleteExpanded') {
			
			if( checkParams(method, a, false, URIProperty.class) ) {
				response = service.deleteExpanded(a[0])
			} else if(checkParams(method, a, false, List.class) ) {
				response = service.deleteExpanded(a[0])
			} else if(checkParams(method, a, false, URIProperty.class, VitalPathQuery.class) || checkParams(method, a, false, URIProperty.class, VitalPathQuery.class)) {
				queryFilter(a, 1)
				response = service.deleteExpanded(a[0], a[1])
			} else {
				throw new RuntimeException("${method} expects either a ${URIProperty.class.simpleName} or a list of ${URIProperty.class.simpleName} objects or an ${URIProperty.class.simpleName},${VitalPathQuery.class.simpleName} ")
			}
				
		} else if(method == 'deleteExpandedObject') {
		
		
			checkParams(method, a, false, GraphObject.class)
				
			response = service.deleteExpandedObject(a[0])
			
		} else if(method == 'deleteExpandedObjects') {
		
			queryFilter(a, 1)
		
			checkParams(method, a, false, List.class, VitalPathQuery.class)
			
			response = service.deleteExpandedObjects(a[0], a[1])
			
		} else if(method == 'deleteFile') {
		
			unsupported(method)
		
//				checkParams(method, a, true, URIProperty.class, String.class)
//				
//				response = service.deleteFile(a[0], a[1])
				
		} else if(method == 'deleteObject') {
		
			checkParams(method, a, true, GraphObject.class)
			
			response = service.deleteObject(a[0])
			
		} else if(method == 'deleteObjects') {
		
			checkParams(method, a, true, List.class)
			
			response = service.deleteObjects(a[0])
			
		} else if(method == 'doOperations') {
			
			checkParams(method, a, true, ServiceOperations.class)
		
			response = service.doOperations(a[0])
			
		} else if(method == 'downloadFile') {
		
			unsupported(method)
		
//				checkParams(method, a, true, URIProperty.class, String.class, OutputStream.class, Boolean.class)
//				
//				response = service.downloadFile(a[0], a[1], a[2], a[3])
			
		} else if(method == 'fileExists') {
			
			unsupported(method)
			
//				checkParams(method, a, true, URIProperty.class, String.class)
//				
//				response = service.fileExists(a[0], a[1])
			
		} else if(method == 'generateURI') {
			
			checkParams(method, a, true, Class.class)
				
			response = service.generateURI(a[0])
			
		} else if(method == 'get') {
		
			if(
				checkParams(method, a, false, GraphContext.class, List.class) ||
				checkParams(method, a, false, GraphContext.class, URIProperty.class) ) {
				
				response = service.get(a[0], a[1])
				
			} else if(
				checkParams(method, a, false, GraphContext.class, List.class, Boolean.class) ||
				checkParams(method, a, false, GraphContext.class, List.class, List.class) ||
				checkParams(method, a, false, GraphContext.class, URIProperty.class, Boolean.class) ||
				checkParams(method, a, false, GraphContext.class, URIProperty.class, List.class)
			) {
			
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
				
		} else if(method == 'getTransactions') {
			
			checkParams(method, a, true)
		
			response = service.getTransactions()
			
		} else if(method == 'insert') {
		
			if( checkParams(method, a, false, VitalSegment.class, GraphObject.class) 
				|| checkParams(method, a, false, VitalSegment.class, List.class) ) {
				
				response = service.insert(a[0], a[1])
			} else {
			
				throw new RuntimeException("${method} expects VitalSegment.class and either GraphObject or list of GraphObjects")
			
			}
		} else if(method == 'listFiles') {
		
			unsupported(method)
		
//				checkParams(method, a, true, String.class)
//				
//				response = service.listFiles(a[0])
			
		} else if(method == 'listSegments') {
		
			checkParams(method, a, true)
		
			response = service.listSegments()
		
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
		
			checkParams(method, a, true, Transaction.class)
		
			response = service.rollbackTransaction(a[0])
		
		} else if(method == 'save') {
		
			if(
				checkParams(method, a, false, GraphObject.class) ||
				checkParams(method, a, false, List.class)
				
			){
				response = service.save(a[0])
			} else if(
				checkParams(method, a, false, VitalSegment.class, GraphObject.class, Boolean.class) ||
				checkParams(method, a, false, VitalSegment.class, List.class, Boolean.class)
			){
				response = service.save(a[0], a[1], a[2])
			} else {
				throw new RuntimeException("${method} expects GraphObject or list of GraphObjects, or ( Vital, Segment GraphObject|List, boolean) triple")
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
			
		} else if(method == 'setTransaction') {
		
			checkParams(method, a, true, Transaction.class)
			
			response = service.setTransaction(a[0])
		
		} else if(method == 'uploadFile') {
		
			unsupported(method)
		
//				checkParams(method, a, true, URIProperty.class, String.class, InputStream.class, Boolean.class)
//				
//				response = service.uploadFile(a[0], a[1], a[2], a[3])
	
		} else if(method == 'validate') {
			
			checkParams(method, a, true)
			
			response = service.validate()
				
		} else {
			throw new NoSuchMethodException("No method in vitalservice: ${method}")
		}
		
		return response
		
	}

}
