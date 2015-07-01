package ai.vital.service.vertx.async

import java.util.Map;

import ai.vital.service.vertx.handler.CallFunctionHandler;
import ai.vital.vitalservice.exception.VitalServiceException;
import ai.vital.vitalservice.exception.VitalServiceUnimplementedException;
import ai.vital.vitalservice.model.App;
import ai.vital.vitalservice.model.Organization;
import ai.vital.vitalservice.query.ResultList;

class Handler1 implements CallFunctionHandler {

		@Override
		public ResultList callFunction(Organization organization, App app,
				String function, Map<String, Object> params)
				throws VitalServiceUnimplementedException,
				VitalServiceException {

			ResultList rl = new ResultList()
			rl.setTotalResults(123)
			return rl;
		}
		
	}