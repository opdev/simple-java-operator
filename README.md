# Simple Java Operator built with Java Operator SDK

## Generate code

```
operator-sdk init --plugins quarkus --domain opdev.io --project-name simple-java
operator-sdk create api --group tools --version v1 --kind DemoResource
```

## Compilation (Maven)

```
mvn clean compile
oc apply -f target/kubernetes/demoresources.tools.opdev.io-v1.yml
mvn quarkus:dev
```