LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := ucrop
LOCAL_SRC_FILES := uCrop.cpp

LOCAL_C_INCLUDES := $(LOCAL_PATH)

LOCAL_LDLIBS    := -landroid -llog -lz
LOCAL_STATIC_LIBRARIES := libpng libjpeg9

include $(BUILD_SHARED_LIBRARY)

$(call import-add-path,$(LOCAL_PATH)/ndk_modules)
$(call import-module,libpng)
$(call import-module,libjpeg9)