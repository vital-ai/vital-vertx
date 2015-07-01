package ai.vital.service.vertx.handler.functions

import java.util.Map

import ai.vital.service.vertx.handler.AbstractVitalServiceHandler;
import ai.vital.service.vertx.handler.CallFunctionHandler;
import ai.vital.vitalservice.VitalStatus;
import ai.vital.vitalservice.exception.VitalServiceException;
import ai.vital.vitalservice.exception.VitalServiceUnimplementedException;
import ai.vital.vitalservice.model.App;
import ai.vital.vitalservice.model.Organization;
import ai.vital.vitalservice.query.ResultList;


class VertxRegisterImpl extends VertxHandler {

	public VertxRegisterImpl(AbstractVitalServiceHandler handler) {
		super(handler);
	}

	@Override
	public ResultList callFunction(Organization organization, App app,
			String function, Map<String, Object> params)
			throws VitalServiceUnimplementedException, VitalServiceException {

		String functionName = getRequiredStringParam('functionName', params)
		
		def handlerClass = params.get('handlerClass')
		if(handlerClass == null) throw new RuntimeException("No 'handlerClass' param")
		if(!(handlerClass instanceof String)) throw new RuntimeException("'handlerClass' must be a string")
		
		if( CallFunctionHandler.allHandlers.contains( functionName ) )
			throw new Exception("Function name is forbidden: ${functionName}" )
		
		if( handler.callFunctionHandlers.containsKey(functionName)) throw new Exception("Function already registered: ${functionName}")
		
		CallFunctionHandler handlerInstance = null
		try {
			Class cls = Class.forName(handlerClass)
			
			if(!CallFunctionHandler.class.isAssignableFrom(cls)) {
				throw new Exception("Handler class does not implement ${CallFunctionHandler.class.canonicalName}")
			}
			
			handlerInstance = cls.newInstance()
			
		} catch(Exception e) {
			throw new RuntimeException("handler class incorrect - " + e.getClass().getCanonicalName() + ' ' + e.localizedMessage, e)
		}
		
		handler.callFunctionHandlers.put(functionName, handlerInstance)
		
		ResultList rl = new ResultList()
		rl.setStatus(VitalStatus.withOKMessage("function registered: ${functionName} -> ${handlerClass}"))
				
		return rl;
	}

}
