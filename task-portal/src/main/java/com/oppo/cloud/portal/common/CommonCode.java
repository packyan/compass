/*
 * Copyright 2023 OPPO.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.oppo.cloud.portal.common;

public class CommonCode {

    /**
     * cache prefix of one-click diagnosis to entity `taskApp`
     */
    public static final String TASK_APP_TEMP = ":taskApp:temp";

    /**
     * diagnosis cache running application state
     */
    public static final String TASK_APP_RUNNING = ":taskApp:running";

    /**
     * diagnose report-runInfo
     */
    public static final String DIAGNOSE_DETECTORSTORAGE = ":diagnose:detectorStorage";

    /**
     * diagnose report-runError
     */
    public static final String DIAGNOSE_RUNERROR = ":diagnose:runError";

    /**
     * graph type
     */
    public static final String CPU_TREND = "cpuTrend";

    public static final String MEMORY_TREND = "memoryTrend";

    public static final String NUM_TREND = "numTrend";
}