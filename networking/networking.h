#include <stdint.h>

#ifndef NETWORKING_H_
#define NETWORKING_H_

#ifndef BORDER_ROUTER_PORT
#define BORDER_ROUTER_PORT 40001
#endif

#ifndef NODE_PORT
#define NODE_PORT 40001
#endif

//Setting up a connection, data is sent over this connection
int conn_set_up();

//Sending data with specified length
void send_msg(uint8_t *data, uint16_t len);

#endif /* NETWORKING_H_ */
