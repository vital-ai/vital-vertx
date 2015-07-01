package ai.vital.vertx.launcher.verticle

import com.typesafe.config.Config
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigObject
import com.typesafe.config.ConfigValue

import java.util.Map.Entry

import org.vertx.groovy.platform.Verticle;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Future;
import org.slf4j.Logger
import org.slf4j.LoggerFactory;

class VitalVertxLauncherVerticle extends Verticle {

	private final static Logger log = LoggerFactory.getLogger(VitalVertxLauncherVerticle.class)
	private List<Entry<String, ConfigValue>> entries = null
	
	private int currentIndex = -1
	
	private Future<Void> startedResult
	
	private Config config
	
	@Override
	public Object start(Future<Void> startedResult) {

		this.startedResult = startedResult
		
		log.info("Starting launcher verticle singleton...")
		
		String configPath = container.config.get("configPath");
		
		File configFile = new File(configPath);
		
		if(!configFile.exists()) throw new RuntimeException("Config file not found: ${configFile.absolutePath}")
		
		this.config = ConfigFactory.parseFile(configFile)
		
		config.resolve()
		
		ConfigObject configObject = config.root()

				
		for(Entry<String, ConfigValue> entry : configObject.entrySet()) {
			
			//deploy them in the specific order ?
			if(!entry instanceof ConfigObject) {
				throw new RuntimeException("Config may only contain config objects...")				
			}
			
			ConfigObject co = (ConfigObject)entry.getValue()
			
			Config mc = config.getConfig("\"" + entry.getKey() + "\"");
			
			boolean enabled = true
			
			try {
				enabled = mc.getBoolean("enabled")
			} catch(ConfigException.Missing ce) {}
			
			
			List<String> dependencies = []
			try {
				dependencies = mc.getStringList("dependencies");
			} catch(ConfigException.Missing ce){}
			
			int instances = -1
			try {
				instances = mc.getInt("instances")
				if(instances < 1) throw new RuntimeException("instances param cannot be lesser than 1, module: " + entry.key)
			} catch(ConfigException.Missing ce){
				throw new RuntimeException("No instances int param in module: " + entry.key)
			}
			
			log.info("Module ${entry.key}, enabled ? ${enabled}, dependencies: ${dependencies}, instances : ${instances}")
			
			for(String dependency : dependencies) {

				if(dependency.equals(entry.key)) {
					throw new RuntimeException("Dependency pointing to self! ${entry.key}") 
				}
								
				if(!configObject.containsKey(dependency)) throw new RuntimeException("Dependency not found: ${dependency}")
				
				Config dc = config.getConfig("\"" + dependency + "\"");
				
				boolean dcEnabled = true
				
				try {
					dcEnabled = dc.getBoolean('enabled')
				} catch(ConfigException.Missing ce){}
				
				if(!dcEnabled) throw new RuntimeException("Dependency module disabled: ${dependency}")
				
			}
			
			
		}
		
		entries = new ArrayList<Entry<String, ConfigValue>>(configObject.entrySet())
		
		
		//TODO add cycle detection, sorting based on dependencies, for now quick sort based on deps lenght
		
		entries.sort { Entry<String, ConfigValue> e1, Entry<String, ConfigValue> e2 ->
			
			Config mc1 = config.getConfig("\"" + e1.getKey() + "\"");
			Config mc2 = config.getConfig("\"" + e2.getKey() + "\"");
			
			List<String> dependencies1 = []
			try {
				dependencies1 = mc1.getStringList("dependencies");
			} catch(ConfigException.Missing ce){}
			
			List<String> dependencies2 = []
			try {
				dependencies2 = mc2.getStringList("dependencies");
			} catch(ConfigException.Missing ce){}
				
			return new Integer(dependencies1.size()).compareTo(dependencies2.size())
		}
		
		processNextEntry()
		
	}
	
	private void processNextEntry() {
			
		currentIndex++
			
		if(currentIndex < entries.size()) {
				
			Entry<String, ConfigValue> entry = entries.get(currentIndex)
				
			log.info("Deploying module ${currentIndex+1} of ${entries.size()}: ${entry.key} ...")
				
			ConfigObject co = (ConfigObject)entry.value
				
			Config mc = config.getConfig("\"" + entry.key + "\"")
			
			boolean enabled = true
			
			try {
				enabled = mc.getBoolean("enabled")
			}catch(ConfigException ce) {}
			
			if(!enabled) {
				processNextEntry()
				return
			}
				
			int instances = mc.getInt("instances")
			
			Map cfg = co.unwrapped()	
			
			container.deployModule(entry.key, cfg, instances) { AsyncResult<String> res ->
					
				if(res.succeeded) {
					
					log.info("Module ${entry.key} deployed successfully")
					
					processNextEntry()
						
				} else {
					log.error("Module ${entry.key} failed", res.cause())
					startedResult.setFailure(res.cause())
				}
				
			}
				
				
		} else {
			
			log.info("All modules deployed successfully [${entries.size()}]")
			
			startedResult.setResult(null);
				
		}

	}			
}
