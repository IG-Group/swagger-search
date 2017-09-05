#!/usr/bin/env bash

set -eu

if [[ "${TRAVIS_BRANCH}" != "master" ]]; then
    exit 0
fi

if [[ "${TRAVIS_PULL_REQUEST}" != "false" ]]; then
    exit 0
fi

VERSION="`date +%Y%m%d`.$TRAVIS_BUILD_NUMBER"
echo "Building version $VERSION"

# CREATE GIT TAG
git config --global user.email "builds@travis-ci.com"
git config --global user.name "Travis CI"
#git checkout "$TRAVIS_BRANCH"
export GIT_TAG=v$VERSION
#git commit -m "Set next build VERSION number" version
git tag $GIT_TAG -a -m "Generated tag from TravisCI build $TRAVIS_BUILD_NUMBER"
echo "Tag done"
git push --quiet https://$GITHUBKEY@github.com/IG-Group/swagger-search $GIT_TAG > /dev/null 2>&1
echo "Pushing"


lein with-profile +set-version set-version $VERSION
lein with-profile +not-lib uberjar
ls -l target