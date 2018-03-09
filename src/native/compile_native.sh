#!/bin/sh

if [ ! -d "$1/native" ] ; then
	mkdir -p "$1/native"
fi

TARGETDIR=$1/native
BASEDIR=$2

EXTERNAL_CLASSES=$3

read -r -d '' SCRIPT <<'EOM'
JAVAC?=$(JAVA_HOME)/bin/javac
JAVADOC?=$(JAVA_HOME)/bin/javadoc
JAR?=$(JAVA_HOME)/bin/jar
JAVAH?=$(JAVA_HOME)/bin/javah
GCJ?=gcj
CC?=gcc
LD?=gcc
JPPFLAGS+=-C -P
CFLAGS+=-Wall -Os -pedantic -Werror
CSTD?=-std=c99
CSHAREFLAG+=-fpic -fno-stack-protector
GCJJNIFLAG=-fjni
JVERCFLAGS+=-source 1.8
JCFLAGS+=

ifeq ($(JAVA_HOME),)
 $(error JAVA_HOME is not set)
endif

INCLUDES+=-I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux
JAVADOCFLAGS?=-quiet -author -link http://java.sun.com/j2se/1.4.2/docs/api/

LDVER?=$(shell ld -v | cut -d' ' -f1)

ifeq ($(LDVER),GNU)
LDSHAREFLAGS+=-fpic -shared
else
LDSHAREFLAGS+=-lc
endif

SRC=$(shell find BASEDIR/src/main/java/cx -name '*.java')

.NOPARALLEL:
.NO_PARALLEL:
.NOTPARALLEL:

all: libunix-java.so

classes: .classes 
.classes: $(SRC) 
	mkdir -p classes
	$(JAVAC) $(JVERCFLAGS) $(JCFLAGS) -d classes -cp classes:EXTERNAL_CLASSES $^
	touch .classes
	
clean:
	rm -rf *.o *.h *.so classes 

%.o: %.c %.h
	$(CC) $(CFLAGS) $(CSTD) $(CSHAREFLAG) $(INCLUDES) -c -o $@ $<
lib%.so: %.o
	$(CC) $(LDFLAGS) $(LDSHAREFLAGS) -o $@ $<
unix-java.h: .classes
	$(JAVAH) -classpath classes:EXTERNAL_CLASSES -o $@ cx.ath.matthew.unix.UnixServerSocket cx.ath.matthew.unix.UnixSocket cx.ath.matthew.unix.USInputStream cx.ath.matthew.unix.USOutputStream

EOM

cp unix-java.* $TARGETDIR

echo "$SCRIPT" | sed "s|EXTERNAL_CLASSES|$EXTERNAL_CLASSES|g" | sed "s|BASEDIR|$BASEDIR|g" >> $TARGETDIR/Makefile

cd $TARGETDIR
make clean
make all
