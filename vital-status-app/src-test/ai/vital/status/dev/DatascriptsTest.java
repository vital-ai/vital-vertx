package ai.vital.status.dev;

import java.util.HashMap;
import java.util.Map;

import ai.vital.prime.client.java.VitalPrimeClientJavaImpl;
import ai.vital.vitalservice.model.Customer;
import ai.vital.vitalservice.query.ResultList;

public class DatascriptsTest {

	
	public static void main(String[] args) throws Exception {
		
		VitalPrimeClientJavaImpl client = new VitalPrimeClientJavaImpl("http://127.0.0.1:9080/java");
		
		System.out.println( client.ping() );
		
		Customer c = new Customer();
		c.setID("vital");
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("action", "zapierPing");
		ResultList l = client.callFunction(c, null, "commons/scripts/ZapierBucketScript", params);
		System.out.println( l.getStatus().getStatus() + ": " + l.getStatus().getMessage() );
		
		
		Map<String, Object> params2 = new HashMap<String, Object>();
		params2.put("action", "zapierPingHistory");
		ResultList l2 = client.callFunction(c, null, "commons/scripts/ZapierBucketScript", params2);
		System.out.println( l2.getStatus().getStatus() + ": " + l2.getStatus().getMessage() );
		
		
	}
	
}
