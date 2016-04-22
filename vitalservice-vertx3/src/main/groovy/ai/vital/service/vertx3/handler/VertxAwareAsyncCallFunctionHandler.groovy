package ai.vital.service.vertx3.handler

import io.vertx.groovy.core.Vertx

/**
 * useful handler for low level implementations like authentication
 * @author Derek
 *
 */
abstract class VertxAwareAsyncCallFunctionHandler implements
		AsyncCallFunctionHandler {

	Vertx vertx

}
