
VERSION ?= 0.0.1
IMAGE_BUILDER?=docker

# CHANNELS define the bundle channels used in the bundle.
# Add a new line here if you would like to change its default config. (E.g CHANNELS = "candidate,fast,stable")
# To re-generate a bundle for other specific channels without changing the standard setup, you can:
# - use the CHANNELS as arg of the bundle target (e.g make bundle CHANNELS=candidate,fast,stable)
# - use environment variables to overwrite this value (e.g export CHANNELS="candidate,fast,stable")
ifneq ($(origin CHANNELS), undefined)
BUNDLE_CHANNELS := --channels=$(CHANNELS)
endif

# DEFAULT_CHANNEL defines the default channel used in the bundle.
# Add a new line here if you would like to change its default config. (E.g DEFAULT_CHANNEL = "stable")
# To re-generate a bundle for any other default channel without changing the default setup, you can:
# - use the DEFAULT_CHANNEL as arg of the bundle target (e.g make bundle DEFAULT_CHANNEL=stable)
# - use environment variables to overwrite this value (e.g export DEFAULT_CHANNEL="stable")
ifneq ($(origin DEFAULT_CHANNEL), undefined)
BUNDLE_DEFAULT_CHANNEL := --default-channel=$(DEFAULT_CHANNEL)
endif
BUNDLE_METADATA_OPTS ?= $(BUNDLE_CHANNELS) $(BUNDLE_DEFAULT_CHANNEL)

# PACKAGE_NAME defines the user specified part of the image name as well as the operator package name
PACKAGE_NAME ?= simple-java-operator

# IMAGE_TAG_BASE defines the docker.io namespace and part of the image name for remote images.
# This variable is used to construct full image tags for bundle and catalog images.
#
# For example, running 'make bundle-build bundle-push catalog-build catalog-push' will build and push both
# opdev.io/simple-java-bundle:$VERSION and opdev.io/simple-java-catalog:$VERSION.
IMAGE_TAG_BASE ?= quay.io/opdev/$(PACKAGE_NAME)

# BUNDLE_IMG defines the image:tag used for the bundle.
# You can use it as an arg. (E.g make bundle-build BUNDLE_IMG=<some-registry>/<project-name-bundle>:<tag>)
BUNDLE_IMG ?= $(IMAGE_TAG_BASE)-bundle:v$(VERSION)

# CATALOG_IMG defines the image:tag used for the bundle.
CATALOG_IMG ?= $(IMAGE_TAG_BASE)-catalog:latest

# Image URL to use all building/pushing image targets
IMG ?= $(IMAGE_TAG_BASE)-controller:v$(VERSION)

all: docker-build

##@ General

# The help target prints out all targets with their descriptions organized
# beneath their categories. The categories are represented by '##@' and the
# target descriptions by '##'. The awk commands is responsible for reading the
# entire set of makefiles included in this invocation, looking for lines of the
# file as xyz: ## something, and then pretty-format the target and help. Then,
# if there's a line with ##@ something, that gets pretty-printed as a category.
# More info on the usage of ANSI control characters for terminal formatting:
# https://en.wikipedia.org/wiki/ANSI_escape_code#SGR_parameters
# More info on the awk command:
# http://linuxcommand.org/lc3_adv_awk.php

help: ## Display this help.
	@awk 'BEGIN {FS = ":.*##"; printf "\nUsage:\n  make \033[36m<target>\033[0m\n"} /^[a-zA-Z_0-9-]+:.*?##/ { printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2 } /^##@/ { printf "\n\033[1m%s\033[0m\n", substr($$0, 5) } ' $(MAKEFILE_LIST)

##@ Build

docker-build: ## Build docker image with the manager.
	mvn clean package -Pnative -Dquarkus.container-image.build=true -Dquarkus.container-image.image=${IMG} -Dquarkus.native.container-runtime=${IMAGE_BUILDER}

docker-push: ## Push docker image with the manager.
	mvn clean package -Pnative -Dquarkus.container-image.push=true -Dquarkus.container-image.image=${IMG} -Dquarkus.native.container-runtime=${IMAGE_BUILDER}

##@ Deployment

install: ## Install CRDs into the K8s cluster specified in ~/.kube/config.
	@$(foreach file, $(wildcard target/kubernetes/*-v1.yml), kubectl apply -f $(file);)

uninstall: ## Uninstall CRDs from the K8s cluster specified in ~/.kube/config.
	@$(foreach file, $(wildcard target/kubernetes/*-v1.yml), kubectl delete -f $(file);)

deploy: ## Deploy controller to the K8s cluster specified in ~/.kube/config.
	kubectl apply -f target/kubernetes/kubernetes.yml

undeploy: ## Undeploy controller from the K8s cluster specified in ~/.kube/config.
	kubectl delete -f target/kubernetes/kubernetes.yml

deploy-hack:
	export MYNS=$(kubectl config view --minify -o 'jsonpath={..namespace}')
	sed 's/PLACEHOLDER/${MYNS}/g' hack/kubernetes.yml | kubectl apply -f -

undeploy-hack:
	export MYNS=$(kubectl config view --minify -o 'jsonpath={..namespace}')
	sed 's/PLACEHOLDER/${MYNS}/g' hack/kubernetes.yml | kubectl delete -f -

##@Bundle
.PHONY: bundle
bundle:  ## Generate bundle manifests and metadata, then validate generated files.
## marker
	cat target/kubernetes/demoresources.tools.opdev.io-v1.yml target/kubernetes/kubernetes.yml | operator-sdk generate bundle -q --overwrite --version $(VERSION) $(BUNDLE_METADATA_OPTS)
	operator-sdk bundle validate ./bundle
	
.PHONY: bundle-build
bundle-build: ## Build the bundle image.
	${IMAGE_BUILDER} build -t $(BUNDLE_IMG) -f target/bundle/$(PACKAGE_NAME)/bundle.Dockerfile target/bundle/$(PACKAGE_NAME)

.PHONY: bundle-push
bundle-push: ## Push the bundle image.
	${IMAGE_BUILDER} push $(BUNDLE_IMG)

.PHONY: catalog-build
catalog-build: opm ## Build the catalog image.
	$(OPM) index add \
        --bundles $(BUNDLE_IMG) \
        --tag $(CATALOG_IMG) \
        --build-tool ${IMAGE_BUILDER}

.PHONY: catalog-push
catalog-push: ## Push the catalog image.
	${IMAGE_BUILDER} push $(CATALOG_IMG)

.PHONY: opm
OPM = ./bin/opm
opm: ## Download opm locally if necessary.
ifeq (,$(wildcard $(OPM)))
ifeq (,$(shell which opm 2>/dev/null))
	@{ \
	set -e ;\
	mkdir -p $(dir $(OPM)) ;\
	OS=$(shell go env GOOS) && ARCH=$(shell go env GOARCH) && \
	curl -sSLo $(OPM) https://github.com/operator-framework/operator-registry/releases/download/v1.23.0/$${OS}-$${ARCH}-opm ;\
	chmod +x $(OPM) ;\
	}
else
OPM = $(shell which opm)
endif
endif
