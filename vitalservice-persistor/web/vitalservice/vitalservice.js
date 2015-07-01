/**
 * VitalService javascript interface
 */
VitalService = function(successCB, errorCB) {
	
	//the vitalservice is initialized asynchronously
	this.impl = new VitalServiceWebsocketImpl('vitalservice', successCB, errorCB);
	
}


/**
 * Calls datascript with name and params
 * returns ResultList
 */
VitalService.prototype.callFunction = function(functionName, paramsMap, successCB, errorCB) {
	this.impl.callMethod('callFunction', [functionName, paramsMap], successCB, errorCB);
}

/**
 * Deletes a single VitalURI or List of VitalURIs
 * returns VitalStatus
 * 
 */
VitalService.prototype.delete_ = function(vitalURIorList, successCB, errorCB) {
	this.impl.callMethod('delete', [vitalURIorList], successCB, errorCB);
}


/**
 * Deletes a list of graph objects
 */
VitalService.prototype.deleteObjects = function(graphObjectsList, successCB, errorCB) {
	this.impl.callMethod('deleteObjects', [graphObjectsList], successCB, errorCB);
}


/**
 * Deletes expanded either a single VitalURI or List of VitalURIs
 * returns VitalStatus 
 */
//VitalService.prototype.deleteExpanded = function(vitalURIorList, successCB, errorCB) {
//	this.impl.callMethod('deleteExpanded', [vitalURIorList], successCB, errorCB);
//}

/**
 * Deletes expanded in segments list (VitalSegment type) either a single VitalURI or List of VitalURIs
 * returns VitalStatus 
 */
//VitalService.prototype.deleteExpandedInSegments = function(vitalURIorList, segmentsList, successCB, errorCB) {
//	this.impl.callMethod('deleteExpandedInSegments', [vitalURIorList, segmentsList], successCB, errorCB);
//}

/**
 * Deletes expanded in segments list (VitalSegment type) with paths (either a single VitalURI or List of VitalURIs
 * returns VitalStatus 
 */
//VitalService.prototype.deleteExpandedInSegmentsWithPaths = function(vitalURIorList, segmentsList, paths, successCB, errorCB) {
//	this.impl.callMethod('deleteExpandedInSegmentsWithPaths', [vitalURIorList, segmentsList, paths], successCB, errorCB);
//}

/**
 * Deletes expanded with paths (either a single VitalURI or List of VitalURIs
 * returns VitalStatus 
 */
//VitalService.prototype.deleteExpandedWithPaths = function(vitalURIorList, paths, successCB, errorCB) {
//	this.impl.callMethod('deleteExpandedWithPaths', [vitalURIorList, paths], successCB, errorCB);
//}

/**
 * Generates a new URI for given class (class object)
 * returns VitalURI
 */
VitalService.prototype.generateURI = function(classObject, successCB, errorCB) {
	this.impl.callMethod('generateURI', [classObject], successCB, errorCB);
}

/**
 * Gets a GraphObject or list, input is either VitalURI or list of VitalURIs accordingly
 */
VitalService.prototype.get = function(vitalURIorList, successCB, errorCB) {
	//always service wide context!
	this.impl.callMethod('get', [vitalURIorList, {type: 'GraphContext', value: 'ServiceWide'}, []], successCB, errorCB);
}

/**
 * Executes graph query
 * returns ResultList
 */
VitalService.prototype.graphQuery = function(graphQuery, successCB, errorCB) {
	this.impl.callMethod('graphQuery', [graphQuery], successCB, errorCB);
}

/**
 * List segments
 */
VitalService.prototype.listSegments = function(successCB, errorCB) {
	this.impl.callMethod('listSegments', [], successCB, errorCB);
}


/**
 * Pings the service 
 * returns VitalStatus
 */
VitalService.prototype.ping = function(successCB, errorCB) {
	this.impl.callMethod('ping', [], successCB, errorCB)
}


/**
 * Saves a graph object or objects list 
 */
VitalService.prototype.save = function(segment, graphObjectOrList, successCB, errorCB) {
	this.impl.callMethod('save', [segment, graphObjectOrList], successCB, errorCB)
}

/**
 * Executes select query
 * returns ResultList
 */
VitalService.prototype.selectQuery = function(selectQuery, successCB, errorCB) {
	this.impl.callMethod('selectQuery', [selectQuery], successCB, errorCB);
}

