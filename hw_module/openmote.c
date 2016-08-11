#include "openmote.h"
#include "../TinyIPFIX/tinyipfix.h"

#include <stdint.h>
#include "net/ip/uip.h"
#include "dev/button-sensor.h"
#include "dev/sht21.h"
#include "dev/max44009.h"


struct template_rec set_fields(uint8_t, uint16_t, uint16_t, uint32_t, void(*sens_val)(void*));
void read_temp(void*);
void read_humid(void*);
void read_light(void*);
void read_time(void*);
void read_id(void*);
void read_pull(void*);

struct template_rec sky_rec[NUM_ENTRIES];

struct template_rec *init_template() {

	sky_rec[0] = set_fields(E_BIT_TEMP, ELEMENT_ID_TEMP, LEN_TEMP, ENTERPRISE_ID_TEMP, &read_temp);
	sky_rec[1] = set_fields(E_BIT_HUMID, ELEMENT_ID_HUMID, LEN_HUMID, ENTERPRISE_ID_HUMID, &read_humid);
	sky_rec[2] = set_fields(E_BIT_LIGHT, ELEMENT_ID_LIGHT, LEN_LIGHT, ENTERPRISE_ID_LIGHT, &read_light);
	sky_rec[3] = set_fields(E_BIT_TIME, ELEMENT_ID_TIME, LEN_TIME, ENTERPRISE_ID_TIME, &read_time);
	sky_rec[4] = set_fields(E_BIT_ID, ELEMENT_ID_ID, LEN_ID, ENTERPRISE_ID_ID, &read_id);
	sky_rec[5] = set_fields(E_BIT_PULL, ELEMENT_ID_PULL, LEN_PULL, ENTERPRISE_ID_PULL, &read_pull);

	return sky_rec;
}

struct template_rec set_fields(uint8_t e_bit, uint16_t element_id,
		uint16_t field_len, uint32_t enterprise_id, void (*sens_val)(void*)) {

	struct template_rec rec;

	rec.element_id = element_id;
	rec.field_len = field_len;
	rec.enterprise_num = enterprise_id;

	if(e_bit == 1)
		rec.element_id |= 0x8000;

	rec.sens_val = sens_val;

	return rec;
}

void read_temp(void* temp) {

	SENSORS_ACTIVATE(sht21);
	*(uint16_t*)(temp) = sht21.value(SHT21_READ_TEMP);
	SENSORS_DEACTIVATE(sht21);
}

void read_humid(void* humid) {

	SENSORS_ACTIVATE(sht21);
	*(uint16_t*)(humid) = sht21.value(SHT21_READ_RHUM);
	SENSORS_DEACTIVATE(sht21);
}


void read_light (void* light) {

	 SENSORS_ACTIVATE(max44009);
	 max44009.value(MAX44009_READ_LIGHT);
	 SENSORS_DEACTIVATE(max44009);
}

void read_time(void* time) {

	*(uint32_t*)(time) = clock_seconds();
}

//last two octets of the MAC address
void read_id(void* id) {

	*((int16_t*)(id)) = uip_lladdr.addr[6] << 8;
	*((int16_t*)(id)) |= uip_lladdr.addr[7];

}

//0 corresponds to push, 1 corresponds to pull
void read_pull(void *pull) {

	*((uint8_t *)(pull)) = PULL_FLAG;
}
