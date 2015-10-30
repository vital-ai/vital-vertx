package ai.vital.adminauth.mod

import groovy.lang.Closure;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.githubusercontent.defuse.passwordhash.PasswordHash;
import org.vertx.groovy.core.eventbus.EventBus
import org.vertx.groovy.platform.Verticle
import org.vertx.groovy.core.eventbus.Message;
import org.vertx.java.core.Future;

import ai.vital.auth.mod.VitalAuthManager;
import ai.vital.auth.queries.Queries;
import ai.vital.domain.CredentialsLogin;
import ai.vital.domain.Login;
import ai.vital.domain.AdminLogin
import ai.vital.domain.SuperAdminLogin
import ai.vital.domain.UserSession;
import ai.vital.service.admin.vertx.async.VitalServiceAdminAsyncClient;
import ai.vital.service.vertx.VitalServiceMod;
import ai.vital.service.vertx.async.VitalServiceAsyncClient;
import ai.vital.service.vertx.binary.ResponseMessage;
import ai.vital.vitalservice.VitalStatus;
import ai.vital.vitalservice.admin.VitalServiceAdmin;
import ai.vital.vitalservice.json.VitalServiceJSONMapper;
import ai.vital.vitalservice.query.ResultList;
import ai.vital.vitalservice.query.VitalSelectQuery
import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.meta.GraphContext;
import ai.vital.vitalsigns.model.GraphObject
import ai.vital.vitalsigns.model.VitalApp
import ai.vital.vitalsigns.model.VitalSegment
import ai.vital.vitalsigns.model.property.URIProperty;



class VitalAdminAuthManager extends VitalAuthManager {

	public final static String admin_address_login     = "admin.vitalauth.login"
	
	public final static String admin_address_logout    = "admin.vitalauth.logout"
	
	public final static String admin_address_authorise = "admin.vitalauth.authorise"
	
	VitalServiceAdminAsyncClient vitalServiceAdmin
	
	VitalApp app
	
	@Override
	protected String getAddress_login() {
		return admin_address_login
	}

	@Override
	protected String getAddress_logout() {
		return admin_address_logout
	}

	@Override
	protected String getAddress_authorise() {
		return admin_address_authorise
	}

	@Override
	protected void initService(Closure callback) {
		
		String appID = container.getConfig().get("app");
		
		if(!appID) {
			callback(new RuntimeException("No 'app' string config parameter"))
			return
		}
		
		vitalServiceAdmin = new VitalServiceAdminAsyncClient(vertx)
		
		vitalServiceAdmin.getApp(appID) { ResponseMessage rm ->
			
			if(rm.exceptionMessage) {
				callback(new RuntimeException("Error when checking app: " + rm.exceptionType + ' - ' + rm.exceptionMessage))
				return
			}
			
			app = (VitalApp) rm.response

			if(app == null) {
				callback(new RuntimeException("App not found: " + appID))
				return
			}
			
			callback(null)			
			
		}
		
	}
	
	@Override
	protected void getSegment(String segmentID, Closure closure) {
		vitalServiceAdmin.getSegment(app, segmentID, closure)
	}

	@Override
	protected void executeSelectQuery(VitalSelectQuery selectQuery,
			Closure closure) {
		vitalServiceAdmin.query(app, selectQuery, closure)
	}

	@Override
	protected void generateURI(Class<? extends GraphObject> clazz,
			Closure closure) {
		vitalServiceAdmin.generateURI(app, clazz, closure)
	}

	@Override
	protected void save(VitalSegment targetSegment, List<GraphObject> objects,
			Closure closure) {
		vitalServiceAdmin.save(app, targetSegment, objects, true, closure)
	}

	@Override
	protected Object delete(List<URIProperty> uris, Closure closure) {
		vitalServiceAdmin.delete(app, uris, closure)
	}

	@Override
	protected Object getRemoteObject(String uri, Closure closure) {
		vitalServiceAdmin.get(app, GraphContext.ServiceWide, URIProperty.withString(uri), closure)
	}

	
	
}
