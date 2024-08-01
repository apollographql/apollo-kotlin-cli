#!/bin/bash
#
# Licensed under the MIT license
# <LICENSE-MIT or https://opensource.org/licenses/MIT>, at your
# option. This file may not be copied, modified, or distributed
# except according to those terms.

set -u

REPOSITORY_OWNER=apollographql
REPOSITORY_NAME=apollo-kotlin-cli

download_binary_and_run_installer() {
    need_cmd curl
    need_cmd mktemp
    need_cmd chmod
    need_cmd mkdir
    need_cmd rm
    need_cmd rmdir
    need_cmd unzip

    LATEST_VERSION=$(curl -Ls -o /dev/null -w %{url_effective} https://github.com/$REPOSITORY_OWNER/$REPOSITORY_NAME/releases/latest)
    LATEST_VERSION=${LATEST_VERSION##*/v}

    if [ -z ${VERSION:-} ]; then
        # VERSION is either not set or empty
        DOWNLOAD_VERSION=$LATEST_VERSION
    else
        # VERSION set and not empty
        DOWNLOAD_VERSION=$VERSION
    fi

    local _tardir="$REPOSITORY_NAME-$DOWNLOAD_VERSION"
    local _url="https://github.com/$REPOSITORY_OWNER/$REPOSITORY_NAME/releases/download/v$DOWNLOAD_VERSION/${_tardir}.zip"
    local _tmpdir="$(mktemp -d 2>/dev/null || ensure mktemp -d -t $REPOSITORY_NAME)"
    local _file="$_tmpdir/input.zip"

    say "downloading $REPOSITORY_NAME from $_url" 1>&2

    ensure mkdir -p "$_tmpdir"
    downloader "$_url" "$_file"
    if [ $? != 0 ]; then
      say "failed to download $_url"
      say "this may be a standard network error, but it may also indicate"
      say "that the release process is not working. When in doubt"
      say "please feel free to open an issue!"
      say "https://github.com/$REPOSITORY_OWNER/$REPOSITORY_NAME/issues/new/choose"
      exit 1
    fi

    ensure unzip -q "$_file" -d "$_tmpdir"

    "$_tmpdir/$REPOSITORY_NAME-$DOWNLOAD_VERSION/bin/$REPOSITORY_NAME" "install"
    local _retval=$?

    ignore rm -rf "$_tmpdir"

    return "$_retval"
}

say() {
    local green=`tput setaf 2 2>/dev/null || echo ''`
    local reset=`tput sgr0 2>/dev/null || echo ''`
    echo "$1"
}

err() {
    local red=`tput setaf 1 2>/dev/null || echo ''`
    local reset=`tput sgr0 2>/dev/null || echo ''`
    say "${red}ERROR${reset}: $1" >&2
    exit 1
}

need_cmd() {
    if ! check_cmd "$1"
    then err "need '$1' (command not found)"
    fi
}

check_cmd() {
    command -v "$1" > /dev/null 2>&1
    return $?
}

need_ok() {
    if [ $? != 0 ]; then err "$1"; fi
}

assert_nz() {
    if [ -z "$1" ]; then err "assert_nz $2"; fi
}

# Run a command that should never fail. If the command fails execution
# will immediately terminate with an error showing the failing
# command.
ensure() {
    "$@"
    need_ok "command failed: $*"
}

# This is just for indicating that commands' results are being
# intentionally ignored. Usually, because it's being executed
# as part of error handling.
ignore() {
    "$@"
}

# This wraps curl or wget. Try curl first, if not installed,
# use wget instead.
downloader() {
    if check_cmd curl
    then _dld=curl
    elif check_cmd wget
    then _dld=wget
    else _dld='curl or wget' # to be used in error message of need_cmd
    fi

    if [ "$1" = --check ]
    then need_cmd "$_dld"
    elif [ "$_dld" = curl ]
    then curl -sSfL "$1" -o "$2"
    elif [ "$_dld" = wget ]
    then wget "$1" -O "$2"
    else err "Unknown downloader"   # should not reach here
    fi
}

download_binary_and_run_installer "$@" || exit 1