# Omadroid phone product on the generic goldfish/ranchu emulator.
# Inherit the AOSP sdk_phone64_x86_64 stack, then overlay identity
# and packages from this repo (vendor/modoterra/omadroid).
#
# https://source.android.com/docs/setup/create/new-device
# https://android.googlesource.com/device/generic/goldfish/+/refs/heads/main/64bitonly/product/sdk_phone64_x86_64.mk

$(call inherit-product, device/generic/goldfish/64bitonly/product/sdk_phone64_x86_64.mk)

# Goldfish defaults this group to 1800MiB. Our system+product+vendor
# images exceed that when packaging super.img.
BOARD_EMULATOR_DYNAMIC_PARTITIONS_SIZE := $(shell expr 2300 \* 1048576 )
BOARD_SUPER_PARTITION_SIZE := $(shell expr $(BOARD_EMULATOR_DYNAMIC_PARTITIONS_SIZE) + 8388608 )

PRODUCT_NAME := omadroid_x86_64
PRODUCT_BRAND := omadroid
PRODUCT_MODEL := Omadroid
PRODUCT_MANUFACTURER := Modoterra

ifeq (omadroid_x86_64,$(TARGET_PRODUCT))
PRODUCT_ENFORCE_ARTIFACT_PATH_REQUIREMENTS := relaxed
endif

$(call inherit-product, vendor/modoterra/omadroid/omadroid.mk)
