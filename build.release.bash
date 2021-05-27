#!/bin/bash

#
# UserManager Remote Installation Script
# This script installs the module to the server when updates are released.
# Only works on AutoRelease enabled repository services.
#
# Also, it needs the autorelease.allow.install file on the server.
#

function prepare() {
	destination "/etc/connective-http/"
	buildOutput "build/libs/"
}

function build() {
    chmod +x gradlew
	chmod +x createlocalserver.sh
	./createlocalserver.sh
    ./gradlew build
}

function install() {
	rm -rfv "$DEST/modules/"UserManager-*.jar
	cp -rfv "$BUILDDIR/"UserManager-*.jar "$DEST/modules"
}

function postInstall() {
    log Rebooting HTTP server...
	systemctl restart connective-http
}
