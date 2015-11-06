package ai.vital.service.vertx.handler.functions

import java.util.Map
import java.util.Map.Entry

import ai.vital.service.vertx.handler.AbstractVitalServiceHandler;
import ai.vital.service.vertx.handler.CallFunctionHandler
import ai.vital.service.vertx.handler.ICallFunctionHandler
import ai.vital.vitalservice.exception.VitalServiceException;
import ai.vital.vitalservice.exception.VitalServiceUnimplementedException;
import ai.vital.vitalservice.query.ResultElement
import ai.vital.vitalservice.query.ResultList;
import ai.vital.vitalsigns.model.VITAL_Node
import ai.vital.vitalsigns.model.VitalApp
import ai.vital.vitalsigns.model.VitalOrganization

class VertxListHandlersImpl extends VertxHandler {

	public VertxListHandlersImpl(AbstractVitalServiceHandler handler) {
		super(handler);
	}

	@Override
	public ResultList callFunction(VitalOrganization organization, VitalApp app,
			String function, Map<String, Object> params)
			throws VitalServiceUnimplementedException, VitalServiceException {

				
		ResultList rl = new ResultList()
		int i = 0
		for(Entry<String, ICallFunctionHandler> entry : new HashSet(handler.commonFunctionHandlers.entrySet())) {
					
			VITAL_Node handlerNode = new VITAL_Node()
			handlerNode.URI = 'urn:' + entry.getKey()
			handlerNode.name = entry.getValue().getClass().getCanonicalName()
			i++
			
			rl.getResults().add(new ResultElement(handlerNode, 1D))
					
		}
		
		for(Entry<String, Map<String, ICallFunctionHandler>> e : handler.appFunctionHandlers.entrySet() ) {
			
			String appID = e.getKey();
			
			if(app == null || app.appID.toString().equals(appID)) {
				
				for(Entry<String, ICallFunctionHandler> entry : e.getValue()) {
					
					VITAL_Node handlerNode = new VITAL_Node()
					handlerNode.URI = 'urn:' + appID + ':' + entry.getKey()
					handlerNode.name = entry.getValue().getClass().getCanonicalName()
					
					i++
					
					rl.getResults().add(new ResultElement(handlerNode, 1D))
					
				}
				
				
			}
			
		}
	
		rl.setTotalResults(i)
		return rl

	}

}
