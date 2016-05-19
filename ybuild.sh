# environment vars that needs to be defined before running this
#
# ALKITAB_PROPRIETARY_DIR = directory where the proprietary (non-opensourced) resources are located
#
# SIGN_KEYSTORE = where the keystore is
# SIGN_ALIAS = key alias
# SIGN_PASSWORD = (string)

SUPER_PROJECT_NAME=androidbible
MAIN_PROJECT_NAME=Alkitab

#############################################

THIS_SCRIPT_FILE=$(cd `dirname "${BASH_SOURCE[0]}"` && pwd)/`basename "${BASH_SOURCE[0]}"`
THIS_SCRIPT_DIR=`dirname $THIS_SCRIPT_FILE`

set -e  # Exit the script as soon as one of the commands failed

if [ "$ALKITAB_PROPRIETARY_DIR" == "" ] ; then
	echo 'ALKITAB_PROPRIETARY_DIR not defined'
	exit 1
fi

if [ "$SIGN_KEYSTORE" == "" -o \! -r "$SIGN_KEYSTORE" ] ; then
	echo 'SIGN_KEYSTORE not defined or not readable'
	exit 1
fi

if [ "$SIGN_ALIAS" == "" ] ; then
	echo 'SIGN_ALIAS not defined'
	exit 1
fi

if [ "$SIGN_PASSWORD" == "" ] ; then
	echo 'SIGN_PASSWORD not defined'
	exit 1
fi

if [ "$FLAVOR" == "" ] ; then
	echo 'FLAVOR not defined'
	exit 1
fi

if [ \! \( -d "$MAIN_PROJECT_NAME" \) ] ; then
	echo "Must be run from $SUPER_PROJECT_NAME dir, which contains $MAIN_PROJECT_NAME, etc directories"
	exit 1
fi

# get the value of a gradle attribute
get_attr() {
	FILE="$1"
	ATTR="$2"
	REGEX="s/^.*$ATTR +'?([^']+)'?.*$/\\1/"
	cat "$FILE" | grep -E "$ATTR" | sed -E "$REGEX"
}

# replace '0000000' on the specified filename with the last commit hash of the git repo
write_last_commit_hash() {
	FILE="$1"
	echo 'Setting last commit hash: '$LAST_COMMIT_HASH' to '$FILE
	sed -i '' "s/0000000/$LAST_COMMIT_HASH/g" "$FILE"
}

overlay() {
	P_SRC="$1"
	P_DST="$2"

	SRC="$THIS_SCRIPT_DIR/ybuild/overlay/$PKGDIST/$P_SRC"
	DST="$BUILD_MAIN_PROJECT_DIR/src/main/$P_DST"

	echo "Overlaying $P_DST with $P_SRC..."

	if [ \! -e `dirname "$DST"` ] ; then
		echo 'Making dir for overlay destination: ' "`dirname "$DST"`" '...'
		mkdir -p "`dirname "$DST"`"
	fi

	cp "$SRC" "$DST" || read
}

# START BUILD-SPECIFIC

if [ "$BUILD_PACKAGE_NAME" == "" ] ; then
	echo 'BUILD_PACKAGE_NAME not defined'
	exit 1
fi

if [ "$BUILD_DIST" == "" ] ; then
	echo 'BUILD_DIST not defined'
	exit 1
fi

# END BUILD-SPECIFIC


echo 'Creating 500 MB ramdisk...'

BUILD_NAME=$SUPER_PROJECT_NAME-build-`date "+%Y%m%d-%H%M%S"`
diskutil erasevolume HFS+ $BUILD_NAME `hdiutil attach -nomount ram://1024000`

BUILD_DIR=/Volumes/$BUILD_NAME

echo 'Build dir:' $BUILD_DIR

if [ ! -d $BUILD_DIR ] ; then
	echo 'Build dir not mounted correctly'
	exit 1
fi

echo -n 'Last commit hash: '
LAST_COMMIT_HASH=`git log -1 --format='format:%h'`
echo $LAST_COMMIT_HASH

echo "Copying $SUPER_PROJECT_NAME..."
mkdir $BUILD_DIR/$SUPER_PROJECT_NAME
rsync -a --exclude ".git/" --exclude "*/build/" ./ $BUILD_DIR/$SUPER_PROJECT_NAME/

echo 'Going to' $BUILD_DIR/$SUPER_PROJECT_NAME
pushd $BUILD_DIR/$SUPER_PROJECT_NAME

	BUILD_MAIN_PROJECT_DIR=$BUILD_DIR/$SUPER_PROJECT_NAME/$MAIN_PROJECT_NAME

	pushd $MAIN_PROJECT_NAME/src/main

		# START BUILD-SPECIFIC

		PKGDIST="$BUILD_PACKAGE_NAME-$BUILD_DIST"

		echo '========================================='
		echo 'Build Config for THIS build:'
		echo '  BUILD_PACKAGE_NAME    = ' $BUILD_PACKAGE_NAME
		echo '  BUILD_DIST            = ' $BUILD_DIST
		echo '  PKGDIST               = ' $PKGDIST
		echo '  FLAVOR                = ' $FLAVOR
		echo '========================================='

		echo 'Removing dummy version on assets/internal...'
		rm -rf assets/internal

		TEXT_RAW="$ALKITAB_PROPRIETARY_DIR/overlay/$BUILD_PACKAGE_NAME/text_raw/"
		mkdir assets/internal
		echo "Copying text overlay from $TEXT_RAW..."
		if ! cp -R $TEXT_RAW assets/internal ; then
			echo 'Copy text overlay FAILED'
			exit 1
		fi

		# END BUILD-SPECIFIC

		MANIFEST_VERSION_CODE=`get_attr ../../build.gradle versionCode`
		MANIFEST_VERSION_NAME=`get_attr ../../build.gradle versionName`

		echo '========================================='
		echo 'From build.gradle:'
		echo '  Version code    = ' $MANIFEST_VERSION_CODE
		echo '  Version name    = ' $MANIFEST_VERSION_NAME
		echo ''
		echo 'SIGN_KEYSTORE   = ' $SIGN_KEYSTORE
		echo 'SIGN_ALIAS      = ' $SIGN_ALIAS
		echo 'SIGN_PASSWORD   = ' '.... =)'
		echo '========================================='

		if [ -e res/values/last_commit.xml ] ; then
			write_last_commit_hash res/values/last_commit.xml
		fi

	popd

	chmod +x ./gradlew
	echo 'Running gradlew from' `pwd`
	./gradlew --no-daemon assemble${FLAVOR^}Release

	FINAL_APK="$BUILD_MAIN_PROJECT_DIR/build/outputs/apk/$MAIN_PROJECT_NAME-$FLAVOR-release.apk"

	if [ \! -r "$FINAL_APK" ] ; then
		echo "$FINAL_APK" 'not found.'
		exit 1
	fi

	OUTPUT="$BUILD_DIR/$MAIN_PROJECT_NAME-$MANIFEST_VERSION_CODE-$MANIFEST_VERSION_NAME-$LAST_COMMIT_HASH-$PKGDIST.apk"
	mv "$FINAL_APK" "$OUTPUT"
	echo 'BUILD SUCCESSFUL. Output:' "$OUTPUT"

popd
