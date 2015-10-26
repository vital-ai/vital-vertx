package ai.vital.service.vertx.async

import java.util.Map;

import ai.vital.service.vertx.handler.CallFunctionHandler;
import ai.vital.vitalservice.exception.VitalServiceException;
import ai.vital.vitalservice.exception.VitalServiceUnimplementedException;
import ai.vital.vitalservice.query.ResultList;
import ai.vital.vitalsigns.model.VitalApp
import ai.vital.vitalsigns.model.VitalOrganization

class Handler1 implements CallFunctionHandler {

		@Override
		public ResultList callFunction(VitalOrganization organization, VitalApp app,
				String function, Map<String, Object> params)
				throws VitalServiceUnimplementedException,
				VitalServiceException {

			ResultList rl = new ResultList()
			rl.setTotalResults(123)
			return rl;
		}
		
	}