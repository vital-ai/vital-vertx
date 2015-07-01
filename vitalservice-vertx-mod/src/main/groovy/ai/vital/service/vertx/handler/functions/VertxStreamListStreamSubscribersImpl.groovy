package ai.vital.service.vertx.handler.functions

import ai.vital.service.vertx.handler.AbstractVitalServiceHandler
import ai.vital.service.vertx.handler.Subscription
import ai.vital.vitalservice.exception.VitalServiceException
import ai.vital.vitalservice.exception.VitalServiceUnimplementedException
import ai.vital.vitalservice.model.App
import ai.vital.vitalservice.model.Organization
import ai.vital.vitalservice.query.ResultElement
import ai.vital.vitalservice.query.ResultList
import ai.vital.vitalsigns.model.VITAL_Node

class VertxStreamListStreamSubscribersImpl extends VertxHandler {

	public VertxStreamListStreamSubscribersImpl(AbstractVitalServiceHandler handler) {
		super(handler);
	}

	@Override
	public ResultList callFunction(Organization organization, App app,
			String function, Map<String, Object> params)
			throws VitalServiceUnimplementedException, VitalServiceException {

		String streamName = getRequiredStringParam('streamName', params)
		
		ResultList rl = new ResultList()
		
		synchronized (handler.subscriptions) {
				
			for( Subscription s : handler.subscriptions.values()) {
					
				if(s.streamNames.contains(streamName)) {
						
					VITAL_Node n = new VITAL_Node();
					n.timestamp = s.timestamp
					n.URI = 'sessionID:' + s.sessionID
					n.name = s.streamNames.toString()
							
					rl.getResults().add(new ResultElement(n, 1D))
						
				}
					
			}
				
		} 		
		
		rl.setTotalResults(rl.results.size())
		
		return rl;
		
	}

}
