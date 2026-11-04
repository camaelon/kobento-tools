# kobento-tools — the headless C++ tools, built on the engine from kobento-libs.
#
#   rc2json          binary → JSON
#   rc2image         headless render → PNG (the MCP render server runs it)
#   rc2layout        the computed layout tree
#   d3scene          the 3D scene harness (conformance/3d-parity.sh)
#   rc2image-native  the same render through the Core Graphics backend (macOS, needs kobento-ios)
#
#   cmake -S cpp -B cpp/build && cmake --build cpp/build
#
# The engine is a sibling checkout: ../kobento-libs next to this repository, or
# -DKOBENTO_LIBS_DIR=/path/to/kobento-libs.

cmake_minimum_required(VERSION 3.20)
project(KobentoTools LANGUAGES C CXX)

if(NOT CMAKE_BUILD_TYPE AND NOT CMAKE_CONFIGURATION_TYPES)
  set(CMAKE_BUILD_TYPE Release CACHE STRING "Build type" FORCE)
endif()

set(KOBENTO_LIBS_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../kobento-libs" CACHE PATH "kobento-libs checkout")
set(KOBENTO_IOS_DIR  "${CMAKE_CURRENT_SOURCE_DIR}/../../kobento-ios"  CACHE PATH "kobento-ios checkout (for rc2image-native)")
if(NOT EXISTS "${KOBENTO_LIBS_DIR}/CMakeLists.txt")
    message(FATAL_ERROR "kobento-libs not found at ${KOBENTO_LIBS_DIR}: check it out next to kobento-tools, or pass -DKOBENTO_LIBS_DIR=…")
endif()
add_subdirectory("${KOBENTO_LIBS_DIR}" kobento-libs)

add_subdirectory(rc2json)
add_subdirectory(rc2image)
add_subdirectory(rc2layout)

# The 3D scene harness used by conformance/3d-parity.sh. -ffp-contract=off as rccore: the
# 3D renderer must match the Java reference bit for bit.
find_package(ZLIB REQUIRED)
add_executable(d3scene d3scene/main.cpp)
target_link_libraries(d3scene PRIVATE rccore ZLIB::ZLIB)
target_compile_options(d3scene PRIVATE -ffp-contract=off)

# The Core Graphics backend is built for iOS out of kobento-ios/iosNative, but its source is
# plain CG/CoreText/ImageIO and so builds on macOS unchanged. Building it here is what lets
# rc2image-native diff the two backends on the desktop instead of on a phone.
if(APPLE AND EXISTS "${KOBENTO_IOS_DIR}/iosNative/CMakeLists.txt")
    enable_language(OBJCXX)
    add_subdirectory("${KOBENTO_IOS_DIR}/iosNative" rcnative)
    add_subdirectory(rc2image-native)
endif()
