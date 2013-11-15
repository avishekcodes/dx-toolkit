// Copyright (C) 2013 DNAnexus, Inc.
//
// This file is part of dx-toolkit (DNAnexus platform client libraries).
//
//   Licensed under the Apache License, Version 2.0 (the "License"); you may
//   not use this file except in compliance with the License. You may obtain a
//   copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
//   WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
//   License for the specific language governing permissions and limitations
//   under the License.

package com.dnanexus;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

/**
 * Utility class containing methods for searching for platform objects by various criteria.
 */
public final class DXSearch {
    @JsonInclude(Include.NON_NULL)
    private static class FindDataObjectsRequest {

        private static class ExactNameQuery implements NameQuery {
            private final String nameExact;

            private ExactNameQuery(String nameExact) {
                this.nameExact = nameExact;
            }

            @SuppressWarnings("unused")
            @JsonValue
            private Object getValue() {
                return this.nameExact;
            }
        }

        private static class GlobNameQuery implements NameQuery {
            private final String glob;

            private GlobNameQuery(String glob) {
                this.glob = glob;
            }

            @SuppressWarnings("unused")
            @JsonValue
            private Map<String, String> getValue() {
                return ImmutableMap.of("glob", this.glob);
            }
        }

        // The following polymorphic classes, and this interface, are for
        // generating the values that can appear in the "name" field of the
        // query.
        @JsonInclude(Include.NON_NULL)
        private static interface NameQuery {}

        private static class RegexpNameQuery implements NameQuery {
            private final String regexp;
            private final String flags;

            private RegexpNameQuery(String regexp) {
                this.regexp = regexp;
                this.flags = null;
            }

            private RegexpNameQuery(String regexp, String flags) {
                this.regexp = regexp;
                this.flags = flags;
            }

            @SuppressWarnings("unused")
            @JsonValue
            private Map<String, String> getValue() {
                ImmutableMap.Builder<String, String> mapBuilder = ImmutableMap.builder();
                mapBuilder.put("regexp", this.regexp);
                if (this.flags != null) {
                    mapBuilder.put("flags", this.flags);
                }
                return mapBuilder.build();
            }
        }

        @JsonInclude(Include.NON_NULL)
        private static class ScopeQuery {
            @SuppressWarnings("unused")
            @JsonProperty
            private final String project;
            @SuppressWarnings("unused")
            @JsonProperty
            private final String folder;
            @SuppressWarnings("unused")
            @JsonProperty
            private final Boolean recurse;

            private ScopeQuery(String projectId) {
                this(projectId, null, null);
            }

            private ScopeQuery(String projectId, String folder) {
                this(projectId, folder, null);
            }

            private ScopeQuery(String projectId, String folder, Boolean recurse) {
                this.project = projectId;
                this.folder = folder;
                this.recurse = recurse;
            }
        }

        @SuppressWarnings("unused")
        @JsonProperty
        private final NameQuery name;
        @SuppressWarnings("unused")
        @JsonProperty
        private final ScopeQuery scope;

        private FindDataObjectsRequest(FindDataObjectsRequestBuilder builder) {
            this.name = builder.nameQuery;
            this.scope = builder.scopeQuery;
        }

        public FindDataObjectsResponse execute(DXEnvironment env) {
            return new FindDataObjectsResponse(DXJSON.safeTreeToValue(
                    DXAPI.systemFindDataObjects(MAPPER.valueToTree(this), env),
                    FindDataObjectsResponseHash.class), DXEnvironment.create());
        }

    }

    /**
     * Builder class for formulating {@code findDataObjects} queries and executing them.
     *
     * <p>
     * Obtain an instance of this class via {@link #findDataObjects()}.
     * </p>
     */
    public static class FindDataObjectsRequestBuilder {
        private FindDataObjectsRequest.NameQuery nameQuery;
        private FindDataObjectsRequest.ScopeQuery scopeQuery;

        private final DXEnvironment env;

        private FindDataObjectsRequestBuilder() {
            this.env = DXEnvironment.create();
        }

        private FindDataObjectsRequestBuilder(DXEnvironment env) {
            this.env = env;
        }

        @VisibleForTesting
        FindDataObjectsRequest buildRequestHash() {
            // Use this method to test the JSON hash created by a particular
            // builder call without actually executing the request.
            return new FindDataObjectsRequest(this);
        }

        /**
         * Builds and executes the query.
         */
        public FindDataObjectsResponse execute() {
            return this.buildRequestHash().execute(this.env);
        }

        /**
         * Only returns objects in the specified folder of the specified project (not in
         * subfolders).
         *
         * <p>
         * This method may only be called once during the construction of a query, and is mutually
         * exclusive with {@link #inFolderOrSubfolders(DXContainer, String)} and
         * {@link #inProject(DXContainer)}.
         * </p>
         */
        public FindDataObjectsRequestBuilder inFolder(DXContainer project, String folder) {
            Preconditions.checkArgument(this.scopeQuery == null,
                    "Cannot specify inProject, inFolder, or inFolderOrSubfolders more than once");
            Preconditions.checkNotNull(project);
            Preconditions.checkNotNull(folder);
            this.scopeQuery = new FindDataObjectsRequest.ScopeQuery(project.getId(), folder);
            return this;
        }

        /**
         * Only returns objects in the specified folder of the specified project or in its
         * subfolders.
         *
         * <p>
         * This method may only be called once during the construction of a query, and is mutually
         * exclusive with {@link #inFolder(DXContainer, String)} and {@link #inProject(DXContainer)}
         * .
         * </p>
         */
        public FindDataObjectsRequestBuilder inFolderOrSubfolders(DXContainer project, String folder) {
            Preconditions.checkArgument(this.scopeQuery == null,
                    "Cannot specify inProject, inFolder, or inFolderOrSubfolders more than once");
            Preconditions.checkNotNull(project);
            Preconditions.checkNotNull(folder);
            this.scopeQuery = new FindDataObjectsRequest.ScopeQuery(project.getId(), folder, true);
            return this;
        }

        /**
         * Only returns objects in the specified project.
         *
         * <p>
         * This method may only be called once during the construction of a query, and is mutually
         * exclusive with {@link #inFolder(DXContainer, String)} and
         * {@link #inFolderOrSubfolders(DXContainer, String)}.
         * </p>
         */
        public FindDataObjectsRequestBuilder inProject(DXContainer project) {
            Preconditions.checkArgument(this.scopeQuery == null,
                    "Cannot specify inProject, inFolder, or inFolderOrSubfolders more than once");
            Preconditions.checkNotNull(project);
            this.scopeQuery = new FindDataObjectsRequest.ScopeQuery(project.getId());
            return this;
        }

        /**
         * Only returns objects whose names exactly equal the specified string.
         *
         * <p>
         * This method may only be called once during the construction of a query, and is mutually
         * exclusive with {@link #nameMatchesGlob(String)}, {@link #nameMatchesRegexp(String)}, and
         * {@link #nameMatchesRegexp(String, boolean)}.
         * </p>
         */
        public FindDataObjectsRequestBuilder nameMatchesExactly(String name) {
            Preconditions.checkArgument(this.nameQuery == null,
                    "Cannot specify nameMatches* methods more than once");
            Preconditions.checkNotNull(name);
            this.nameQuery = new FindDataObjectsRequest.ExactNameQuery(name);
            return this;
        }

        /**
         * Only returns objects whose name match the specified glob.
         *
         * <p>
         * This method may only be called once during the construction of a query, and is mutually
         * exclusive with {@link #nameMatchesExactly(String)}, {@link #nameMatchesRegexp(String)},
         * and {@link #nameMatchesRegexp(String, boolean)}.
         * </p>
         */
        public FindDataObjectsRequestBuilder nameMatchesGlob(String glob) {
            Preconditions.checkArgument(this.nameQuery == null,
                    "Cannot specify nameMatches* methods more than once");
            Preconditions.checkNotNull(glob);
            this.nameQuery = new FindDataObjectsRequest.GlobNameQuery(glob);
            return this;
        }

        /**
         * Only returns objects whose names match the specified regexp.
         *
         * <p>
         * This method may only be called once during the construction of a query, and is mutually
         * exclusive with {@link #nameMatchesExactly(String)}, {@link #nameMatchesGlob(String)}, and
         * {@link #nameMatchesRegexp(String, boolean)}.
         * </p>
         */
        public FindDataObjectsRequestBuilder nameMatchesRegexp(String regexp) {
            Preconditions.checkArgument(this.nameQuery == null,
                    "Cannot specify nameMatches* methods more than once");
            Preconditions.checkNotNull(regexp);
            this.nameQuery = new FindDataObjectsRequest.RegexpNameQuery(regexp);
            return this;
        }

        /**
         * Only returns objects whose names match the specified regexp (optionally allowing the
         * match to be case insensitive).
         *
         * <p>
         * This method may only be called once during the construction of a query, and is mutually
         * exclusive with {@link #nameMatchesExactly(String)}, {@link #nameMatchesGlob(String)}, and
         * {@link #nameMatchesRegexp(String)}.
         * </p>
         */
        public FindDataObjectsRequestBuilder nameMatchesRegexp(String regexp,
                boolean caseInsensitive) {
            Preconditions.checkArgument(this.nameQuery == null,
                    "Cannot specify nameMatches* methods more than once");
            Preconditions.checkNotNull(regexp);
            this.nameQuery =
                    new FindDataObjectsRequest.RegexpNameQuery(regexp, caseInsensitive ? "i" : null);
            return this;
        }
    }

    /**
     * The set of jobs that matched a {@code findDataObjects} query.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FindDataObjectsResponse {

        // TODO: lazily load results and provide an iterator in addition to
        // buffered List access

        private final FindDataObjectsResponseHash findDataObjectsResponseHash;
        private final DXEnvironment env;

        @VisibleForTesting
        FindDataObjectsResponse(FindDataObjectsResponseHash findDataObjectsResponseHash,
                DXEnvironment env) {
            this.findDataObjectsResponseHash = findDataObjectsResponseHash;
            this.env = env;
        }

        /**
         * Returns a {@code List} of the matching data objects.
         */
        public List<DXDataObject> asList() {
            // TODO: page through results until there are no more
            List<DXDataObject> output = Lists.newArrayList();
            for (FindDataObjectsResponseHash.Entry e : findDataObjectsResponseHash.results) {
                output.add(DXDataObject.getInstanceWithEnvironment(e.id,
                        DXContainer.getInstance(e.project), this.env));
            }
            return ImmutableList.copyOf(output);
        }
    }

    /**
     * Deserialized output from the system/findDataObjects route. Not directly accessible by users
     * (see FindDataObjectsResponse instead).
     */
    @VisibleForTesting
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class FindDataObjectsResponseHash {

        private static class Entry {
            @JsonProperty
            private String id;
            @JsonProperty
            private String project;
        }

        @JsonProperty
        private List<Entry> results;

        @SuppressWarnings("unused")
        @JsonProperty
        private String next;

    }

    @JsonInclude(Include.NON_NULL)
    private static class FindJobsRequest {
        // Fields of the input hash to the /system/findJobs API call
        @SuppressWarnings("unused")
        @JsonProperty
        private final String launchedBy;
        @SuppressWarnings("unused")
        @JsonProperty("project")
        private final String inProject;
        private final Date createdBefore;
        private final Date createdAfter;

        // Construct this object by pulling all its fields out of the builder.
        private FindJobsRequest(FindJobsRequestBuilder builder) {
            this.launchedBy = builder.launchedBy;
            this.inProject = builder.inProject;
            this.createdBefore = builder.createdBefore;
            this.createdAfter = builder.createdAfter;
        }

        /**
         * Executes the query and returns its result.
         */
        public FindJobsResponse execute(DXEnvironment env) {
            return new FindJobsResponse(
                    DXJSON.safeTreeToValue(DXAPI.systemFindJobs(MAPPER.valueToTree(this), env),
                            FindJobsResponseHash.class), env);
        }

        // Getter to support JSON serialization of createdAfter.
        @SuppressWarnings("unused")
        @JsonProperty("createdAfter")
        private Long getCreatedAfter() {
            if (createdAfter == null) {
                return null;
            }
            return createdAfter.getTime();
        }

        // Getter to support JSON serialization of createdBefore.
        @SuppressWarnings("unused")
        @JsonProperty("createdBefore")
        private Long getCreatedBefore() {
            if (createdBefore == null) {
                return null;
            }
            return createdBefore.getTime();
        }

    }

    /**
     * Builder class for formulating {@code findJobs} queries and executing them.
     *
     * <p>
     * Obtain an instance of this class via {@link #findJobs()}.
     * </p>
     */
    public static class FindJobsRequestBuilder {
        private String launchedBy = null;
        private String inProject = null;
        private Date createdBefore = null;
        private Date createdAfter = null;

        private final DXEnvironment env;

        private FindJobsRequestBuilder() {
            this.env = DXEnvironment.create();
        }

        private FindJobsRequestBuilder(DXEnvironment env) {
            this.env = env;
        }

        @VisibleForTesting
        FindJobsRequest buildRequestHash() {
            // Use this method to test the JSON hash created by a particular
            // builder call without actually executing the request.
            return new FindJobsRequest(this);
        }

        /**
         * Only return jobs created after the specified date.
         */
        public FindJobsRequestBuilder createdAfter(Date date) {
            Preconditions.checkArgument(this.createdAfter == null,
                    "Cannot specify createdAfter more than once");
            Preconditions.checkNotNull(date);
            this.createdAfter = date;
            return this;
        }

        /**
         * Only return jobs created before the specified date.
         */
        public FindJobsRequestBuilder createdBefore(Date date) {
            Preconditions.checkArgument(this.createdBefore == null,
                    "Cannot specify createdBefore more than once");
            Preconditions.checkNotNull(date);
            this.createdBefore = date;
            return this;
        }

        /**
         * Builds and executes the query.
         */
        public FindJobsResponse execute() {
            return this.buildRequestHash().execute(this.env);
        }

        /**
         * Only return jobs in the specified project.
         */
        public FindJobsRequestBuilder inProject(DXContainer project) {
            Preconditions.checkArgument(this.inProject == null,
                    "Cannot specify inProject more than once");
            Preconditions.checkNotNull(project);
            this.inProject = project.getId();
            return this;
        }

        /**
         * Only return jobs launched by the specified user.
         */
        public FindJobsRequestBuilder launchedBy(String user) {
            // TODO: consider changing the semantics of this and other
            // setters so that later calls overwrite earlier calls. Then
            // remove the restriction that the field may only be specified
            // once.
            Preconditions.checkArgument(this.launchedBy == null,
                    "Cannot specify launchedBy more than once");
            Preconditions.checkNotNull(user);
            this.launchedBy = user;
            return this;
        }

    }

    /**
     * The set of jobs that matched a {@code findJobs} query.
     */
    public static class FindJobsResponse {

        // TODO: lazily load results and provide an iterator in addition to
        // buffered List access

        private final FindJobsResponseHash findJobsResponseHash;
        private final DXEnvironment env;

        @VisibleForTesting
        FindJobsResponse(FindJobsResponseHash findJobsResponseHash, DXEnvironment env) {
            this.findJobsResponseHash = findJobsResponseHash;
            this.env = env;
        }

        /**
         * Returns a {@code List} of the matching jobs.
         */
        public List<DXJob> asList() {
            // TODO: page through results until there are no more
            List<DXJob> output = Lists.newArrayList();
            for (FindJobsResponseHash.Entry e : findJobsResponseHash.results) {
                output.add(DXJob.getInstanceWithEnvironment(e.id, this.env));
            }
            return ImmutableList.copyOf(output);
        }

    }

    /**
     * Deserialized output from the /system/findJobs route. Not directly accessible by users (see
     * FindJobsResponse instead).
     */
    @VisibleForTesting
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class FindJobsResponseHash {

        private static class Entry {
            @JsonProperty
            private String id;
        }

        @JsonProperty
        private List<Entry> results;

        @SuppressWarnings("unused")
        @JsonProperty
        private String next;

    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Returns a builder object that can be used to construct a query for finding data objects (and
     * then to execute it).
     *
     * <p>
     * Example use:
     * </p>
     *
     * <pre>
     * FindDataObjectsResponse fdor = DXSearch.findDataObjects().inProject(&quot;project-000000000000000000000000&quot;)
     *         .inFolder(&quot;/my/subfolder&quot;).execute();
     *
     * for (DXDataObject o : fdor.asList()) {
     *     System.out.println(o.getId());
     * }
     * </pre>
     */
    public static FindDataObjectsRequestBuilder findDataObjects() {
        return new FindDataObjectsRequestBuilder();
    }

    /**
     * Returns a builder object that can be used to construct a query for finding data objects (and
     * then to execute it) using the specified environment.
     *
     * <p>
     * Example use:
     * </p>
     *
     * <pre>
     * FindDataObjectsResponse fdor = DXSearch.findDataObjects(env).inProject(&quot;project-000000000000000000000000&quot;)
     *         .inFolder(&quot;/my/subfolder&quot;).execute();
     *
     * for (DXDataObject o : fdor.asList()) {
     *     System.out.println(o.getId());
     * }
     * </pre>
     */
    public static FindDataObjectsRequestBuilder findDataObjectsWithEnvironment(DXEnvironment env) {
        return new FindDataObjectsRequestBuilder(env);
    }

    /**
     * Returns a builder object that can be used to construct a query for finding jobs (and then to
     * execute it).
     *
     * <p>
     * Example use:
     * </p>
     *
     * <pre>
     * FindJobsResponse fjr = DXSearch.findJobs().launchedBy(&quot;user-dnanexus&quot;).inProject(&quot;project-000000000000000000000000&quot;)
     *         .createdBefore(new GregorianCalendar(2012, 11, 31).getTime()).execute();
     *
     * for (DXJob job : fjr.asList()) {
     *     System.out.println(job.getId());
     * }
     * </pre>
     */
    public static FindJobsRequestBuilder findJobs() {
        return new FindJobsRequestBuilder();
    }

    /**
     * Returns a builder object that can be used to construct a query for finding jobs (and then to
     * execute it) using the specified environment.
     *
     * <p>
     * Example use:
     * </p>
     *
     * <pre>
     * FindJobsResponse fjr = DXSearch.findJobs().launchedBy(&quot;user-dnanexus&quot;).inProject(&quot;project-000000000000000000000000&quot;)
     *         .createdBefore(new GregorianCalendar(2012, 11, 31).getTime()).execute();
     *
     * for (DXJob job : fjr.asList()) {
     *     System.out.println(job.getId());
     * }
     * </pre>
     */
    public static FindJobsRequestBuilder findJobsWithEnvironment(DXEnvironment env) {
        return new FindJobsRequestBuilder(env);
    }

    // Prevent this utility class from being instantiated.
    private DXSearch() {}

}
