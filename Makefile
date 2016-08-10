CONTIKI_PROJECT = node
all: $(CONTIKI_PROJECT)

#CFLAGS += -DPROJECT_CONF_H=\"project-conf.h\"

CONTIKI = /home/livio/workspace/contiki

NETWORKING_DIR = networking
HW_MODULE_DIR = hw_module
TINYIPFIX_DIR = TinyIPFIX

include $(NETWORKING_DIR)/Makefile.networking
include $(HW_MODULE_DIR)/Makefile.hw_module
include $(TINYIPFIX_DIR)/Makefile.tinyipfix

include $(CONTIKI)/Makefile.include
