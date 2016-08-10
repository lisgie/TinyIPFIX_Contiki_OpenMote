#include "tinyipfix.h"
#include <string.h>

//here comes the choosing of the sensor
#include "../hw_module/openmote.h"

//maybe not needed
#ifndef EXTENDED_HEADER_SEQ
	#define EXTENDED_HEADER_SEQ 0
#endif
#ifndef EXTENDED_HEADER_SET_ID
	#define EXTENDED_HEADER_SET_ID 0
#endif
#if EXTENDED_HEADER_SET_ID == 1
	#if EXTENDED_HEADER_SEQ == 1
		#define MSG_HEADER_SIZE 5
	#else
		#define MSG_HEADER_SIZE 4
	#endif
#else
	#if EXTENDED_HEADER_SEQ == 1
		#define MSG_HEADER_SIZE 4
	#else
		#define MSG_HEADER_SIZE 3
	#endif
#endif

#define SWITCH_ENDIAN_16(n) (uint16_t)((((uint16_t) (n)) << 8) | (((uint16_t) (n)) >> 8))
#define SWITCH_ENDIAN_32(n) (((uint32_t)SWITCH_ENDIAN_16(n) << 16) | SWITCH_ENDIAN_16((uint32_t)(n) >> 16))
#define SWITCH_ENDIAN_64(n) (((uint64_t)SWITCH_ENDIAN_32(n) << 32) | SWITCH_ENDIAN_32((uint64_t)(n) >> 32))

void build_msg_header(uint8_t *buf, uint16_t set_id, uint16_t length, uint16_t seq_num);

void build_template(void);

void build_data_payload(void);
void build_data_header(void);

uint8_t is_initialized = 0;

uint8_t template_buf[MAX_MSG_SIZE];

uint8_t data_buf[MAX_MSG_SIZE];

uint16_t data_seq_num = 0;

struct template_rec *sensor;
struct buf_info *instance;


void initialize(void) {

	sensor = init_template();

	//fixed at compile time: template/data header and template payload, only do it once
	build_template(); //header and payload
	build_data_header();

	is_initialized = 1;

}

void build_msg_header(uint8_t* buf, uint16_t set_id, uint16_t length, uint16_t seq_num) {

	//basic checks
	if(set_id > MAX_SET_ID || seq_num > MAX_SEQ_NUM || length > MAX_MSG_SIZE)
		return;

	//zeroing out, can't rely on zero in mem
	buf[0] = 0;

	if(set_id == TEMPLATE_SET_ID) {
		set_id = 1;
	} else if(set_id == DATA_SET_ID) {
		set_id = 2;
	}

	if(set_id < 16) {
		buf[0] |= (set_id << 2);
	}

	length += MSG_HEADER_SIZE;
	buf[0] |= (uint8_t)(length >> 8);
	buf[1] = (uint8_t)(length);

	buf[2] = (uint8_t)(seq_num);

	if(MSG_HEADER_SIZE == 3) {

	} else if(EXTENDED_HEADER_SET_ID == 0 && EXTENDED_HEADER_SEQ == 1) {

		buf[0] |= 0x40;
		buf[2] = (uint8_t)(seq_num >> 8);
		buf[3] = (uint8_t)(seq_num);
	} else if(EXTENDED_HEADER_SET_ID == 1 && EXTENDED_HEADER_SEQ == 0) {

		buf[0] |= 0x80;
		buf[0] |= ((set_id >> 8) << 2);
		buf[3] = (uint8_t)(set_id);

	} else if(MSG_HEADER_SIZE == 5) {

		buf[0] |= 0xc0;

		buf[0] |= ((set_id >> 8) << 2);
		buf[4] = (uint8_t)(set_id);

		buf[2] = (uint8_t)(seq_num >> 8);
		buf[3] = (uint8_t)(seq_num);
	}
}

 void build_template(void) {

	uint8_t i;
	uint16_t element_id, field_len;
	uint32_t enterprise_num;

	uint16_t ref_set_id = DATA_SET_ID;
	uint16_t field_count = NUM_ENTRIES;

	uint16_t template_size = 0, template_tmp_len = MSG_HEADER_SIZE;

	//calculate length first to be able to build the message header
	for(i = 0; i < NUM_ENTRIES; i++) {

		if( ((sensor[i].element_id) | 0x8000) == sensor[i].element_id) {
			template_size += 4;
		}
	}

	//payload
	template_size += 4*NUM_ENTRIES;
	//set header
	template_size += SET_HEADER_SIZE;

	build_msg_header(template_buf, TEMPLATE_SET_ID, template_size, 0xFFFF);

	ref_set_id = SWITCH_ENDIAN_16(ref_set_id);
	field_count = SWITCH_ENDIAN_16(field_count);

	memcpy(&template_buf[template_tmp_len], &ref_set_id, sizeof(ref_set_id));
	template_tmp_len += sizeof(ref_set_id);
	memcpy(&template_buf[template_tmp_len], &field_count, sizeof(field_count));
	template_tmp_len += sizeof(ref_set_id);

	//get template length by counting fields with respect to set enterprise bit
	for(i = 0; i < NUM_ENTRIES; i++) {

		element_id = SWITCH_ENDIAN_16(sensor[i].element_id);
		memcpy(&template_buf[template_tmp_len], &element_id, sizeof(element_id));
		template_tmp_len += sizeof(element_id);

		field_len = SWITCH_ENDIAN_16(sensor[i].field_len);
		memcpy(&template_buf[template_tmp_len], &field_len, sizeof(field_len));
		template_tmp_len += sizeof(field_len);

		//check if E_BIT is set
		if( ((sensor[i].element_id) | 0x8000) == sensor[i].element_id) {

			enterprise_num = SWITCH_ENDIAN_32(sensor[i].enterprise_num);
			memcpy(&template_buf[template_tmp_len], &enterprise_num, sizeof(enterprise_num));
			template_tmp_len += sizeof(enterprise_num);
		}
	}
}

void build_data_header(void) {

	uint8_t i;
	uint16_t data_size = 0;

	for(i = 0; i < NUM_ENTRIES; i++) {

		data_size += sensor[i].field_len;
	}

	build_msg_header(data_buf, DATA_SET_ID, data_size, data_seq_num);
}

void build_data_payload(void) {

	uint8_t i, val8;
	uint16_t val16;
	uint32_t val32;
	uint64_t val64;

	uint16_t data_tmp_len = MSG_HEADER_SIZE;

	//adjust the sequence number without rebuilding the header!
	data_seq_num++;
	if(EXTENDED_HEADER_SEQ == 1) {
		if(data_seq_num > MAX_SEQ_LARGE)
			data_seq_num = 0;

		data_buf[2] = (uint8_t)(data_seq_num >> 8);
		data_buf[3] = (uint8_t)(data_seq_num);
	} else {
		if(data_seq_num > MAX_SEQ_SMALL) {
			data_seq_num = 0;
		}
		data_buf[2] = (uint8_t)(data_seq_num);
	}
	//

	for(i = 0; i < NUM_ENTRIES; i++) {

		switch(sensor[i].field_len) {

			case 1:
				sensor[i].sens_val(&val8);
				memcpy(&data_buf[data_tmp_len], &val8, sizeof(uint8_t));
				data_tmp_len += sizeof(uint8_t);
				break;
			case 2:
				sensor[i].sens_val(&val16);
				val16 = SWITCH_ENDIAN_16(val16);
				memcpy(&data_buf[data_tmp_len], &val16, sizeof(uint16_t));
				data_tmp_len += sizeof(uint16_t);
				break;
			case 4:
				sensor[i].sens_val(&val32);
				val32 = SWITCH_ENDIAN_32(val32);
				memcpy(&data_buf[data_tmp_len], &val32, sizeof(uint32_t));
				data_tmp_len += sizeof(uint32_t);
				break;
			case 8:
				sensor[i].sens_val(&val64);
				val64 = SWITCH_ENDIAN_64(val64);
				memcpy(&data_buf[data_tmp_len], &val64, sizeof(uint64_t));
				data_tmp_len += sizeof(uint64_t);
				break;
		}
	}
}

uint8_t *get_template(void) {

	if(!is_initialized)
		return NULL;

	return template_buf;
}

uint8_t *get_data(void) {

	if(!is_initialized)
		return NULL;

	build_data_payload();

	return data_buf;
}


