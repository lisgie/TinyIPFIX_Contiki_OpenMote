/*
 * openmote.h
 *
 *  Created on: Aug 10, 2016
 *      Author: livio
 */

#ifndef OPENMOTE_H_
#define OPENMOTE_H_

#ifndef SKY_H_
#define SKY_H_

#define NUM_ENTRIES 8

//SHT11 Temperature Sensor
#define ELEMENT_ID_TEMP 0x80A0
#define ENTERPRISE_ID_TEMP 0xF0AA00AA
#define E_BIT_TEMP 1
#define LEN_TEMP 2

//SHT11 Humidity Sensor
#define ELEMENT_ID_HUMID 0x80A1
#define ENTERPRISE_ID_HUMID 0xF0AA00AA
#define E_BIT_HUMID 1
#define LEN_HUMID 2

//S1087 Light Sensor
#define ELEMENT_ID_LIGHT_PHOTO 0x80A2
#define ENTERPRISE_ID_LIGHT_PHOTO 0xF0AA00AA
#define E_BIT_LIGHT_PHOTO 1
#define LEN_LIGHT_PHOTO 2

//S1087-01 Light Sensor
#define ELEMENT_ID_LIGHT_TOTAL 0x80A3
#define ENTERPRISE_ID_LIGHT_TOTAL 0xF0AA00AA
#define E_BIT_LIGHT_TOTAL 1
#define LEN_LIGHT_TOTAL 2

//2xAA Batteries
#define ELEMENT_ID_BATTERY 0x80A4
#define ENTERPRISE_ID_BATTERY 0XF0AA00AA
#define E_BIT_BATTERY 1
#define LEN_BATTERY 2

//Internal clock
#define ELEMENT_ID_TIME 0x80A5
#define ENTERPRISE_ID_TIME 0xF0AA00AA
#define E_BIT_TIME 1
#define LEN_TIME 4

//Node ID, last two octets of IPv6 address - redundant
#define ELEMENT_ID_ID 0x80A6
#define ENTERPRISE_ID_ID 0xF0AA00AA
#define E_BIT_ID 1
#define LEN_ID 2

//Possible future extension, pull flag
#define ELEMENT_ID_PULL 0x80A7
#define ENTERPRISE_ID_PULL 0xF0AA00AA
#define E_BIT_PULL 1
#define LEN_PULL 1

//this is needed as changeable header option for the data sets
#define EXTENDED_HEADER_SEQ 0
#define EXTENDED_HEADER_SET_ID 0

//needs to be globally accessible
struct template_rec *init_template();

#endif /* SKY_H_ */





#endif /* TINYIPFIX_CONTIKI_OPENMOTE_HW_MODULE_OPENMOTE_H_ */
