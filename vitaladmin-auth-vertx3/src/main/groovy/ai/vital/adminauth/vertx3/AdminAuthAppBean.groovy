package ai.vital.adminauth.vertx3

import ai.vital.auth.vertx3.VitalAuthManager.AuthAppBean
import ai.vital.service.admin.vertx3.VitalServiceAdminVertx3;
import ai.vital.service.admin.vertx3.async.VitalServiceAdminAsyncClient
import ai.vital.vitalservice.admin.VitalServiceAdmin
import ai.vital.vitalservice.factory.VitalServiceFactory;
import ai.vital.vitalservice.query.VitalSelectQuery
import ai.vital.vitalsigns.meta.GraphContext;
import ai.vital.vitalsigns.model.GraphObject
import ai.vital.vitalsigns.model.VitalSegment
import ai.vital.vitalsigns.model.property.URIProperty
import io.vertx.groovy.core.Vertx
import io.vertx.groovy.core.eventbus.Message

public class AdminAuthAppBean extends AuthAppBean {

		VitalServiceAdmin vitalServiceAdminSync  
		
		VitalServiceAdminAsyncClient vitalServiceAdmin
		
		@Override
		protected void _initService(Vertx vertx) {
			
			this.vertx = vertx
			
			for(VitalServiceAdmin vsa : VitalServiceFactory.listOpenAdminServices()) {
				if(vsa.getName() == VitalServiceAdminVertx3.SERVICE_NAME) {
					vitalServiceAdminSync = vsa
				}
			}
			
			if(vitalServiceAdminSync == null) throw new RuntimeException("AppID: ${appID} service admin instance not found")
			
			vitalServiceAdmin = new VitalServiceAdminAsyncClient(vertx)
			
			app = vitalServiceAdminSync.getApp(appID)
			
			if(app == null) throw new RuntimeException("App not found: ${appID}")
			
		}

		@Override
		protected VitalSegment _getSegment(String segmentID) {
			return vitalServiceAdminSync.getSegment(app, segmentID)
		}

		@Override
		protected boolean _supportsNormal() {
			return false
		}

		@Override
		protected boolean _supportsAdmin() {
			return true
		}

		@Override
		protected boolean _supportsSuperAdmin() {
			return false
		}

		@Override
		protected void _executeSelectQuery(VitalSelectQuery selectQuery,
				Closure closure) {
			vitalServiceAdmin.query(app, selectQuery, closure)
		}

		@Override
		protected void _generateURI(Class<? extends GraphObject> clazz,
				Closure closure) {
			vitalServiceAdmin.generateURI(app, clazz, closure)
		}

		@Override
		protected void _passMessage(Message msg) {
			vertx.eventBus.send(VitalServiceAdminVertx3.ADDRESS, msg.body()) { Message response ->
				
				msg.reply(response.body())
				
			}
		}

		@Override
		protected void _save(VitalSegment targetSegment,
				List<GraphObject> objects, Closure closure) {
			vitalServiceAdmin.save(app, targetSegment, objects, true, closure)
		}

		@Override
		protected void _delete(List<URIProperty> uris, Closure closure) {
			vitalServiceAdmin.delete(app, uris, closure)
		}

		@Override
		protected void _getRemoteObject(String uri, Closure closure) {
			vitalServiceAdmin.get(app, GraphContext.ServiceWide, URIProperty.withString(uri), closure)
		}

		@Override
		protected void _callFunction(String fname, Map<String, Object> params,
				Closure closure) {
			vitalServiceAdmin.callFunction(app, fname, params, closure)
		}

		@Override
		protected VitalSegment _getTargetSegment() {
			return adminLoginsSegment
		}
		
	}