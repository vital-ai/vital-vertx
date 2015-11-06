package ai.vital.service.vertx.handler

import org.vertx.groovy.core.Vertx

/**
 * useful handler for low level implementations like authentication
 * @author Derek
 *
 */
abstract class VertxAwareAsyncCallFunctionHandler implements
		AsyncCallFunctionHandler {

	Vertx vertx

}
