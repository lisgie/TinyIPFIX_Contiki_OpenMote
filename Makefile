CONTIKI_PROJECT = node
all: $(CONTIKI_PROJECT)

CFLAGS += -ffunction-sections
LDFLAGS += -Wl,--gc-sections,--undefined=_reset_vector__,--undefined=InterruptVectors,--undefined=_copy_data_init__,--undefined=_clear_bss_init__,--undefined=_end_of_init__

CFLAGS += -DPROJECT_CONF_H=\"project-conf.h\"

WITH_UIP6=1
UIP_CONF_IPV6=1
UIP_CONF_IPV6_RPL=1

CONTIKI = /home/livio/workspace/contiki-2.7



#include $(DEBUG_DIR)/Makefile.debug

NETWORKING_DIR = networking
HW_MODULE_DIR = hw_module
TINYIPFIX_DIR = TinyIPFIX

include $(NETWORKING_DIR)/Makefile.networking
include $(HW_MODULE_DIR)/Makefile.hw_module
include $(TINYIPFIX_DIR)/Makefile.tinyipfix

include $(CONTIKI)/Makefile.include
