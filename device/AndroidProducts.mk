# Omadroid lunch targets. Linked into AOSP as
# device/modoterra/omadroid/AndroidProducts.mk.

PRODUCT_MAKEFILES := \
    $(LOCAL_DIR)/omadroid_x86_64.mk

COMMON_LUNCH_CHOICES := \
    omadroid_x86_64-aosp_current-userdebug
