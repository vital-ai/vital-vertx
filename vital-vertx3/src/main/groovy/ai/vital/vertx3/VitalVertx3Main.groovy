package ai.vital.vertx3

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

/**
 * Main entry point for improved vertx3 apps
 * Sincl
 * @author Derek
 *
 */
class VitalVertx3Main {

	static def main(args) {
		
		if(args.length < 1 || args.length > 2) {
			println "usage: vitalvertx3 <conf_file> [appClasspath]"
			println "     optional appClasspath, 'local' (default) for development or single fat jar mode, or external classpath otherwise"
			System.exit(1)
			return
		}
		
		String appJar = args.length > 1 ? args[1] : 'local'
		boolean local = appJar == 'local'
		println "appJar: ${appJar}"
		println "local mode ? ${local}"
		
		File confFile = new File(args[0])
		println "Config file: ${confFile.absolutePath}"
		
		if(!confFile.exists()) {
			System.err.println "Config file not found: ${confFile.absolutePath}"
			System.exit(1)
			return
		}
		
		if(!confFile.isFile()) {
			System.err.println "Config file location is not a file: ${confFile.absolutePath}"
			System.exit(1)
			return
		}

		Config config = ConfigFactory.parseFile(confFile)		
		
		VitalVertx3App app = new VitalVertx3App().configure(config)
		
		app.start(appJar) { Throwable startError ->
			
			if(startError != null) {
				System.err.println("ERROR when starting app ${app.name}: ${startError.getLocalizedMessage()}")
				startError.printStackTrace()
			} else {
			
				println "App started successfully: ${app.name}"
			
			}
			
		}
		
	}
	
	
}
