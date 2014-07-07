#include "cn_lois_video_wsplayer_H264decode.h"
#include <android/log.h>
#define LOG_TAG "H264Decode"

#undef LOG
#define LOGD(a )  __android_log_write(ANDROID_LOG_DEBUG,LOG_TAG,a)

#include "g726.h"

extern "C"{

#include "dsputil.h"
#include "h264.h"


#define PI 3.14159265358979323846

#ifdef HAVE_AV_CONFIG_H
#undef HAVE_AV_CONFIG_H
#endif

#include "avcodec.h"
#include "define.h"

#define INBUF_SIZE 4096

extern AVCodec h264_decoder;

}


static bool colortab_init = false;
static int colortab[4*256*sizeof(int)];
static int *u_b_tab;
static int *u_g_tab;
static int *v_g_tab;
static int *v_r_tab;

static unsigned int rgb_2_pix[3*768*sizeof(unsigned int)];
static unsigned int *r_2_pix;
static unsigned int *g_2_pix;
static unsigned int *b_2_pix;

static void CreateYUVTab()
{
	int i;
	int u, v;

	if (colortab_init) return;
	u_b_tab = &colortab[0*256];
	u_g_tab = &colortab[1*256];
	v_g_tab = &colortab[2*256];
	v_r_tab = &colortab[3*256];

	for (i=0; i<256; i++)
	{
		u = v = (i-128);

		u_b_tab[i] = (int) ( 1.772 * u);
		u_g_tab[i] = (int) ( 0.34414 * u);
		v_g_tab[i] = (int) ( 0.71414 * v); 
		v_r_tab[i] = (int) ( 1.402 * v);
	}

	r_2_pix = &rgb_2_pix[0*768];
	g_2_pix = &rgb_2_pix[1*768];
	b_2_pix = &rgb_2_pix[2*768];

	for(i=0; i<256; i++)
	{
		r_2_pix[i] = 0;
		g_2_pix[i] = 0;
		b_2_pix[i] = 0;
	}

	for(i=0; i<256; i++)
	{
		r_2_pix[i+256] = i << 16;
		g_2_pix[i+256] = i << 8;
		b_2_pix[i+256] = i;
	}

	for(i=0; i<256; i++)
	{
		r_2_pix[i+512] = 0xFF0000;
		g_2_pix[i+512] = 0xFF00;
		b_2_pix[i+512] = 0xFF;
	}

	r_2_pix += 256;
	g_2_pix += 256;
	b_2_pix += 256;

	colortab_init = true;
}

//#define SET_POINT(row, column, color) pdst[(row) * dst_ystride + (column)] = (color)
#define SET_POINT(row, column, r, g, b) doff = pdst + ((row) * dst_ystride + (column)) * 3; \
		doff[0] = b_2_pix[b]; \
		doff[1] = b_2_pix[g]; \
		doff[2] = b_2_pix[r]; 

void yuv2bppRgb565(unsigned char *pdst, unsigned char *y, unsigned char *u, unsigned char *v, int width, int height, int src_ystride, int src_uvstride, int dst_ystride)
{
	if (!colortab_init) return;
	int col, row;
	int r, g, b, rgb;

	int yy, ub, ug, vg, vr;

	const int width2 = width/2;
	const int height2 = height/2;

	unsigned char* yoff;
	unsigned char* uoff;
	unsigned char* voff;
	
	unsigned char* doff;

	for(row=0; row < height2; row++)
	{
		yoff = y + row * 2 * src_ystride;
		uoff = u + row * src_uvstride;
		voff = v + row * src_uvstride;

		for(col=0; col < width2; col++)
		{
			yy  = *(yoff+(col<<1));
			ub = u_b_tab[*(uoff+col)];
			ug = u_g_tab[*(uoff+col)];
			vg = v_g_tab[*(voff+col)];
			vr = v_r_tab[*(voff+col)];

			b = yy + ub;
			g = yy - ug - vg;
			r = yy + vr;

			//rgb = r_2_pix[r] + g_2_pix[g] + b_2_pix[b];
			SET_POINT(2 * row, 2 * col, r, g, b);

			yy = *(yoff+(col<<1)+1);
			b = yy + ub;
			g = yy - ug - vg;
			r = yy + vr;

			//rgb = r_2_pix[r] + g_2_pix[g] + b_2_pix[b];
			SET_POINT(2 * row, 2 * col + 1, r, g, b);

			yy = *(yoff+(col<<1)+src_ystride);
			b = yy + ub;
			g = yy - ug - vg;
			r = yy + vr;

			//rgb = r_2_pix[r] + g_2_pix[g] + b_2_pix[b];
			SET_POINT(2 * row + 1, 2 * col, r, g, b);

			yy = *(yoff+(col<<1)+src_ystride+1);
			b = yy + ub;
			g = yy - ug - vg;
			r = yy + vr;

			//rgb = r_2_pix[r] + g_2_pix[g] + b_2_pix[b];
			SET_POINT(2 * row + 1, 2 * col + 1, r, g, b);
		}
	}
}

typedef unsigned long       DWORD;
typedef int                 BOOL;
typedef unsigned short      WORD;
typedef char CHAR;
typedef short SHORT;
typedef long LONG;
typedef struct tagBITMAPFILEHEADER {
        WORD    bfType;
        WORD   bfSize;
        WORD   bfSize_hi;
        WORD    bfReserved1;
        WORD    bfReserved2;
        WORD   bfOffBits;
        WORD    bfReserved3;
} BITMAPFILEHEADER;

typedef struct tagBITMAPINFOHEADER{
        DWORD      biSize;
        LONG       biWidth;
        LONG       biHeight;
        WORD       biPlanes;
        WORD       biBitCount;
        DWORD      biCompression;
        DWORD      biSizeImage;
        LONG       biXPelsPerMeter;
        LONG       biYPelsPerMeter;
        DWORD      biClrUsed;
        DWORD      biClrImportant;
} BITMAPINFOHEADER;
#define BMP_HEAD_LENGTH (14 + sizeof(BITMAPINFOHEADER))
void AssembleBmp( unsigned char *p, int bmp_width, int bmp_height)
{
	BITMAPFILEHEADER fileHeader;
	BITMAPINFOHEADER infoHeader; 

	memset(&fileHeader, 0, sizeof(fileHeader));
	fileHeader.bfType = 0x4d42;  //bmp  
	fileHeader.bfOffBits = BMP_HEAD_LENGTH;
	DWORD size = fileHeader.bfOffBits + ((bmp_width*bmp_height)*3); 
	fileHeader.bfSize = (WORD)(size & 0xFFFF);
	fileHeader.bfSize_hi = (WORD)((size >> 16) & 0xFFFF);

	memset(&infoHeader, 0, sizeof(infoHeader));
	infoHeader.biSize = sizeof(infoHeader);
	infoHeader.biWidth = bmp_width;
	infoHeader.biHeight = -bmp_height;
	infoHeader.biPlanes = 1;
	infoHeader.biBitCount = 24;

	memcpy(p, &fileHeader, 14);
	memcpy(p + 14, &infoHeader, sizeof(infoHeader));
}

class H264Decode
{
	AVCodecContext *c;
	AVFrame *picture;
	g726_state state_ptr;
public:
	int width;
	int height;

	H264Decode(void)
	{
		AVCodec *codec = &h264_decoder;

		/* find the mpeg1 video decoder */
		avcodec_init();
		c= avcodec_alloc_context();
		picture= avcodec_alloc_frame();
	//	 dsputil_init(&dsp, c);

		if(codec->capabilities&CODEC_CAP_TRUNCATED)
			c->flags|= CODEC_FLAG_TRUNCATED; /* we do not send complete frames */

		c->flags |= CODEC_FLAG_LOW_DELAY;
		c->flags2 |= CODEC_FLAG2_BPYRAMID | CODEC_FLAG2_MIXED_REFS | CODEC_FLAG2_FASTPSKIP | CODEC_FLAG2_FAST;

		/* For some codecs, such as msmpeg4 and mpeg4, width and height
		   MUST be initialized there because this information is not
		   available in the bitstream. */

		/* open it */
		  
		  
		if (avcodec_open(c, codec) < 0) {
			//throw gcnew System::Exception("could not open codec");
			return;
		}
		H264Context *h = (H264Context*)c->priv_data;
		MpegEncContext *s = &h->s;
		s->dsp.idct_permutation_type =1;
		dsputil_init(&s->dsp, c);

		g726_init_state(&state_ptr);
	}

	int DecodeOneFrame(unsigned char *buf, int buf_size, unsigned char * bpp, unsigned int *bpp_size)
	{
		int len, got_picture;
		int outlen = *bpp_size;
		*bpp_size = 0;
		len = avcodec_decode_video(c, picture, &got_picture, buf, buf_size);
		if (len < 0)
		{
			return len;
		}
		else if (len == 0)
		{
			int got_picture2;
			len = avcodec_decode_video(c, picture, &got_picture2, buf, buf_size);
			got_picture = got_picture || got_picture2;
		}

		if (got_picture)
		{
			width = c->width;
			height = c->height;
			if (outlen >= BMP_HEAD_LENGTH + width * height * 3)
			{
				AssembleBmp(bpp, width, height);
				yuv2bppRgb565((bpp + BMP_HEAD_LENGTH), picture->data[0], picture->data[1], picture->data[2], 
					width, height, picture->linesize[0], picture->linesize[1], c->width);
				*bpp_size = BMP_HEAD_LENGTH + width * height * 3;
			}
		}

		return len;
	}

	int DecodeG726Audio(unsigned char *buf, int buf_size, unsigned char * out, unsigned int *out_size)
	{
		int outlen = *out_size;
		*out_size = 0;
		if (outlen >= buf_size * 8)
		{
			*out_size = buf_size * 8;
			g726_Decode((char*)buf, buf_size, out, &state_ptr);
			return buf_size;
		}
		else 
		{
			return 0;
		}
	}

	~H264Decode()
	{
		avcodec_close(c);
		av_free(c);
		av_free(picture);
	}

};

JNIEXPORT jint JNICALL Java_cn_lois_video_wsplayer_H264decode_DecodeOneFrame   
  (JNIEnv * env, jclass obj, jlong decode, jobject pInBuffer, jobject pOutBuffer) {   
  
    H264Decode *pDecode = (H264Decode *)decode;   
    unsigned char *In = NULL;unsigned char *Out = NULL;   
    unsigned int InPosition = 0;unsigned int InRemaining = 0;unsigned int InSize = 0;   
    unsigned int OutSize = 0;   
    jint DecodeSize = -1;   
  
    jbyte *InJbyte = 0;   
    jbyte *OutJbyte = 0;   
  
    jbyteArray InByteArrary = 0;   
    jbyteArray OutByteArrary = 0;   
  
    //获取Input/Out ByteBuffer相关属性   
    {   
        //Input   
        {
            jclass ByteBufferClass = env->GetObjectClass(pInBuffer);   
            jmethodID PositionMethodId = env->GetMethodID(ByteBufferClass,"position","()I");   
            jmethodID ArraryMethodId = env->GetMethodID(ByteBufferClass,"array","()[B");   
               
            InPosition = env->CallIntMethod(pInBuffer,PositionMethodId);   
            //InRemaining = env->CallIntMethod(pInBuffer,RemainingMethodId);   
            //InSize = InPosition + InRemaining;   
               
            InByteArrary = (jbyteArray)env->CallObjectMethod(pInBuffer,ArraryMethodId);   
           
            InJbyte = env->GetByteArrayElements(InByteArrary,0);   
               
            //In = (unsigned char*)InJbyte + InPosition; 
            In = (unsigned char*)InJbyte;
        }   
  
        //Output   
        {
            jclass ByteBufferClass = env->GetObjectClass(pOutBuffer);   
            jmethodID ArraryMethodId = env->GetMethodID(ByteBufferClass,"array","()[B");   
            jmethodID RemainingMethodId = env->GetMethodID(ByteBufferClass,"remaining","()I");   
            jmethodID PositionMethodId = env->GetMethodID(ByteBufferClass,"position","()I");   
  
            OutSize = env->CallIntMethod(pOutBuffer,RemainingMethodId) + env->CallIntMethod(pOutBuffer,PositionMethodId);  
            OutByteArrary = (jbyteArray)env->CallObjectMethod(pOutBuffer,ArraryMethodId);   
            OutJbyte = env->GetByteArrayElements(OutByteArrary,0);   
  
            Out = (unsigned char*)OutJbyte;   
        }   
    }   
    //解码   
    DecodeSize = pDecode->DecodeOneFrame(In,InPosition,Out,&OutSize);   
  
    //设置Input/Output ByteBuffer相关属性   
    {   
        //Input   
        {   
            //jclass ByteBufferClass = env->GetObjectClass(pInBuffer);   
            //jmethodID SetPositionMethodId = env->GetMethodID(ByteBufferClass,"position","(I)Ljava/nio/Buffer;");   
               
            //设置输入缓冲区偏移   
            //env->CallObjectMethod(pInBuffer,SetPositionMethodId,InPosition + DecodeSize);   
        }   
  
        //Output   
        {
            jclass ByteBufferClass = env->GetObjectClass(pOutBuffer);   
            jmethodID SetPositionMethodId = env->GetMethodID(ByteBufferClass,"position","(I)Ljava/nio/Buffer;");   
  
           //设置输出缓冲区偏移   
            env->CallObjectMethod(pOutBuffer,SetPositionMethodId,OutSize);   
       }   
    }   

    //清理   
    env->ReleaseByteArrayElements(InByteArrary,InJbyte,0);   
    env->ReleaseByteArrayElements(OutByteArrary,OutJbyte,0);   
  
    return DecodeSize;   
}   
/*
 * Class:     cn_lois_video_wsplayer_H264decode
 * Method:    DecodeG726Audio
 * Signature: (JLjava/nio/ByteBuffer;Ljava/nio/ByteBuffer;)I
 */
JNIEXPORT jint JNICALL Java_cn_lois_video_wsplayer_H264decode_DecodeG726Audio
(JNIEnv * env, jclass obj, jlong decode, jobject pInBuffer, jobject pOutBuffer) {

    H264Decode *pDecode = (H264Decode *)decode;   
    unsigned char *In = NULL;unsigned char *Out = NULL;   
    unsigned int InPosition = 0;unsigned int InRemaining = 0;unsigned int InSize = 0;   
    unsigned int OutSize = 0;   
    jint DecodeSize = -1;   
  
    jbyte *InJbyte = 0;   
    jbyte *OutJbyte = 0;   
  
    jbyteArray InByteArrary = 0;   
    jbyteArray OutByteArrary = 0;   
  
    //获取Input/Out ByteBuffer相关属性   
    {   
        //Input   
        {
            jclass ByteBufferClass = env->GetObjectClass(pInBuffer);   
            jmethodID PositionMethodId = env->GetMethodID(ByteBufferClass,"position","()I");   
            jmethodID ArraryMethodId = env->GetMethodID(ByteBufferClass,"array","()[B");   
               
            InPosition = env->CallIntMethod(pInBuffer,PositionMethodId);   
            //InRemaining = env->CallIntMethod(pInBuffer,RemainingMethodId);   
            //InSize = InPosition + InRemaining;   
               
            InByteArrary = (jbyteArray)env->CallObjectMethod(pInBuffer,ArraryMethodId);   
           
            InJbyte = env->GetByteArrayElements(InByteArrary,0);   
               
            //In = (unsigned char*)InJbyte + InPosition; 
            In = (unsigned char*)InJbyte;
        }   
  
        //Output   
        {
            jclass ByteBufferClass = env->GetObjectClass(pOutBuffer);   
            jmethodID ArraryMethodId = env->GetMethodID(ByteBufferClass,"array","()[B");   
            jmethodID RemainingMethodId = env->GetMethodID(ByteBufferClass,"remaining","()I");   
            jmethodID PositionMethodId = env->GetMethodID(ByteBufferClass,"position","()I");   
  
            OutSize = env->CallIntMethod(pOutBuffer,RemainingMethodId) + env->CallIntMethod(pOutBuffer,PositionMethodId);  
            OutByteArrary = (jbyteArray)env->CallObjectMethod(pOutBuffer,ArraryMethodId);   
            OutJbyte = env->GetByteArrayElements(OutByteArrary,0);   
  
            Out = (unsigned char*)OutJbyte;   
        }   
    }   
    //解码   
    DecodeSize = pDecode->DecodeG726Audio(In + 4,InPosition - 4,Out,&OutSize);   
  
    //设置Input/Output ByteBuffer相关属性   
    {   
        //Input   
        {   
            //jclass ByteBufferClass = env->GetObjectClass(pInBuffer);   
            //jmethodID SetPositionMethodId = env->GetMethodID(ByteBufferClass,"position","(I)Ljava/nio/Buffer;");   
               
            //设置输入缓冲区偏移   
            //env->CallObjectMethod(pInBuffer,SetPositionMethodId,InPosition + DecodeSize);   
        }   
  
        //Output   
        {
            jclass ByteBufferClass = env->GetObjectClass(pOutBuffer);   
            jmethodID SetPositionMethodId = env->GetMethodID(ByteBufferClass,"position","(I)Ljava/nio/Buffer;");   
  
           //设置输出缓冲区偏移   
            env->CallObjectMethod(pOutBuffer,SetPositionMethodId,OutSize);   
       }   
    }   

    //清理   
    env->ReleaseByteArrayElements(InByteArrary,InJbyte,0);   
    env->ReleaseByteArrayElements(OutByteArrary,OutJbyte,0);   
  
    return DecodeSize;   
}
 
JNIEXPORT jlong JNICALL Java_cn_lois_video_wsplayer_H264decode_Initialize   
  (JNIEnv * env, jclass obj) {   
  
	CreateYUVTab();

    H264Decode *pDecode = new H264Decode(); 
    return (jlong)pDecode;   
}   
  
JNIEXPORT void JNICALL Java_cn_lois_video_wsplayer_H264decode_Destroy   
  (JNIEnv * env, jclass obj, jlong decode) {   
  
    H264Decode *pDecode = (H264Decode *)decode;   
    if (pDecode)   
    {   
        delete pDecode;   
        pDecode = NULL;   
    }   
}  
