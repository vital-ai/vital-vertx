package ai.vital.service.vertx.handler.functions

import java.util.Map

import ai.vital.service.vertx.handler.AbstractVitalServiceHandler;
import ai.vital.service.vertx.handler.CallFunctionHandler;
import ai.vital.vitalservice.VitalStatus;
import ai.vital.vitalservice.exception.VitalServiceException;
import ai.vital.vitalservice.exception.VitalServiceUnimplementedException;
import ai.vital.vitalservice.query.ResultList;
import ai.vital.vitalsigns.model.VitalApp
import ai.vital.vitalsigns.model.VitalOrganization


class VertxUnregisterImpl extends VertxHandler {

	public VertxUnregisterImpl(AbstractVitalServiceHandler handler) {
		super(handler);
	}

	@Override
	public ResultList callFunction(VitalOrganization organization, VitalApp app,
			String function, Map<String, Object> params)
			throws VitalServiceUnimplementedException, VitalServiceException {

		String functionName = getRequiredStringParam('functionName', params)
		
		if( CallFunctionHandler.allHandlers.contains( functionName ) )
			throw new Exception("Function name is forbidden: ${functionName}" )

		CallFunctionHandler handlerInstance = handler.callFunctionHandlers.remove(functionName)
		
		if(handlerInstance == null) throw new RuntimeException("No handler for function name: ${functionName}")
				
		ResultList rl = new ResultList()
		
		rl.setStatus(VitalStatus.withOKMessage("function unregistered: ${functionName} -> ${handlerInstance.getClass().canonicalName}"))
				
		return rl;
	}

}
