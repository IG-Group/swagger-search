#!/usr/bin/env bash

set -eu

increment_version ()
{
  declare -a part=( ${1//\./ } )
  declare    new
  declare -i carry=1

  for (( CNTR=${#part[@]}-1; CNTR>=0; CNTR-=1 )); do
    new=$((part[CNTR]+carry))
    carry=0
    part[CNTR]=${new}
  done
  new="${part[*]}"
  echo -e "${new// /.}"
}

echo "Branch is $TRAVIS_BRANCH"

if ! [[ "${TRAVIS_BRANCH}" == "master" || "${TRAVIS_BRANCH}" =~ ^v[0-9] ]]; then
    exit 0
fi

if [[ "${TRAVIS_PULL_REQUEST}" != "false" ]]; then
    exit 0
fi

AUTOMATED_AUTHOR_EMAIL=builds@travis-ci.com
LAST_COMMIT_AUTHOR_EMAIL=$(git --no-pager show -s --format='%ae' HEAD)
echo "last commit ID: $LAST_COMMIT_AUTHOR_EMAIL"
VERSION=`cat version`

if [ $LAST_COMMIT_AUTHOR_EMAIL != $AUTOMATED_AUTHOR_EMAIL ]; then

    # CREATE GIT TAG
    git config --global user.email $AUTOMATED_AUTHOR_EMAIL
    git config --global user.name "Travis CI"
    git checkout "$TRAVIS_BRANCH"

    increment_version $VERSION > version
    echo "Current version $VERSION, next version `cat version`"
    export GIT_TAG=v$VERSION
    git commit -m "Set release build VERSION number" version
    git tag $GIT_TAG -a -m "Generated tag from TravisCI build $TRAVIS_BUILD_NUMBER"
    echo "Tag done"
    git push --tags --quiet https://$GITHUBKEY@github.com/IG-Group/swagger-search master > /dev/null 2>&1
    echo "Pushing"

else
    if [[ "${TRAVIS_BRANCH}" =~ ^v[1-9] ]]; then
        echo "Deploying version $VERSION"
        lein with-profile +set-version set-version $VERSION
        lein with-profile +not-lib uberjar
        lein deploy releases
    else
        exit 0
    fi
fi