/**
 * VitalService javascript interface
 */
VitalServiceAdmin = function(successCB, errorCB) {
	
	//the vitalservice is initialized asynchronously
	this.impl = new VitalServiceWebsocketImpl('vitalserviceadmin', successCB, errorCB);
	
}


/**
 * Adds app
 * returns VitalStatus
 */
VitalServiceAdmin.prototype.addApp = function(app, successCB, errorCB) {
	this.impl.callMethod('addApp', [app], successCB, errorCB);
}


/**
 * Adds segment
 * returns segment
 */
VitalServiceAdmin.prototype.addSegment = function(app, segment, createIfNotExists, successCB, errorCB) {
	this.impl.callMethod('addSegment', [app, segment, createIfNotExists], successCB, errorCB);
}



/**
 * Calls datascript with name and params
 * returns ResultList
 */
VitalServiceAdmin.prototype.callFunction = function(app, functionName, paramsMap, successCB, errorCB) {
	this.impl.callMethod('callFunction', [app, functionName, paramsMap], successCB, errorCB);
}

/**
 * Deletes a single VitalURI or List of VitalURIs
 * returns VitalStatus
 * 
 */
VitalServiceAdmin.prototype.delete_ = function(app, vitalURIorList, successCB, errorCB) {
	this.impl.callMethod('delete', [app, vitalURIorList], successCB, errorCB);
}


/**
 * Deletes a list of graph objects
 */
VitalServiceAdmin.prototype.deleteObjects = function(app, graphObjectsList, successCB, errorCB) {
	this.impl.callMethod('deleteObjects', [app, graphObjectsList], successCB, errorCB);
}


/**
 * Deletes expanded either a single VitalURI or List of VitalURIs
 * returns VitalStatus 
 */
//VitalServiceAdmin.prototype.deleteExpanded = function(app, vitalURIorList, successCB, errorCB) {
//	this.impl.callMethod('deleteExpanded', [app, vitalURIorList], successCB, errorCB);
//}

/**
 * Deletes expanded in segments list (VitalSegment type) either a single VitalURI or List of VitalURIs
 * returns VitalStatus 
 */
//VitalServiceAdmin.prototype.deleteExpandedInSegments = function(app, vitalURIorList, segmentsList, successCB, errorCB) {
//	this.impl.callMethod('deleteExpandedInSegments', [app, vitalURIorList, segmentsList], successCB, errorCB);
//}

/**
 * Deletes expanded in segments list (VitalSegment type) with paths (either a single VitalURI or List of VitalURIs
 * returns VitalStatus 
 */
//VitalServiceAdmin.prototype.deleteExpandedInSegmentsWithPaths = function(app, vitalURIorList, segmentsList, paths, successCB, errorCB) {
//	this.impl.callMethod('deleteExpandedInSegmentsWithPaths', [app, vitalURIorList, segmentsList, paths], successCB, errorCB);
//}

/**
 * Deletes expanded with paths (either a single VitalURI or List of VitalURIs
 * returns VitalStatus 
 */
//VitalServiceAdmin.prototype.deleteExpandedWithPaths = function(app, vitalURIorList, paths, successCB, errorCB) {
//	this.impl.callMethod('deleteExpandedWithPaths', [app, vitalURIorList, paths], successCB, errorCB);
//}

/**
 * Generates a new URI for given class (class object)
 * returns VitalURI
 */
VitalServiceAdmin.prototype.generateURI = function(app, classObject, successCB, errorCB) {
	this.impl.callMethod('generateURI', [app, classObject], successCB, errorCB);
}

/**
 * Gets a GraphObject or list, input is either VitalURI or list of VitalURIs accordingly
 */
VitalServiceAdmin.prototype.get = function(app, vitalURIorList, successCB, errorCB) {
	//always service wide context!
	this.impl.callMethod('get', [app, vitalURIorList, {type: 'GraphContext', value: 'ServiceWide'}, []], successCB, errorCB);
}

/**
 * Executes graph query
 * returns ResultList
 */
VitalServiceAdmin.prototype.graphQuery = function(app, graphQuery, successCB, errorCB) {
	this.impl.callMethod('graphQuery', [app, graphQuery], successCB, errorCB);
}

/**
 * List apps
 * returns list of App
 */
VitalServiceAdmin.prototype.listApps = function(successCB, errorCB) {
	this.impl.callMethod('listApps', [], successCB, errorCB);
}

/**
 * List segments
 */
VitalServiceAdmin.prototype.listSegments = function(app, successCB, errorCB) {
	this.impl.callMethod('listSegments', [app], successCB, errorCB);
}


/**
 * Pings the service 
 * returns VitalStatus
 */
VitalServiceAdmin.prototype.ping = function(successCB, errorCB) {
	this.impl.callMethod('ping', [], successCB, errorCB)
}

/**
 * Removes app
 * returns VitalStatus
 */
VitalServiceAdmin.prototype.removeApp = function(app, successCB, errorCB) {
	this.impl.callMethod('removeApp', [app], successCB, errorCB)
}

/**
 * Removes segment
 * returns VitalStatus
 */
VitalServiceAdmin.prototype.removeSegment = function(app, segment, successCB, errorCB) {
	this.impl.callMethod('removeApp', [app, segment], successCB, errorCB)
}


/**
 * Saves a graph object or objects list 
 */
VitalServiceAdmin.prototype.save = function(app, segment, graphObjectOrList, successCB, errorCB) {
	this.impl.callMethod('save', [app, segment, graphObjectOrList], successCB, errorCB)
}

/**
 * Executes select query
 * returns ResultList
 */
VitalServiceAdmin.prototype.selectQuery = function(app, selectQuery, successCB, errorCB) {
	this.impl.callMethod('selectQuery', [app, selectQuery], successCB, errorCB);
}

