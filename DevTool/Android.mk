
LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)


LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := LejiaDevTool
LOCAL_CERTIFICATE := platform

LOCAL_DEX_PREOPT := false

LOCAL_JAVA_LIBRARIES += telephony-common
LOCAL_JAVA_LIBRARIES += mediatek-framework

include $(BUILD_PACKAGE)

