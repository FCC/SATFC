cmake_minimum_required(VERSION 2.8)
project(SATenstein)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O3 -lm -Wall -Wno-unused-variable -Wno-unused-but-set-variable -Wno-comment")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11")

# Need this because of inline functions
if (APPLE)
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -std=gnu89")
ENDIF()

if (CMAKE_SYSTEM_NAME STREQUAL Linux) 
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -lrt")
ENDIF()

include_directories( src )
include_directories( test )


##########
# SOURCE #
##########

set(SOURCE_FILES
        src/adaptg2wsatplus.c
        src/adaptnovelty.c
        src/algorithms.c
        src/derandomized.c
        src/ddfw.c
        src/satenstein.c
        src/gsat.c
        src/gsat-tabu.c
        src/gwsat.c
        src/hsat.c
        src/hwsat.c
        src/irots.c
        src/mt19937ar.c
        src/mylocal.c
        src/novelty.c
        src/parameters.c
        src/paws.c
        src/random.c
        src/reports.c
        src/rgsat.c
        src/rnovelty.c
        src/rots.c
        src/samd.c
        src/saps.c
        src/ubcsat-help.c
        src/ubcsat-internal.c
        src/ubcsat-io.c
        src/ubcsat-mem.c
        src/ubcsat-reports.c
        src/ubcsat-time.c
        src/ubcsat-triggers.c
        src/vw.c
        src/walksat.c
        src/walksat-tabu.c
        src/dcca.c
        src/satenstein-types.h
        src/sparrow.c

        src/fmemopen.c
        src/fmemopen.h
)

add_executable(ubcsat src/ubcsat.c ${SOURCE_FILES})
target_link_libraries(ubcsat m)


#########
# TESTS #
#########

set(TEST_FILES ${SOURCE_FILES}
        test/AddRemoveListsTest.cpp
        test/DccaTest.cpp
        test/FacadeTest.cpp
)

# Add google test framework
ADD_SUBDIRECTORY(lib/gtest-1.7.0 gtest)
include_directories(gtest STATIC lib/gtest-1.7.0/include )

add_executable(runTests ${TEST_FILES} test/TestRunner.cpp)
target_link_libraries(runTests gtest jnaubcsat)


#############
# RESOURCES #
#############

# The following command copies the resources folder into the binary directory,
#  making it possible to load resources from a relative path within the project
add_custom_target(migrateResources
        COMMAND ${CMAKE_COMMAND} -E copy_directory
        ${CMAKE_SOURCE_DIR}/res
        ${CMAKE_BINARY_DIR}/res
)
add_dependencies(runTests migrateResources)


##########
# FACADE #
##########

add_library(jnasatenstein MODULE src/ubcsat-facade.cpp ${SOURCE_FILES})


