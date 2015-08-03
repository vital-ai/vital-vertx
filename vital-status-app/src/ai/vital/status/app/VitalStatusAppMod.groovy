package ai.vital.status.app

import groovy.json.JsonBuilder;
import groovy.json.JsonSlurper;

import java.nio.charset.StandardCharsets;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.vertx.groovy.platform.Verticle;
import org.vertx.groovy.core.AsyncResult
import org.vertx.groovy.core.eventbus.Message
import org.vertx.java.core.eventbus.Message as JMessage
import org.vertx.java.core.Future;
import org.vertx.java.core.MultiMap
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.Handler;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import ai.vital.service.superadmin.vertx.VitalServiceSuperAdminMod;
import ai.vital.service.vertx.json.VitalServiceJSONMapper;
import ai.vital.vitalservice.VitalStatus;
import ai.vital.vitalservice.model.Customer;
import ai.vital.vitalservice.query.ResultList


/**
 * Singleton verticle, should be launched only once
 * @author Derek
 *
 */
class VitalStatusAppMod extends Verticle implements Handler<HttpServerRequest>{

	private final static Logger log = LoggerFactory.getLogger(VitalStatusAppMod.class)
	
	private Future<Void> startedResult
	
	static boolean initialized = false;
	static boolean beingInitialized = false;
	
	Map users = [:]
	
	List<String> testAddresses = []
	
	static Customer customer = new Customer(ID: 'vital')

	static Map customerJSON = null 
	
	static {
		customerJSON = VitalServiceJSONMapper.toJSON(customer)
	}
		
	public static class RequestContext {
		
		HttpServerRequest req
		
		HttpServerResponse res
		
		List<Map> results = []
		
		boolean json
		
		List<String> addresses
		
		String zapierHistoryError = null
		
		List<Map> zapierHistory
		
	}
	
	@Override
	public Object start(Future<Void> startedResult) {
		
		log.info("Starting ${VitalStatusAppMod.class.canonicalName} ...")

		if(initialized) throw new RuntimeException(VitalStatusAppMod.class.getCanonicalName() + " already initialized")
		if(beingInitialized) throw new RuntimeException(VitalStatusAppMod.class.getCanonicalName() + " already being initialized")
		
		beingInitialized = true

		this.startedResult = startedResult
				
		deployRESTServer()
		
		return startedResult
		
	}
	
	void deployRESTServer() {

		Map config = container.getConfig()
		
		
		Object _addresses = config.get('test_addresses')
		
		if(_addresses == null) throw new RuntimeException("No 'test_addresses' config param")
		if(!(_addresses instanceof List)) throw new RuntimeException("'test_addresses' must be a list of strings")
		
		List _addressesL = _addresses
		if(_addressesL.size() < 1) throw new RuntimeException("'test_addresses' must not be empty")
		testAddresses.addAll(_addressesL)
		
		log.info("Test addresses [${testAddresses.size()}]: ${testAddresses}")
		
		
		Object portObj = config.get('port')
		if(portObj == null) throw new RuntimeException("No 'port' config param")
		if(!(portObj instanceof Integer)) throw new RuntimeException("'port' param must be an integer, got ${portObj}")
		
		Object hostObj = config.get('host')
		if(hostObj == null) throw new RuntimeException("No 'host' config param")
		if(!(hostObj instanceof String)) throw new RuntimeException("'host' param must be a string, got ${hostObj}")
		
		def server = vertx.createHttpServer()
		def javaServer = server.toJavaServer()

		Object sslObj = config.get('ssl')
		if(sslObj == null) throw new RuntimeException("No 'ssl' config param")
		if(!(sslObj instanceof Boolean)) throw new RuntimeException("'ssl' param must be a boolean, got ${sslObj}")
		
		if(sslObj == true) {
			
			Object key_store_path = config.get('key_store_path')
			if(key_store_path == null) throw new RuntimeException("No 'key_store_path' config param")
			if(!(key_store_path instanceof String)) throw new RuntimeException("'key_store_path' must be a string, got ${key_store_path}")
			
			
			Object key_store_password = config.get('key_store_password')
			if(key_store_password == null) throw new RuntimeException("No 'key_store_password' config param")
			if(!(key_store_password instanceof String)) throw new RuntimeException("'key_store_password' must be a string, got ${key_store_password}")

			javaServer.setSSL(true)			
			javaServer.setKeyStorePath(key_store_path)
			javaServer.setKeyStorePassword(key_store_password)
			
		}
		
		Object usersObj = config.get("users")
		
		if(usersObj == null) throw new RuntimeException("No 'users' config param")
		if(!(usersObj instanceof Map)) new RuntimeException("'users' param must be a map, got ${usersObj}")
		
		for(Entry entry : ((Map)usersObj).entrySet()) {
			
			Object userObj = entry.key
			if(!(userObj instanceof String)) throw new RuntimeException("'users' map keys must be strings, got : ${userObj}")
			Object passwordObj = entry.value
			if(!(passwordObj instanceof String)) throw new RuntimeException("'users' map values must be strings, got : ${userObj}")
			
			if(users.containsKey(userObj)) throw new RuntimeException("More than 1 user entry: ${userObj}")
			
			users.put(userObj, passwordObj)
			
		}
		
		if(users.size() < 1) throw new RuntimeException("No users defined!")
		
		
		javaServer.requestHandler(this)

		server.listen((Integer)portObj, (String) hostObj) { AsyncResult asyncResult ->
			if( asyncResult.succeeded ) {
				log.info("Status rest server started successfully")
				onAllModulesDeployed()
			} else {
				log.error("Status rest server failed to start: ", asyncResult.cause)
				startedResult.setFailure(asyncResult.cause)
			}
		}
				
		
	}
	
	private void onAllModulesDeployed() {
		
			log.info("All verticles deployed: " + VitalStatusAppMod.class.canonicalName)
			
			this.startedResult.setResult(null)
			
			initialized = true
			beingInitialized = false
				
	}

	@Override
	public void handle(HttpServerRequest req) {

		String path = req.path();
		
		HttpServerResponse res = req.response()
		
		MultiMap headers =  req.headers()

		String hs = ""		
		for(Entry<String, String> e : headers.entries()) {
			if(hs.length() > 0) hs += "   "
			hs += (e.key + ":" + e.value )
		}
		
//		log.info("HEADERS: ${hs}")
		String userAgent = headers.get("User-Agent")
		if(userAgent.toLowerCase().contains("zapier")) {
			
			log.info("Zapier headers: ${hs}")
			
			vertx.eventBus.send(VitalServiceSuperAdminMod.ADDRESS, ['method': 'callFunction', 'args': [customerJSON, null, 'commons/scripts/ZapierBucketScript', ['action': 'zapierPing']]]) { Message message ->
				
				Map functionStatus = message.body
				
				if(functionStatus.status == 'ok') {
					log.info('action=zapierPing success')
				} else {
					log.error('action=zapierPing failed: ' + functionStatus.message)
				}
				
			}
			
		}
		
		
		req.resume()
		
		if(path == '/' || path == '/index.html' || path == '/json') {
			
			//filter
			String auth = req.headers().get("Authorization")
			
			boolean json = path == '/json'
			
			try {
				
				if(auth == null || auth.isEmpty()) {
					throw new Exception("No authentication string") 
					return;
				}
				
				if(!auth.startsWith("Basic ")) {
					throw new Exception("Bad auth string, must start with 'Basic '")
				}
				
				String namePass = new String( Base64.decodeBase64(auth.substring("Basic ".length())), StandardCharsets.UTF_8 );
				
				String[] cols = namePass.split(":")
				
				if(cols.length != 2) throw new Exception("Expected username:password in auth string")
				
				String username = cols[0]
				
				String password = cols[1]
				
				String p = users.get(username)				
				
				if(p == null) throw new Exception("User not found: ${username}")
				
				if(!p.equals(password)) throw new Exception("Invalid password.")
				
			} catch(Exception e) {
				res.setStatusCode(401).putHeader("WWW-Authenticate", 'Basic realm="Vital Status"').end(e.localizedMessage)
				return
			}
			 			
			
			
			//validate request
			
			RequestContext ctx = new RequestContext()
			ctx.req = req
			ctx.res = res
			ctx.json = json
			ctx.addresses = new ArrayList(testAddresses)
			vertx.eventBus.send(VitalServiceSuperAdminMod.ADDRESS, [method: 'ping', args: []]) { Message selectMessage ->
				
				Map pingStatus = selectMessage.body
				pingStatus.put("name", "VitalService Ping")
				pingStatus.id = "" + System.currentTimeMillis()  + RandomStringUtils.randomNumeric(4)
				
				ctx.results.add(pingStatus)
				
				onServicePingStatus(ctx)
				
				
			} 
			
			
		/*
		} else if(path.equals("/setError")) {
			
			this.errorStatus = true
			
			res.end("Error status set to true")
			return
			
		} else if(path.equals("/resetError")) {
		
			this.errorStatus = false
			res.end("Error status set to false")
			return
		*/
		} else {
		
			res.setStatusCode(404).end("Not found")
		
		}
		
	}
	
	
	void onServicePingStatus(RequestContext ctx) {
		
		vertx.eventBus.send(VitalServiceSuperAdminMod.ADDRESS, ['method': 'callFunction', 'args': [customerJSON, null, 'commons/scripts/ZapierStatusScript', [:]]]) { Message message ->
			
			Map functionStatus = message.body
			
			Map status = null;
			
			if(functionStatus.status == 'ok') {
				
				ResultList rl = VitalServiceJSONMapper.fromJSON(functionStatus.response)
				
				status = [status: rl.status?.status, message: rl.status?.message]
				
			} else {
			
				status = functionStatus
			
			}
			
			status.name = "Datascript call Status"
			status.id = "" + System.currentTimeMillis() + RandomStringUtils.randomNumeric(4)
			
			if(status.status == null) {
				status.status = "error"
				status.message = "No status returned by the service"
			}
			
			ctx.results.add(status)
			
			checksAddresses(ctx)
			
		}
		
		
	}
	
	void checksAddresses(RequestContext ctx) {
		
		if(ctx.addresses.size() > 0) {
			
			String nextAddress = ctx.addresses.remove(0)
			
			//send empty json object
			def handler = new Handler<org.vertx.java.core.AsyncResult<JMessage<JsonObject>>>() {
				
				public void handle(Object resultO) {
					org.vertx.java.core.AsyncResult<JMessage<JsonObject>> result = resultO;
					
					Map m = null
					
					if(result.succeeded()) {
					
						m = result.result().body().toMap()
						
						
					} else {

						if(result.result() != null && result.result().body() != null) {
							m = result.result().body().toMap()
						} else {
							m = [status: 'error', message: "MODULE NOT FOUND"]
						}
					
					
					}
					if(m.status == null) {
						m.status = "error"
						m.message = "Address: ${nextAddress} didn't return any status"
					}
					
					m.name = nextAddress
					m.id = "" + System.currentTimeMillis() + RandomStringUtils.randomNumeric(4)
					
					ctx.results.add(m)
					
					checksAddresses(ctx)
					
				}
				
			};
			vertx.eventBus.javaEventBus().sendWithTimeout(nextAddress, new JsonObject(), 500, handler);
			/*
			vertx.eventBus.javaEventBus().sendWsend(nextAddress, [:]) { Message response ->
				
				Map addressResponse = response.body
				
				if(addressResponse.status == null) {
					addressResponse.status = "error"
					addressResponse.message = "Address: ${nextAddress} didn't return any status"
				}
				
				addressResponse.put("name", "${addresses}")
				addressResponse.id = "" + System.currentTimeMillis() 
				
				r.add(addressResponse)
				
				checksAddresses(res, json, r, addresses)
			
			}
			*/

			return
						
		} else {
		
		
			if(ctx.json) {
				
				onRender(ctx)
				
			} else {
			
				//ask for zapier ping history data
			
				vertx.eventBus.send(VitalServiceSuperAdminMod.ADDRESS, ['method': 'callFunction', 'args': [customerJSON, null, 'commons/scripts/ZapierBucketScript', ['action': 'zapierPingHistory']]]) { Message message ->
					
					Map functionStatus = message.body
					
					if(functionStatus.status == 'ok') {
						
						ResultList rl = VitalServiceJSONMapper.fromJSON(functionStatus.response)
						
						if(rl.status != null && rl.status.status != VitalStatus.Status.ok) {
							
							ctx.zapierHistoryError = rl.status.message
							
						} else {
						
							JsonSlurper slurper = new JsonSlurper()
							
							ctx.zapierHistory = slurper.parseText(rl.status.message)
							
						}
						
					} else {
					
						ctx.zapierHistoryError = functionStatus.message
						
					
					}
					
					onRender(ctx)
					
				}

			
			}
			
			
		
		
		}
		
	}
	
	void onRender(RequestContext ctx) {
		
		def res = ctx.res
		
		if(ctx.json) {
			
			
//			pingStatus.status = errorStatus ? "error" : "ok"
			
			def builder = new JsonBuilder(ctx.results)
			
			res.putHeader("Content-Type", "application/json")
			
			res.end(builder.toPrettyString())
			
		} else {
			res.putHeader("Content-Type", "text/html; charset=UTF-8")
			
			String t = '';
			int i = 1
			for(Map s : ctx.results) {
				
				t += "<tr><td>${i++}</td><td>${s.name}</td><td>${s.status}</td><td>${s.message}</td></tr>\n"
				
			}
			
			String zh = null
			
			if(ctx.zapierHistory != null) {
			
				String zhr = ''
				
				for(Map h : ctx.zapierHistory) {
					
					Date startDate = new Date( h.startDate)
					Date endDate = h.endDate ? new Date(h.endDate) : null
					
					zhr += "<tr><td>${startDate}</td><td>${endDate ? endDate : 'current'}</td><td>${h.value}</td></tr>\n"
					
				}
					
				zh = """\
<table>
	<tr>
		<th>Start</th>
		<th>End</th>
		<th>Count</th>
	<tr>
	${zhr}
</table>		
"""
				
				
			} else {
			
				zh += "<p>Zapier history failed: ${ctx.zapierHistoryError}</p>"
			
			}
			
			
						
			String h = """\
<!DOCTYPE html>
<html>
<head>
<title>Vital Status</title>
</head>

<body>
<h3>Vital Status</h3>
<h5>Components</h5>
<table>
	<tr>
		<th>&nbsp;</th>
        <th>Name</th>
		<th>Status</th>
		<th>Message</th>
	</tr>
	${t}
</table>

<h5>Zapier ping bucket history</h5>
${zh}

</body>
</html> 
"""


			res.end(h)
			
		}
		
		
		
	}
}
