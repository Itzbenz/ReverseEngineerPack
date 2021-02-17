#!/bin/sh

command -v apktool >/dev/null 2>&1 || { echo >&2 "I require apktool but it's not installed. Aborting."; exit 1; }
command -v keytool >/dev/null 2>&1 || { echo >&2 "I require keytool but it's not installed. Aborting."; exit 1; }
command -v jarsigner >/dev/null 2>&1 || { echo >&2 "I require jarsigner but it's not installed. Aborting."; exit 1; }

TMPDIR=`mktemp -d 2>/dev/null || mktemp -d -t 'apkdebug'`
APK=$1     
DEBUG_APK="${APK%.*}.debug.apk"
if [ -f $APK ]; then
	(echo "=> Unpacking APK..." &&
	apktool -q d $APK -o $TMPDIR/app &&
	echo "=> Adding debug flag..." && 
	sed -i -e "s/android:debuggable=\"[^\"]*\" *//;s/<application /<application android:debuggable=\"true\" /" $TMPDIR/app/AndroidManifest.xml && 
	echo "=> Repacking APK..." &&
	apktool -q b $TMPDIR/app -o $DEBUG_APK &&
	echo "=> Signing APK..." &&
	keytool -genkey -noprompt \
	 -alias alias1 \
	 -dname "CN=Unknown, OU=Unknown, O=Unknown, L=Unknown, S=Unknown, C=Unknown" \
	 -keystore $TMPDIR/keystore \
	 -storepass password \
	 -keyalg RSA \
	 -keypass password &&
	jarsigner -keystore $TMPDIR/keystore -storepass password -keypass password $DEBUG_APK alias1 > /dev/null 2>&1 &&
	echo "=> Checking your debug APK..." && 
	(jarsigner -verify $DEBUG_APK > /dev/null 2>&1 &&
	echo "\n======" &&
	echo "Success!"
	echo "======\n" &&
	echo "(deleting temporary directory...)\n" &&
	echo "Your debug APK : $DEBUG_APK" && 
	rm -rf $TMPDIR)) || (echo "=====" && echo "Something failed :'(" && echo "Leaving temporary dir $TMPDIR if you want to inspect what went wrong.")
else
	echo "File not found: $APK"
fi
