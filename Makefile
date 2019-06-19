RELEASE_VERSION ?= latest

SUBDIRS=debezium-cdc/connect debezium-cdc/service debezium-cdc/watcher kafka-as-a-storage transform-and-aggregate
DOCKER_TARGETS=docker_build docker_push docker_tag

all: $(SUBDIRS)
build: $(SUBDIRS)
clean: $(SUBDIRS)
$(DOCKER_TARGETS): $(SUBDIRS)

$(SUBDIRS):
	$(MAKE) -C $@ $(MAKECMDGOALS)

.PHONY: all $(SUBDIRS) $(DOCKER_TARGETS)
