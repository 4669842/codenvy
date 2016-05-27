#!/bin/bash
#
# CODENVY CONFIDENTIAL
# ________________
#
# [2012] - [2015] Codenvy, S.A.
# All Rights Reserved.
# NOTICE: All information contained herein is, and remains
# the property of Codenvy S.A. and its suppliers,
# if any. The intellectual and technical concepts contained
# herein are proprietary to Codenvy S.A.
# and its suppliers and may be covered by U.S. and Foreign Patents,
# patents in process, and are protected by trade secret or copyright law.
# Dissemination of this information or reproduction of this material
# is strictly forbidden unless prior written permission is obtained
# from Codenvy S.A..
#

[ -f "./lib.sh" ] && . ./lib.sh
[ -f "../lib.sh" ] && . ../lib.sh

printAndLog "TEST CASE: Test version command with Codenvy 4.x"
vagrantUp ${SINGLE_NODE_VAGRANT_FILE}

installImCliClient
validateInstalledImCliClientVersion

# test getting latest stable version of codenvy by default
executeIMCommand "version"
validateExpectedString ".*\"artifact\".\:.\"codenvy\".*\"availableVersion\".*.*\"stable\".*"

installCodenvy ${LATEST_CODENVY4_VERSION}
validateInstalledCodenvyVersion ${LATEST_CODENVY4_VERSION}

executeIMCommand "version"

# take into account that the latest Codenvy 4 version at the Updater Server could have 'STABLE' label
if [[ ${LATEST_CODENVY4_VERSION} == ${LATEST_STABLE_CODENVY_VERSION} ]]; then
    EXPECTED_LABEL=STABLE
else
    EXPECTED_LABEL=UNSTABLE
fi

validateExpectedString ".*\"artifact\".\:.\"codenvy\".*\"version\".\:.\"${LATEST_CODENVY4_VERSION}\".*\"label\".\:.\"${EXPECTED_LABEL}\".*"

vagrantDestroy
