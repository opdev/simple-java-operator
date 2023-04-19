package io.opdev;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    if (resource.getStatus().getSpecMessage() != desiredMsg){ 
       resource.getStatus().setSpecMessage(desiredMsg);
       return UpdateControl.updateStatus(resource);
    }

    return UpdateControl.noUpdate();
  }

}

