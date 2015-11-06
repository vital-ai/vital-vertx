package ai.vital.service.vertx.async

import java.util.Map;

import org.apache.commons.lang.RandomStringUtils;
import org.vertx.groovy.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import ai.vital.service.vertx.AbstractVitalServiceVertxTest;
import ai.vital.service.vertx.binary.ResponseMessage;
import ai.vital.service.vertx.handler.CallFunctionHandler;
import ai.vital.vitalservice.VitalStatus;
import ai.vital.vitalservice.exception.VitalServiceException;
import ai.vital.vitalservice.exception.VitalServiceUnimplementedException;
import ai.vital.vitalservice.query.ResultList;
import ai.vital.vitalsigns.model.VITAL_Node
import ai.vital.vitalsigns.model.VitalApp;

class VitalServiceVertxTest extends AbstractVitalServiceVertxTest {

	VitalServiceAsyncClient asyncClient
	
	//messages test fail in batch test mode, when tested as a single junit it passes ok, 
	//probably multiple vertx platforms mess something up 
	static boolean messagesEnabled = false;
	
	@Override
	protected void setUp() throws Exception {
		
		super.setUp();
		
		asyncClient = new VitalServiceAsyncClient(ltp.vertx, VitalApp.withId('app'))
		 
	}
	
	public void testVitalServiceAsyncClient() {
		
		ResponseMessage r = null
		
		ltp.delayed { ->
			asyncClient.ping { ResponseMessage m ->
				r = m
				ltp.resume()
			
			}
		}
		
		ltp.waitNow()
		
		
		assertNotNull(r)
		assertNotNull(r.response)
		
		
		r = null
		ltp.delayed { ->
			asyncClient.validate { ResponseMessage m ->
				r = m
				ltp.resume()
			} 
		}
		
		ltp.waitNow()
		
		assertNotNull(r)
		assertNotNull(r.response)
		
		
	}
	
	protected ResultList listHandlers() {

		return callFunction(CallFunctionHandler.VERTX_LIST_HANDLERS, [:])
		
	}
	
	protected ResultList registerHandler(String functionName, String handlerClass) {
		return callFunction(CallFunctionHandler.VERTX_REGISTER_HANDLER, [functionName: functionName, handlerClass: handlerClass])
	}
	
	protected ResultList unregisterHandler(String functionName) {
		return callFunction(CallFunctionHandler.VERTX_UNREGISTER_HANDLER, [functionName: functionName])
	}
	
	protected ResultList callFunction(String function, Map<String, Object> params) {
		
		ResponseMessage r = null
				
		ltp.delayed { ->
				
			asyncClient.callFunction(function, params) { ResponseMessage m ->
				
				r = m
				
				ltp.resume()
				
			}
				
		}
		
		ltp.waitNow()
		
		assertNotNull(r)
		assertNull(r.exceptionMessage)
		assertNotNull(r.response)
		
		assertTrue(r.response instanceof ResultList)
		
		return r.response
		
	}
	
	public void testCustomHandlers() {
		
		ResultList rl = listHandlers()
		int initialSize = rl.results.size()
		
		assertTrue(initialSize > 0)
		
		rl = registerHandler(CallFunctionHandler.VERTX_SEND_TO_STREAM, 'someClass')
		
		assertEquals("shouldn't let register reserved functionName", VitalStatus.Status.error, rl.status.status)
		
		rl = registerHandler('f1', 'someClass')
		
		assertEquals("shouldn't let register unknown class", VitalStatus.Status.error, rl.status.status)
		
		rl = registerHandler('f1', FakeHandler.class.canonicalName)
		
		assertEquals("shouldn't let register fake handler class", VitalStatus.Status.error, rl.status.status)
		
		
		rl = callFunction('f1', [:])
		assertEquals("shouldn't let call unknown function f1", VitalStatus.Status.error, rl.status.status)
		
		
		rl = registerHandler('f1', Handler1.class.canonicalName)
		
		assertEquals("should let register handler class: " + rl.status.message, VitalStatus.Status.ok, rl.status.status)
		
		rl = callFunction('f1', [:])
		
		assertEquals("should let call registered function: " + rl.status.message, VitalStatus.Status.ok, rl.status.status)
		
		assertEquals(123, rl.totalResults.intValue())
		
		rl = listHandlers()
		
		boolean found = false
		
		for(VITAL_Node n : rl) {
			if( n.name?.toString() == Handler1.class.canonicalName ) found = true 
		}
		
		assertEquals(initialSize + 1, rl.results.size())
		assertTrue(found)
		
		
		rl = unregisterHandler('f2')
		assertEquals("shouldn't let unregister unknown function f2", VitalStatus.Status.error, rl.status.status)
		
		rl = unregisterHandler(CallFunctionHandler.JS_LIST_STREAM_HANDLERS)
		assertEquals("shouldn't let unregister protected function", VitalStatus.Status.error, rl.status.status)
		
		rl = unregisterHandler('f1')
		assertEquals("should let unregister known function f1 " + rl.status.message, VitalStatus.Status.ok, rl.status.status)
	
		
		rl = listHandlers()
		
		found = false
		
		for(VITAL_Node n : rl) {
			if( n.name?.toString() == Handler1.class.canonicalName ) found = true
		}
		
		assertEquals(initialSize, rl.results.size())
		assertFalse(found)
		
		
		rl = callFunction('f1', [:])
		assertEquals("shouldn't let call unknown function f1", VitalStatus.Status.error, rl.status.status)
		

	}
	
	private ResultList listStreamSubscribers(String streamName) {
		
		ResultList rl = callFunction(CallFunctionHandler.VERTX_STREAM_LIST_STREAM_SUBSCRIBERS, [streamName: streamName])
		
		assertEquals(rl.status.message, VitalStatus.Status.ok, rl.status.status)
		
		return rl
		
	}
	
	private ResultList listSessionSubscriptions(String sessionID) {
		
		ResultList rl = callFunction(CallFunctionHandler.VERTX_STREAM_LIST_SESSION_SUBSCRIPTIONS, [sessionID: sessionID])
		
		assertEquals(rl.status.message, VitalStatus.Status.ok, rl.status.status)
		
		return rl
		
	}
	
	private ResultList streamSubscribe(List<String> streamNames, String sessionID) {
		
		ResultList rl = callFunction(CallFunctionHandler.VERTX_STREAM_SUBSCRIBE, [streamNames: streamNames, sessionID: sessionID])
		
		assertEquals(rl.status.message, VitalStatus.Status.ok, rl.status.status)
		
		return rl
		
	}
	
	private ResultList streamUnsubscribe(List<String> streamNames, String sessionID) {
		
		ResultList rl = callFunction(CallFunctionHandler.VERTX_STREAM_UNSUBSCRIBE, [streamNames: streamNames, sessionID: sessionID])
				
		assertEquals(rl.status.message, VitalStatus.Status.ok, rl.status.status)
				
		return rl
						
	}
	
	private ResultList sendMessage(ResultList msg, String streamName, List<String> sessionIDs) {
		
		ResultList rl = callFunction(CallFunctionHandler.VERTX_SEND_TO_STREAM, [streamName: streamName, sessionIDs: sessionIDs, message: msg])
				
		assertEquals(rl.status.message, VitalStatus.Status.ok, rl.status.status)
				
		return rl
		
	}
	
	public void testStreamHandlers() {
		
		//use two streams
		def stream1 = 'stream1'
		
		def stream2 = 'stream2'
		
		assertEquals("empty stream1", 0, listStreamSubscribers(stream1).results.size())
		assertEquals("empty stream2", 0, listStreamSubscribers(stream2).results.size())
	
		
		String session1 = RandomStringUtils.random(6)	
		String session2 = RandomStringUtils.random(7)	
		String session3 = RandomStringUtils.random(8)
		
		
		ResultList s1s1 = streamSubscribe([stream1], session1)
		
		assertEquals(1, s1s1.totalResults.intValue())
		s1s1 = streamSubscribe([stream1], session1)
		
		assertEquals(0, s1s1.totalResults.intValue())
		
		streamSubscribe([stream2], session1)
		
		streamSubscribe([stream2], session2)
		streamSubscribe([stream1, stream2], session3)
		
		
		assertEquals(1, listSessionSubscriptions(session1).results.size())
		
		assertEquals("stream1", 2, listStreamSubscribers(stream1).results.size())
		assertEquals("stream2", 3, listStreamSubscribers(stream2).results.size())
		
		
		//unsubscribe session3 from stream1
		ResultList s1s3uns = streamUnsubscribe([stream1], session3)
		assertEquals(1, s1s3uns.totalResults.intValue())
		s1s3uns = streamUnsubscribe([stream1], session3)
		assertEquals(0, s1s3uns.totalResults.intValue())
		
		
		
		assertEquals("stream1", 1, listStreamSubscribers(stream1).results.size())
		assertEquals("stream2", 3, listStreamSubscribers(stream2).results.size())
		
		
		
		//now send to stream and make sure everything
		
		Map<String, Integer> stream2Count = [:]
		stream2Count.put(stream1, 0)
		stream2Count.put(stream2, 0)
		
		/*
		def msgHandler = { Message msg ->
			
			JsonObject body = msg.body
			String streamName = body.getString('streamName')
			
			if(streamName ==null) throw new RuntimeException("No streamName property")
			
			stream2Count.put(streamName, stream2Count.get(streamName) + 1)
			
		}
		*/
		
		ltp.vertx.eventBus.registerHandler('stream.' + session1) { Message msg ->
			
			JsonObject body = msg.body
			String streamName = body.getString('streamName')
			
			if(streamName ==null) throw new RuntimeException("No streamName property")
			println "session 1 received msg stream ${streamName}"
			
			synchronized (stream2Count) {
				stream2Count.put(streamName, stream2Count.get(streamName) + 1)
			}
			
		}
		
		ltp.vertx.eventBus.registerHandler('stream.' + session2) { Message msg ->
			
			
			JsonObject body = msg.body
			String streamName = body.getString('streamName')
			
			if(streamName ==null) throw new RuntimeException("No streamName property")
			println "session 2 received msg stream ${streamName}"
			
			synchronized (stream2Count) {
				stream2Count.put(streamName, stream2Count.get(streamName) + 1)
			}
			
		}
		ltp.vertx.eventBus.registerHandler('stream.' + session3) { Message msg ->
			
			JsonObject body = msg.body
			String streamName = body.getString('streamName')
			
			if(streamName ==null) throw new RuntimeException("No streamName property")
			println "session 3 received msg stream ${streamName}"

			synchronized (stream2Count) {
				stream2Count.put(streamName, stream2Count.get(streamName) + 1)
			}
			
		}
		
		Thread.sleep(100)
		

		if(messagesEnabled) {
			
			ResultList m = new ResultList()
			
			ResultList m1RL = sendMessage(m, stream2, null)
			Thread.sleep(100)
			assertEquals(3, m1RL.totalResults.intValue())
			
				
			println "checking messages"
			//3 messages should be delivered 
			Thread.sleep(100)
			assertEquals(0, stream2Count.get(stream1))
			assertEquals(3, stream2Count.get(stream2))
			
			
			ResultList m2RL = sendMessage(m, stream1, [session1, session2])
			assertEquals(1, m2RL.totalResults.intValue())
			Thread.sleep(100)
			//1 message should get to 1
			assertEquals(1, stream2Count.get(stream1))
			assertEquals(3, stream2Count.get(stream2))
			
			
			ResultList m3RL = sendMessage(m, stream1, [session2, session3])
			assertEquals(0, m3RL.totalResults.intValue())
			Thread.sleep(100)
			//0 message should get to 1
			assertEquals(1, stream2Count.get(stream1))
			assertEquals(3, stream2Count.get(stream2))
			
		}	
		
	}
	
}
