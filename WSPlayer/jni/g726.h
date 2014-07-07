#pragma once
#include "g72x.h"
void g726_Encode(unsigned char *speech,char *bitstream);
void g726_Decode(char *bitstream, int bitLen, unsigned char *speech, g726_state *state_ptr);