./build.sh && valgrind --leak-check=yes --track-origins=yes ./test.out > log.txt 2>&1 | less log.txt
