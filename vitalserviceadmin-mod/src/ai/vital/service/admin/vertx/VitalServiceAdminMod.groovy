package ai.vital.service.admin.vertx

import org.vertx.groovy.core.eventbus.Message;
import org.vertx.groovy.platform.Verticle;
import org.vertx.java.core.Future;

import ai.vital.vitalservice.VitalStatus
import ai.vital.vitalservice.admin.VitalServiceAdmin
import ai.vital.vitalservice.factory.Factory
import ai.vital.vitalservice.model.App
import ai.vital.vitalservice.query.VitalGraphQuery;
import ai.vital.vitalservice.query.VitalSelectQuery;
import ai.vital.vitalservice.segment.VitalSegment;
import ai.vital.service.vertx.json.VitalServiceJSONMapper
import ai.vital.vitalsigns.datatype.VitalURI
import ai.vital.vitalsigns.meta.GraphContext;
import ai.vital.vitalsigns.model.GraphObject;
import ai.vital.vitalsigns.model.VITAL_Event;


class VitalServiceAdminMod extends Verticle {

	public static String ADDRESS = "vitalserviceadmin"
	
	@Override	public Object start() {		return this.start(null);	}

	private VitalServiceAdmin adminInstance;
	
	@Override
	public Object start(Future<Void> startedResult) {
		container.logger.info "Starting Vital Service Admin Verticle ..."				container.logger.info "Initializing vital service admin singleton if not set ..."
		try {
			adminInstance = Factory.getVitalServiceAdmin();
		} catch(Exception e) {
			container.logger.error(e.localizedMessage)
			if(startedResult != null) {
				startedResult.setFailure(e)
			}
			throw e;		}
		
		container.logger.info "Obtained vital admin instance: ${adminInstance.class.getCanonicalName()}";
		
		
		vertx.eventBus.registerHandler(ADDRESS) { Message message ->
			handle(message);
		}
		
		container.logger.info "Handler registered."
		
		if(startedResult != null) {
			startedResult.setResult(null);
		}
		
		return new Object()

	}
	
	@Override
	public Object stop() {
		if(adminInstance != null) {
			container.logger.info "Stopping vital admin instance..."
			adminInstance.close();
			adminInstance = null;
		}
	}
	
	public void handle(Message msg) {
		
		Map body = msg.body()
		
		String method = null
		
		Object[] a = null
		
		Map r = [:]
		
		try {
			
			method = body.get("method")
			
			if(! method) throw new RuntimeException("No method string")

			List args = body.get("args")
				
			if(args == null) throw new RuntimeException("Args list cannot be null")
				
			List l = []
				
			//convert args into array dynamically
			for(int i = 0 ; i < args.size(); i++) {
					
				Object o = args.get(i)
	
				l.add(VitalServiceJSONMapper.fromJSON(o))
														
			}
				
			a = l.toArray(new Object[l.size()])
				
//			if( ! allowedMethods.contains(method) ) throw new RuntimeException("Method not allowed: ${method}")
			
			VitalServiceAdmin service = adminInstance
			
			Object response = null

			if(method == 'addApp') {
				
				checkParams(method, a, true, App.class)
				
				response = service.addApp(a[0])

			} else if(method == 'addSegment') {
			
				checkParams(method, a, true, App.class, VitalSegment.class, Boolean.class)
			
				response = service.addSegment(a[0], a[1], a[2])
								
			} else if(method == 'callFunction') {
				
				checkParams(method, a, true, App.class, String.class, Map.class)
				
				response = service.callFunction(a[0], a[1], a[2])
				 
			} else if(method == 'close') {
			
				throw new RuntimeException("Method not allowed: ${method}")
				
			} else if(method == 'delete') {
			
				if(
					! checkParams(method, a, false, App.class, VitalURI.class) &&
					! checkParams(method, a, false, App.class, List.class)
				) {
					throw new RuntimeException("${method} expects either a VitalURI or a list of VitalURI objects")
				}
				
				response = service.delete(a[0], a[1])
				
			} else if(method == 'deleteExpanded') {
			
				if(
					! checkParams(method, a, false, App.class, VitalURI.class) &&
					! checkParams(method, a, false, App.class, List.class)
				) {
					throw new RuntimeException("${method} expects either a VitalURI or a list of VitalURI objects after App")
				}
				
				response = service.deleteExpanded(a[0], a[1])
				
			} else if(method == 'deleteExpandedInSegments') {

				if(
					! checkParams(method, a, false, App.class, VitalURI.class, List.class) &&
					! checkParams(method, a, false, App.class, List.class, List.class)
				) {
					throw new RuntimeException("${method} expects either a VitalURI or a list of VitalURI objects after App and before List<VitalSegment>")
				}
					
				response = service.deleteExpandedInSegments(a[0], a[1], a[2])
			
			} else if(method == 'deleteExpandedInSegmentsWithPaths') {

				if(
					! checkParams(method, a, false, App.class, VitalURI.class, List.class, List.class) &&
					! checkParams(method, a, false, App.class, List.class, List.class, List.class)
				) {
					throw new RuntimeException("${method} expects either a VitalURI or a list of VitalURI objects after App and before List<VitalSegment>, List<List<QueryPathElement>>")
				}
				
				response = service.deleteExpandedInSegmentsWithPaths(a[0], a[1], a[2], a[3])
				
			} else if(method == 'deleteExpandedWithPaths') {
				
				if(
					! checkParams(method, a, false, App.class, VitalURI.class, List.class) &&
					! checkParams(method, a, false, App.class, List.class, List.class)
				) {
					throw new RuntimeException("${method} expects either a VitalURI or a list of VitalURI objects after App and before List<List<QueryPathElement>>")
				}
				
				response = service.deleteExpandedWithPaths(a[0], a[1], a[2])
				
			} else if(method == 'deleteFile') {
			
				checkParams(method, a, true, App.class, VitalURI.class, String.class)
				
				response = service.deleteFile(a[0], a[1], a[2])
			
			} else if(method == 'deleteObjects') {
			
				checkParams(method, a, true, App.class, List.class)
				
				response = service.deleteObjects(a[0], a[1])
					
			} else if(method == 'downloadFile') {
			
				checkParams(method, a, true, App.class, VitalURI.class, String.class, OutputStream.class, Boolean.class)
				
				response = service.downloadFile(a[0], a[1], a[2], a[3], a[4])
				
			} else if(method == 'fileExists') {
			
				checkParams(method, a, true, App.class, VitalURI.class, String.class)
				
				response = service.fileExists(a[0], a[1], a[2])
				
			} else if(method == 'generateURI') {
			
				checkParams(method, a, true, App.class, Class.class)
				
				response = service.generateURI(a[0], a[1])
				
			} else if(method == 'get') {
			
				if(
					! checkParams(method, a, false, App.class, VitalURI.class, GraphContext.class, List.class) &&
					! checkParams(method, a, false, App.class, List.class, GraphContext.class, List.class)
				) {
					throw new RuntimeException("${method} expects either a VitalURI or a list of VitalURI objects after App and before GraphContext and List<GraphObjectsIterable>")
				}
			
				response = service.get(a[0], a[1], a[2], a[3])
				
			} else if(method == 'getExpanded') {
			
				checkParams(method, a, true, App.class, VitalURI.class)
				
				response = service.getExpanded(a[0], a[1])
				
			} else if(method == 'getExpandedInSegments') {
			
				checkParams(method, a, true, App.class, VitalURI.class, List.class)
			
				response = service.getExpandedInSegments(a[0], a[1], a[2])
				
			} else if(method == 'getExpandedInSegmentsWithPaths') {
			
				checkParams(method, a, true, App.class, VitalURI.class, List.class, List.class)
			
				response = service.getExpandedInSegmentsWithPaths(a[0], a[1], a[2], a[3])
				
			} else if(method == 'getExpandedWithPaths') {
			
				checkParams(method, a, true, App.class, VitalURI.class, List.class)
			
				response = service.getExpandedWithPaths(a[0], a[1], a[2]);
				
			} else if(method == 'graphQuery') {
			
				checkParams(method, a, true, App.class, VitalGraphQuery.class)
				
				response = service.graphQuery(a[0], a[1])
				
			} else if(method == 'listApps') {
			
				checkParams(method, a, true)
			
				response = service.listApps();
			
					
			} else if(method == 'listSegments') {
			
				checkParams(method, a, true, App.class)
			
				response = service.listSegments(a[0])
			
			} else if(method == 'ping') {
			
				checkParams(method, a, true)
			
				response = service.ping()
				
			} else if(method == 'removeApp') {
			
				checkParams(method, a, true, App.class)
			
				response = service.removeApp(a[0])
				
			} else if(method == 'removeSegment') {
			
				checkParams(method, a, true, App.class, VitalSegment.class, Boolean.class)

				response = service.removeSegment(a[0], a[1], a[2])

			} else if(method == 'save') {
			
				if(
					! checkParams(method, a, false, App.class, VitalSegment.class, GraphObject.class) &&
					! checkParams(method, a, false, App.class, VitalSegment.class, List.class)
				) {
					throw new RuntimeException("${method} expects VitalSegment.class and either GraphObject or list of GraphObjects")
				}
				
				response = service.save(a[0], a[1], a[2])
								
			} else if(method == 'selectQuery') {
			
				checkParams(method, a, true, App.class, VitalSelectQuery.class)
				
				response = service.selectQuery(a[0], a[1])
				
			} else if(method == 'sendFlumeEvent') {
			
				checkParams(method, a, true, App.class, VITAL_Event.class, Boolean.class)
				
				response = service.sendFlumeEvent(a[0], a[1], a[2])
			
			} else if(method == 'sendFlumeEvents') {
			
				checkParams(method, a, true, App.class, List.class, Boolean.class)
				
				response = service.sendFlumeEvents(a[0], a[1], a[2])
				
			} else if(method == 'uploadFile') {
			
				checkParams(method, a, true, App.class, VitalURI.class, String.class, InputStream.class, Boolean.class)
				response = service.uploadFile(a[0], a[1], a[2], a[3], a[4])
			
			} else {
				throw new NoSuchMethodException("No method in vitalservice: ${method}")
			}
			
	
			if(response != null) {

				if(response instanceof VitalStatus) {
					r.put('status', ((VitalStatus)response).status.name())
					r.put('message',  ((VitalStatus)response).message)
				}
				
				response = VitalServiceJSONMapper.toJSON(response)
							
				r.put('response', response)
			}
			
			if(r.get('status') == null)  {
				r.put('status', 'ok')
			}
			
		} catch(Exception e) {
			e.printStackTrace();
			r.put('status', 'error')
			r.put('message', e.getLocalizedMessage())
		}
		
		msg.reply(r)
					
	}
	

	private boolean checkParams(String method, Object[] args, boolean throwException, Class... classes) {
		
		if(classes.length != args.length) {
			if(throwException) throw new RuntimeException("Method: ${method} - arguments count does not match, : ${method}, given: ${args.length}, expected: ${classes.length}")
			return false;
		}
		for(int i = 0 ; i < classes.length; i++) {
			
			if(args[i] == null) continue;
			
			if( ! classes[i].isInstance(args[i]) ) {
				if(throwException) throw new RuntimeException("Method: ${method} - argument ${i} is not an instance of ${classes[i].canonicalName}: ${args[i].class.canonicalName}")
				return false;
			}
		}
		
		return true;
		
	}
		
}
