/**
 * Websocket based implementation
 * @param successCB
 * @param errorCB
 * @returns
 */
VitalServiceWebsocketImpl = function(address, successCB, errorCB) {
	
	this.address = address;
	
	this.connected = false;
	var _this = this;
	
	if(vertx == null || vertx.EventBus == null) {
		throw 'vertx.EventBus module not loaded!' 
	}
	
	this.eb = new vertx.EventBus(window.location.protocol + '//' + window.location.hostname + ':' + window.location.port + '/eventbus');
	this.eb.onopen = function() {
		console.log("Event bus connected");
		_this.connected = true;
		successCB();
	};
	
	this.eb.onclose = function() {
		errorCB();
	}
	
	if(typeof( VitalServiceJson ) != 'undefined') {
		console.log("loading optional json validation module...");
		
		this.vsJson = new VitalServiceJson();
		
	} else {
		console.warn("VitalServiceJson module not available, validation disabled.");
	}
	
	
}

/**
 * Calls the service method, all input parameters are validated against json schema - same 
 */
VitalServiceWebsocketImpl.prototype.callMethod = function(method, args, successCB, errorCB){
	
	if(typeof(successCB) != "function") {
		alert("method: " + method + " - Success callback not a function, arguments list invalid");
		return
	}
	
	if(typeof(errorCB) != "function") {
		alert("method: " + method + " - Error callback not a function, arguments list invalid");
		return
	}
	
	var data = {
		method: method,
		args: args
	};
	
	var _this = this;
	
	this.eb.send(this.address, data, function(result) {
		
		console.log(method + ' result: ', result);
		
		//check the status, then object
		
		if(result.status == 'ok') {
			
			//validate response
			
			var response = result.response;
			
			if(response != null) {
				
				//check response type
				if(_this.vsJson != null) {
					var validationError = _this.vsJson.validateResponse(response);
					
					if(validationError != null) {
						errorCB(validationError);
						return;
						
					}
					
				}
				
			}
			
			successCB(result.response);
			
		} else {
			
			errorCB(result.message)
			
		}
		
	});
	
}

