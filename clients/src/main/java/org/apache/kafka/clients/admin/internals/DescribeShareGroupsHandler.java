/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.kafka.clients.admin.internals;

import org.apache.kafka.clients.admin.MemberAssignment;
import org.apache.kafka.clients.admin.MemberDescription;
import org.apache.kafka.clients.admin.ShareGroupDescription;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.ShareGroupState;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.message.ShareGroupDescribeRequestData;
import org.apache.kafka.common.message.ShareGroupDescribeResponseData;
import org.apache.kafka.common.protocol.Errors;
import org.apache.kafka.common.requests.AbstractResponse;
import org.apache.kafka.common.requests.FindCoordinatorRequest;
import org.apache.kafka.common.requests.FindCoordinatorRequest.CoordinatorType;
import org.apache.kafka.common.requests.MetadataResponse;
import org.apache.kafka.common.requests.ShareGroupDescribeRequest;
import org.apache.kafka.common.requests.ShareGroupDescribeResponse;
import org.apache.kafka.common.utils.LogContext;
import org.apache.kafka.common.utils.Utils;

import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DescribeShareGroupsHandler extends AdminApiHandler.Batched<CoordinatorKey, ShareGroupDescription> {

    private final boolean includeAuthorizedOperations;
    private final Logger log;
    private final AdminApiLookupStrategy<CoordinatorKey> lookupStrategy;

    public DescribeShareGroupsHandler(
          boolean includeAuthorizedOperations,
          LogContext logContext) {
        this.includeAuthorizedOperations = includeAuthorizedOperations;
        this.log = logContext.logger(DescribeShareGroupsHandler.class);
        this.lookupStrategy = new CoordinatorStrategy(CoordinatorType.GROUP, logContext);
    }

    private static Set<CoordinatorKey> buildKeySet(Collection<String> groupIds) {
        return groupIds.stream()
            .map(CoordinatorKey::byGroupId)
            .collect(Collectors.toSet());
    }

    public static AdminApiFuture.SimpleAdminApiFuture<CoordinatorKey, ShareGroupDescription> newFuture(Collection<String> groupIds) {
        return AdminApiFuture.forKeys(buildKeySet(groupIds));
    }

    @Override
    public String apiName() {
        return "describeShareGroups";
    }

    @Override
    public AdminApiLookupStrategy<CoordinatorKey> lookupStrategy() {
        return lookupStrategy;
    }

    @Override
    public ShareGroupDescribeRequest.Builder buildBatchedRequest(int coordinatorId, Set<CoordinatorKey> keys) {
        List<String> groupIds = keys.stream().map(key -> {
            if (key.type != FindCoordinatorRequest.CoordinatorType.GROUP) {
                throw new IllegalArgumentException("Invalid transaction coordinator key " + key +
                    " when building `DescribeShareGroups` request");
            }
            return key.idValue;
        }).collect(Collectors.toList());
        ShareGroupDescribeRequestData data = new ShareGroupDescribeRequestData()
            .setGroupIds(groupIds)
            .setIncludeAuthorizedOperations(includeAuthorizedOperations);
        return new ShareGroupDescribeRequest.Builder(data, true);
    }

    @Override
    public ApiResult<CoordinatorKey, ShareGroupDescription> handleResponse(
            Node coordinator,
            Set<CoordinatorKey> groupIds,
            AbstractResponse abstractResponse) {
        final ShareGroupDescribeResponse response = (ShareGroupDescribeResponse) abstractResponse;
        final Map<CoordinatorKey, ShareGroupDescription> completed = new HashMap<>();
        final Map<CoordinatorKey, Throwable> failed = new HashMap<>();
        final Set<CoordinatorKey> groupsToUnmap = new HashSet<>();

        for (ShareGroupDescribeResponseData.DescribedGroup describedGroup : response.data().groups()) {
            CoordinatorKey groupIdKey = CoordinatorKey.byGroupId(describedGroup.groupId());
            Errors error = Errors.forCode(describedGroup.errorCode());
            if (error != Errors.NONE) {
                handleError(groupIdKey, error, describedGroup.errorMessage(), failed, groupsToUnmap);
                continue;
            }

            final List<MemberDescription> memberDescriptions = new ArrayList<>(describedGroup.members().size());
            final Set<AclOperation> authorizedOperations = validAclOperations(describedGroup.authorizedOperations());

            describedGroup.members().forEach(groupMember ->
                memberDescriptions.add(new MemberDescription(
                    groupMember.memberId(),
                    groupMember.clientId(),
                    groupMember.clientHost(),
                    new MemberAssignment(convertAssignment(groupMember.assignment()))
                ))
            );

            final ShareGroupDescription shareGroupDescription =
                new ShareGroupDescription(groupIdKey.idValue,
                    memberDescriptions,
                    ShareGroupState.parse(describedGroup.groupState()),
                    coordinator,
                    authorizedOperations);
            completed.put(groupIdKey, shareGroupDescription);
        }

        return new ApiResult<>(completed, failed, new ArrayList<>(groupsToUnmap));
    }

    private Set<TopicPartition> convertAssignment(ShareGroupDescribeResponseData.Assignment assignment) {
        return assignment.topicPartitions().stream().flatMap(topic ->
            topic.partitions().stream().map(partition ->
                new TopicPartition(topic.topicName(), partition)
            )
        ).collect(Collectors.toSet());
    }

    private void handleError(
            CoordinatorKey groupId,
            Errors error,
            String errorMsg,
            Map<CoordinatorKey, Throwable> failed,
            Set<CoordinatorKey> groupsToUnmap) {
        switch (error) {
            case GROUP_AUTHORIZATION_FAILED:
                log.debug("`DescribeShareGroups` request for group id {} failed due to error {}", groupId.idValue, error);
                failed.put(groupId, error.exception(errorMsg));
                break;

            case COORDINATOR_LOAD_IN_PROGRESS:
                // If the coordinator is in the middle of loading, then we just need to retry
                log.debug("`DescribeShareGroups` request for group id {} failed because the coordinator " +
                    "is still in the process of loading state. Will retry", groupId.idValue);
                break;

            case COORDINATOR_NOT_AVAILABLE:
            case NOT_COORDINATOR:
                // If the coordinator is unavailable or there was a coordinator change, then we unmap
                // the key so that we retry the `FindCoordinator` request
                log.debug("`DescribeShareGroups` request for group id {} returned error {}. " +
                    "Will attempt to find the coordinator again and retry", groupId.idValue, error);
                groupsToUnmap.add(groupId);
                break;

            case GROUP_ID_NOT_FOUND:
                log.error("`DescribeShareGroups` request for group id {} failed because the group does not exist.", groupId.idValue);
                failed.put(groupId, error.exception(errorMsg));
                break;

            default:
                log.error("`DescribeShareGroups` request for group id {} failed due to unexpected error {}", groupId.idValue, error);
                failed.put(groupId, error.exception(errorMsg));
        }
    }

    private Set<AclOperation> validAclOperations(final int authorizedOperations) {
        if (authorizedOperations == MetadataResponse.AUTHORIZED_OPERATIONS_OMITTED) {
            return null;
        }
        return Utils.from32BitField(authorizedOperations)
            .stream()
            .map(AclOperation::fromCode)
            .filter(operation -> operation != AclOperation.UNKNOWN
                && operation != AclOperation.ALL
                && operation != AclOperation.ANY)
            .collect(Collectors.toSet());
    }
}
