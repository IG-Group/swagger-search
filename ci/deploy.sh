#!/usr/bin/env bash

set -eu

if [[ "${TRAVIS_BRANCH}" != "master" ]]; then
    exit 0
fi

if [[ "${TRAVIS_PULL_REQUEST}" != "false" ]]; then
    exit 0
fi

increment_version ()
{
  declare -a part=( ${1//\./ } )
  declare    new
  declare -i carry=1

  for (( CNTR=${#part[@]}-1; CNTR>=0; CNTR-=1 )); do
    len=${#part[CNTR]}
    new=$((part[CNTR]+carry))
    [ ${#new} -gt $len ] && carry=1 || carry=0
    [ $CNTR -gt 0 ] && part[CNTR]=${new: -len} || part[CNTR]=${new}
  done
  new="${part[*]}"
  echo -e "${new// /.}"
}

VERSION=`cat version`
echo "Building version $VERSION"
increment_version $VERSION > version

# CREATE GIT TAG
git config --global user.email "builds@travis-ci.com"
git config --global user.name "Travis CI"
git checkout "$TRAVIS_BRANCH"
export GIT_TAG=v$VERSION
git commit -m "Set next build VERSION number" version
git tag $GIT_TAG -a -m "Generated tag from TravisCI build $TRAVIS_BUILD_NUMBER"
echo "Tag done"
git push --quiet https://$GITHUBKEY@github.com/dev-confidence/example-backend-api $GIT_TAG > /dev/null 2>&1
echo "Pushing"


lein with-profile +set-version set-version $VERSION
lein with-profile +not-lib uberjar