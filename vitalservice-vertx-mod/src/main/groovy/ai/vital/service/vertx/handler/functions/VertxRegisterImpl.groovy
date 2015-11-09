package ai.vital.service.vertx.handler.functions

import java.util.Map

import ai.vital.service.vertx.handler.AbstractVitalServiceHandler;
import ai.vital.service.vertx.handler.ICallFunctionHandler;
import ai.vital.service.vertx.handler.CallFunctionHandler;
import ai.vital.vitalservice.VitalStatus;
import ai.vital.vitalservice.exception.VitalServiceException;
import ai.vital.vitalservice.exception.VitalServiceUnimplementedException;
import ai.vital.vitalservice.query.ResultList;
import ai.vital.vitalsigns.model.VitalApp
import ai.vital.vitalsigns.model.VitalOrganization


class VertxRegisterImpl extends VertxHandler {

	public VertxRegisterImpl(AbstractVitalServiceHandler handler) {
		super(handler);
	}

	@Override
	public ResultList callFunction(VitalOrganization organization, VitalApp app,
			String function, Map<String, Object> params, Map<String, Object> sessionParams)
			throws VitalServiceUnimplementedException, VitalServiceException {

		String functionName = getRequiredStringParam('functionName', params)
		
		def handlerClass = params.get('handlerClass')
		if(handlerClass == null) throw new RuntimeException("No 'handlerClass' param")
		if(!(handlerClass instanceof String)) throw new RuntimeException("'handlerClass' must be a string")
		
		if( CallFunctionHandler.allHandlers.contains( functionName ) )
			throw new Exception("Function name is forbidden: ${functionName}" )
		
		if(app == null) throw new RuntimeException("No app specified")
		String appID = app.appID?.toString()
		if(!appID) throw new RuntimeException("No appID param")
		
		Map<String, ICallFunctionHandler> appHandlers = handler.appFunctionHandlers.get(appID)
		if(appHandlers != null && appHandlers.containsKey(functionName)) {
			throw new Exception("Function already registered: ${functionName}, app: ${appID}")
		}	
		
		
		ICallFunctionHandler handlerInstance = null
		try {
			Class cls = Class.forName(handlerClass)
			
			if(!ICallFunctionHandler.class.isAssignableFrom(cls)) {
				throw new Exception("Handler class does not implement ${CallFunctionHandler.class.canonicalName}")
			}
			
			handlerInstance = cls.newInstance()
			
		} catch(Exception e) {
			throw new RuntimeException("handler class incorrect - " + e.getClass().getCanonicalName() + ' ' + e.localizedMessage, e)
		}
		
		if(appHandlers == null) {
			appHandlers = Collections.synchronizedMap([:])
			handler.appFunctionHandlers.put(appID, appHandlers)
		}
		
		appHandlers.put(functionName, handlerInstance)
		
		ResultList rl = new ResultList()
		rl.setStatus(VitalStatus.withOKMessage("function registered: ${functionName} -> ${handlerClass}, appID: ${appID}"))
				
		return rl;
	}

}
