# Product contents owned by this repo. Applied after inheriting
# goldfish sdk_phone64_x86_64 so filter-out sees the AOSP lists.

OMADROID_DIR := vendor/modoterra/omadroid

PRODUCT_PACKAGES += \
    OmadroidLauncher \
    OmadroidShell \
    OmadroidContacts \
    OmadroidGallery

# Do not filter-out PRODUCT_PACKAGES here. inherit-product stores
# goldfish entries as inherit tags; filter-out never sees Gallery2.
# OmadroidLauncher.overrides is what actually omits those modules.

PRODUCT_PACKAGE_OVERLAYS += $(OMADROID_DIR)/device/overlay

PRODUCT_COPY_FILES += \
    $(OMADROID_DIR)/device/privapp-permissions-omadroid.xml:$(TARGET_COPY_OUT_SYSTEM_EXT)/etc/permissions/privapp-permissions-omadroid.xml
