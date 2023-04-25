package io.opdev;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.HashMap;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
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
  public UpdateControl<DemoResource> reconcile(DemoResource resource, Context<DemoResource> context) {
    
    log.info("This is the reconciliation loop of the simple-java-operator. demo resource message is {}", resource.getSpec().getMessage());
    if (reconcileDeployment(resource, context) | reconcileService(resource, context) | reconcileStatus(resource,context)){
      return UpdateControl.updateStatus(resource);
    }

    return UpdateControl.noUpdate();
  }

  private boolean reconcileStatus(DemoResource resource, Context<DemoResource> context) {
    String desiredMsg = resource.getSpec().getMessage();
    if (resource.getStatus() == null){
      resource.setStatus(new DemoResourceStatus());
      resource.getStatus().setSpecMessage("");
    }
    if (!resource.getStatus().getSpecMessage().equalsIgnoreCase(desiredMsg)){ 
       resource.getStatus().setSpecMessage(desiredMsg);
       log.info("Setting demo resource status message to {}", desiredMsg);
       return true;
    }
    return false;
  }

  private boolean reconcileService(DemoResource resource, Context<DemoResource> context) {
    String desiredName = resource.getMetadata().getName();

    Service demoService = client.services().withName(desiredName).get();
    if (demoService == null){
      log.info("Creating a service {}", desiredName);
      Map<String,String> labels = createLabels(desiredName);

      demoService = new ServiceBuilder()
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
    return true;
    }
    return false;
  }

  private Map<String, String> createLabels(String labelValue) {
    Map<String,String> labelsMap = new HashMap<>();
    labelsMap.put("owner", labelValue);
    return labelsMap;
  }

  private boolean reconcileDeployment(DemoResource resource, Context<DemoResource> context) {
    String desiredName = resource.getMetadata().getName();

    Deployment demoDeployment = client.apps().deployments().withName(desiredName).get();
    if (demoDeployment == null){
      log.info("Creating a deployment {}", desiredName);
      Map<String,String> labels = createLabels(desiredName);

      demoDeployment = new DeploymentBuilder()
      .withMetadata(createMetadata(resource, labels))
      .withNewSpec()
        .withNewSelector().withMatchLabels(labels).endSelector()
        .withNewTemplate()
            .withNewMetadata().withLabels(labels).endMetadata()
            .withNewSpec()
                .addNewContainer()
                    .withName("busybox").withImage("busybox")
                    .addNewPort()
                        .withName("http").withProtocol("TCP").withContainerPort(8080)
                    .endPort()
                .endContainer()
            .endSpec()
        .endTemplate()
    .endSpec()
      .build();

      client.apps().deployments().resource(demoDeployment).createOrReplace();
      return true;
    }
    return false;
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

