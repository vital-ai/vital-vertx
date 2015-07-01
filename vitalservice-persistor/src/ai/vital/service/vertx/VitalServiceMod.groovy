package ai.vital.service.vertx

import groovy.transform.TypeChecked.TypeCheckingInfo;

import org.vertx.groovy.platform.Verticle;

import ai.vital.service.vertx.handler.VitalServiceHandler;

public class VitalServiceMod extends Verticle {
	
	public static String ADDRESS = "vitalservice"
	
	public Object start() {
		
		container.logger.info "Starting Vital Service Verticle  ..."
		
		container.logger.info "Initializing vital service singleton..."
		
		ai.vital.vitalservice.factory.Factory.getVitalService()
		
//executes the service methods
		
		
		VitalServiceHandler handler = new VitalServiceHandler()		
		
//vertx.eventBus.registerHandler("vitalservice") { message ->
//	message.reply("pong!")
//	container.logger.info("Sent back pong groovy!")
//}
		
		container.logger.info "Registering vital service handler..."
		
		vertx.eventBus.registerHandler(ADDRESS) { message ->
			//container.logger.info "Received message: ${message}"
			handler.handle(message)
		}
		
		container.logger.info "Handler registered."
		
		return new Object()
		
	}

	@Override
	public Object stop() {
		
		container.logger.info "Stopping Vital Service module, closing active service instance..."
		ai.vital.vitalservice.factory.Factory.getVitalService().close()
		
		container.logger.info "Stopped"
		return new Object()
				
	}
	
	
	
	
	
}

