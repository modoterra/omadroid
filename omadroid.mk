# Product contents owned by this repo. Applied after inheriting
# goldfish sdk_phone64_x86_64 so filter-out sees the AOSP lists.

OMADROID_DIR := vendor/modoterra/omadroid

PRODUCT_PACKAGES += \
    OmadroidLauncher

PRODUCT_PACKAGES := $(filter-out \
    Browser2 \
    Calendar \
    Camera2 \
    Contacts \
    DeskClock \
    Gallery2 \
    Music \
    QuickSearchBox \
    messaging \
    PhotoTable \
    ThemePicker \
    EasterEgg \
    Launcher3QuickStep \
    Dialer \
    ,$(PRODUCT_PACKAGES))

PRODUCT_PACKAGE_OVERLAYS += $(OMADROID_DIR)/device/overlay

PRODUCT_COPY_FILES += \
    $(OMADROID_DIR)/device/privapp-permissions-omadroid.xml:$(TARGET_COPY_OUT_SYSTEM_EXT)/etc/permissions/privapp-permissions-omadroid.xml
