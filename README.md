# Simple Java Operator built with Java Operator SDK

## Prerequisites

```
brew install operator-sdk mvn
```

## Additional context

- https://javaoperatorsdk.io/docs/features

## Generate code

```
operator-sdk init --plugins quarkus --domain opdev.io --project-name simple-java
operator-sdk create api --group tools --version v1 --kind DemoResource
```

## Compilation (Maven)

```
mvn clean compile
mvn quarkus:dev
```

## Testing (while Quarkus is running or controller deployed)

```
oc apply -f src/test/resources/cr-test-demo-resource.yaml
```
