LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := H264Decode

LOCAL_CFLAGS := -DANDROID_NDK \
                -DDISABLE_IMPORTGL

LOCAL_SRC_FILES := allcodecs.c \
					bitstream.c \
					cabac.c \
					dsputil.c \
					error_resilience.c \
					golomb.c \
					h264.c \
					h264_parser.c \
					h264idct.c \
					h264pred.c \
					imgconvert.c \
					jrevdct.c \
					log.c \
					mem.c \
					mpegvideo.c \
					opt.c \
					parser.c \
					rational.c \
					simple_idct.c \
					svq3.c \
					utils.c \
					vp3dsp.c \
					g72x.cpp \
					g711.cpp \
					g726.cpp \
					g726_16.cpp \
					cn_lois_video_wsplayer_H264decode.cpp

include $(BUILD_SHARED_LIBRARY)  
