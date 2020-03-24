package k8s.example.client.k8s;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.google.gson.reflect.TypeToken;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import io.kubernetes.client.util.Watch;
import k8s.example.client.Constants;
import k8s.example.client.models.Registry;
import k8s.example.client.models.RegistryCondition;
import k8s.example.client.models.RegistryStatus;

public class RegistryWatcher extends Thread {
	private final Watch<Registry> watchRegistry;
	private static String latestResourceVersion = "0";
	private CustomObjectsApi api = null;

	RegistryWatcher(ApiClient client, CustomObjectsApi api, String resourceVersion) throws Exception {
		watchRegistry = Watch.createWatch(client,
				api.listClusterCustomObjectCall("tmax.io", "v1", "registries", null, null, null, null, null, resourceVersion, null, Boolean.TRUE, null),
				new TypeToken<Watch.Response<Registry>>() {}.getType());

		this.api = api;
		latestResourceVersion = resourceVersion;
	}
	
	@Override
	public void run() {
		try {
			watchRegistry.forEach(response -> {
				try {
					if (Thread.interrupted()) {
						System.out.println("Interrupted!");
						watchRegistry.close();
					}
				} catch (Exception e) {
					System.out.println(e.getMessage());
				}
				
				
				// Logic here
				try {
					Registry registry = response.object;
					
					if( registry != null) {
						latestResourceVersion = response.object.getMetadata().getResourceVersion();
						String eventType = response.type.toString();
						System.out.println("====================== Registry " + eventType + " ====================== \n" + registry.toString());
						
						switch(eventType) {
						case Constants.EVENT_TYPE_ADDED : 
							if(registry.getStatus() == null ) {
								K8sApiCaller.initRegistry(registry.getMetadata().getName(), registry);
								System.out.println("Creating registry");
							}
							
							break;
						case Constants.EVENT_TYPE_MODIFIED : 
							if( registry.getStatus().getConditions() != null) {
								for( RegistryCondition registryCondition : registry.getStatus().getConditions()) {
									if( registryCondition.getType().equals("Phase")) {
										if (registryCondition.getStatus().equals(RegistryStatus.REGISTRY_PHASE_CREATING)) {
											K8sApiCaller.createRegistry(registry);
											System.out.println("Registry is running");
										}
									}
								}
							}
							
							break;
						case Constants.EVENT_TYPE_DELETED : 
//							K8sApiCaller.deleteRegistry(registry);
							System.out.println("Registry is deleted");
							
							break;
						}						
					}
				} catch (ApiException e) {
					System.out.println("ApiException: " + e.getMessage());
					System.out.println(e.getResponseBody());
				} catch (Exception e) {
					System.out.println("Exception: " + e.getMessage());
					StringWriter sw = new StringWriter();
					e.printStackTrace(new PrintWriter(sw));
					System.out.println(sw.toString());
				} catch (Throwable e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});
		} catch (Exception e) {
			System.out.println("Registry Watcher Exception: " + e.getMessage());
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			System.out.println(sw.toString());
		}
	}

	public static String getLatestResourceVersion() {
		return latestResourceVersion;
	}
}
