package io.opdev;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;

public class DemoResourceReconciler implements Reconciler<DemoResource> { 
  private final KubernetesClient client;

  public DemoResourceReconciler(KubernetesClient client) {
    this.client = client;
  }

  // TODO Fill in the rest of the reconciler

  @Override
  public UpdateControl<DemoResource> reconcile(DemoResource resource, Context context) {
    // TODO: fill in logic

    return UpdateControl.noUpdate();
  }
}

