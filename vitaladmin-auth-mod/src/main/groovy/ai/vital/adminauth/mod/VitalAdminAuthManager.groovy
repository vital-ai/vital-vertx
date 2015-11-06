package ai.vital.adminauth.mod

import groovy.lang.Closure;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.githubusercontent.defuse.passwordhash.PasswordHash;
import org.junit.After;
import org.vertx.groovy.core.eventbus.EventBus
import org.vertx.groovy.platform.Verticle
import org.vertx.groovy.core.eventbus.Message;
import org.vertx.java.core.Future;

import ai.vital.auth.mod.VitalAuthManager;
import ai.vital.auth.mod.VitalAuthManager.AuthAppBean
import ai.vital.auth.queries.Queries;
import ai.vital.domain.CredentialsLogin;
import ai.vital.domain.Login;
import ai.vital.domain.AdminLogin
import ai.vital.domain.SuperAdminLogin
import ai.vital.domain.UserSession;
import ai.vital.service.admin.vertx.VitalServiceAdminMod;
import ai.vital.service.admin.vertx.async.VitalServiceAdminAsyncClient;
import ai.vital.service.vertx.VitalServiceMod;
import ai.vital.service.vertx.async.VitalServiceAsyncClient;
import ai.vital.service.vertx.binary.ResponseMessage;
import ai.vital.vitalservice.VitalStatus;
import ai.vital.vitalservice.admin.VitalServiceAdmin;
import ai.vital.vitalservice.factory.VitalServiceFactory;
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

	/*	

	*/
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
	protected AuthAppBean createBean(String access) {
		if(access == 'service') {
			return new AuthAppBean()
		}
		if(access == 'admin') {
			return new AdminAuthAppBean()
		}
		
		throw new RuntimeException("Unsupported access: ${access}")
	}
	
	


		
	
}
