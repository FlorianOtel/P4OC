# Local dev convenience — not part of the project's standard build (see mise.toml).
# Override these through the caller environment when the defaults do not apply.
JAVA_HOME     ?= /usr/lib/jvm/java-17-openjdk-amd64
ANDROID_HOME  ?= $(HOME)/Android/Sdk
APP_ID       := dev.blazelight.p4oc.debug
MAIN_ACTIVITY := dev.blazelight.p4oc.MainActivity
PORT         ?= 4096
PORT2        ?= 4097

export JAVA_HOME
export ANDROID_HOME

.DEFAULT_GOAL := run

.PHONY: run install build uninstall logcat serve serve2 clean

build:
	./gradlew :app:assembleDebug

install:
	./gradlew :app:installDebug

run: install
	adb shell am start -n $(APP_ID)/$(MAIN_ACTIVITY)

uninstall:
	adb uninstall $(APP_ID)

logcat:
	adb logcat --pid=$$(adb shell pidof $(APP_ID))

# Starts the opencode server and forwards it over USB (adb reverse) so the
# device can reach it at http://127.0.0.1:$(PORT) regardless of Wi-Fi.
serve:
	adb reverse tcp:$(PORT) tcp:$(PORT)
	opencode serve --hostname 0.0.0.0 --port $(PORT)

serve2:
	adb reverse tcp:$(PORT2) tcp:$(PORT2)
	opencode serve --hostname 0.0.0.0 --port $(PORT2)

clean:
	./gradlew clean
