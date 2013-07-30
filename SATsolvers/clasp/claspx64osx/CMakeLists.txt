cmake_minimum_required(VERSION 2.6)

project(clasp)
add_definitions( -DWITH_THREADS=0 )
add_subdirectory(libclasp)
add_subdirectory(libprogram_opts)
add_subdirectory(app)


