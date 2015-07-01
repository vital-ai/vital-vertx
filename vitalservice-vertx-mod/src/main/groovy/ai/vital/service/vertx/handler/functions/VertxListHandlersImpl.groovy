package ai.vital.service.vertx.handler.functions

import java.util.Map
import java.util.Map.Entry

import ai.vital.service.vertx.handler.AbstractVitalServiceHandler;
import ai.vital.service.vertx.handler.CallFunctionHandler
import ai.vital.vitalservice.exception.VitalServiceException;
import ai.vital.vitalservice.exception.VitalServiceUnimplementedException;
import ai.vital.vitalservice.model.App;
import ai.vital.vitalservice.model.Organization;
import ai.vital.vitalservice.query.ResultElement
import ai.vital.vitalservice.query.ResultList;
import ai.vital.vitalsigns.model.VITAL_Node

class VertxListHandlersImpl extends VertxHandler {

	public VertxListHandlersImpl(AbstractVitalServiceHandler handler) {
		super(handler);
	}

	@Override
	public ResultList callFunction(Organization organization, App app,
			String function, Map<String, Object> params)
			throws VitalServiceUnimplementedException, VitalServiceException {

				
		ResultList rl = new ResultList()
		int i = 0
		for(Entry<String, CallFunctionHandler> entry : new HashSet(handler.callFunctionHandlers.entrySet())) {
					
			VITAL_Node handlerNode = new VITAL_Node()
			handlerNode.URI = 'urn:' + entry.getKey()
			handlerNode.name = entry.getValue().getClass().getCanonicalName()
			i++
			
			rl.getResults().add(new ResultElement(handlerNode, 1D))
					
		}
	
		rl.setTotalResults(i)
		return rl

	}

}
