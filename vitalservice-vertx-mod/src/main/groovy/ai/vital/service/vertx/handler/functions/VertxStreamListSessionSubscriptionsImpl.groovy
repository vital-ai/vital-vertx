package ai.vital.service.vertx.handler.functions

import ai.vital.service.vertx.handler.AbstractVitalServiceHandler
import ai.vital.service.vertx.handler.Subscription
import ai.vital.vitalservice.exception.VitalServiceException
import ai.vital.vitalservice.exception.VitalServiceUnimplementedException
import ai.vital.vitalservice.query.ResultElement
import ai.vital.vitalservice.query.ResultList
import ai.vital.vitalsigns.model.VITAL_Node
import ai.vital.vitalsigns.model.VitalApp
import ai.vital.vitalsigns.model.VitalOrganization

class VertxStreamListSessionSubscriptionsImpl extends VertxHandler {

	public VertxStreamListSessionSubscriptionsImpl(AbstractVitalServiceHandler handler) {
		super(handler);
	}

	@Override
	public ResultList callFunction(VitalOrganization organization, VitalApp app,
			String function, Map<String, Object> params)
			throws VitalServiceUnimplementedException, VitalServiceException {

		String sessionID = getRequiredStringParam('sessionID', params)
		
		ResultList rl = new ResultList()
		
		synchronized (handler.subscriptions) {
				
			Subscription s = handler.subscriptions.get(sessionID)
		
			VITAL_Node n = new VITAL_Node();
			n.timestamp = s.timestamp
			n.URI = 'sessionID:' + s.sessionID
			n.name = s.streamNames.toString()
					
			rl.getResults().add(new ResultElement(n, 1D))
				
		
		}
		
		rl.setTotalResults(rl.results.size())
		
		return rl;
		
	}

}
