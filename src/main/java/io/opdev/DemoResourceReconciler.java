package io.opdev;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.HashMap;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;

public class DemoResourceReconciler implements Reconciler<DemoResource> { 
  private final KubernetesClient client;

  private static final Logger log = LoggerFactory.getLogger(DemoResourceReconciler.class);

  public DemoResourceReconciler(KubernetesClient client) {
    this.client = client;
  }

  @Override
  public UpdateControl<DemoResource> reconcile(DemoResource resource, Context context) {
    
    log.info("This is the reconciliation loop of the simple-java-operator. demo resource message is {}", resource.getSpec().getMessage());
    String desiredMsg = resource.getSpec().getMessage();
    if (resource.getStatus() == null){
      resource.setStatus(new DemoResourceStatus());
    }
    if (!resource.getStatus().getSpecMessage().equalsIgnoreCase(desiredMsg)){ 
       resource.getStatus().setSpecMessage(desiredMsg);
       log.info("Setting demo resource status message to {}", desiredMsg);
       return UpdateControl.updateStatus(resource);
    }

    log.info("Creating a service {}", resource.getMetadata().getName());
    Map<String,String> labels = new HashMap<String,String>();

    Service demoService = new ServiceBuilder()
        .withMetadata(createMetadata(resource, labels))
        .withNewSpec()
            .addNewPort()
                .withName("http")
                .withPort(8080)
            .endPort()
            .withSelector(labels)
            .withType("ClusterIP")
        .endSpec()
        .build();

    client.services().resource(demoService).createOrReplace();

    return UpdateControl.noUpdate();
  }

  private ObjectMeta createMetadata(DemoResource resource, Map<String, String> labels){
    final var metadata=resource.getMetadata();
    return new ObjectMetaBuilder()
        .withName(metadata.getName())
        .addNewOwnerReference()
            .withUid(metadata.getUid())
            .withApiVersion(resource.getApiVersion())
            .withName(metadata.getName())
            .withKind(resource.getKind())
        .endOwnerReference()
        .withLabels(labels)
    .build();
}

}

