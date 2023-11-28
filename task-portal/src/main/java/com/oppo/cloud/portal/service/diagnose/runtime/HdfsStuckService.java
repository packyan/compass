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

package com.oppo.cloud.portal.service.diagnose.runtime;

import com.alibaba.fastjson2.JSONObject;
import com.oppo.cloud.common.constant.AppCategoryEnum;
import com.oppo.cloud.common.domain.eventlog.*;
import com.oppo.cloud.common.domain.eventlog.config.DetectorConfig;
import com.oppo.cloud.common.util.ui.UIUtil;
import com.oppo.cloud.portal.domain.diagnose.Chart;
import com.oppo.cloud.portal.domain.diagnose.runtime.HdfsStuck;
import com.oppo.cloud.portal.domain.diagnose.runtime.base.MetricInfo;
import com.oppo.cloud.portal.domain.diagnose.runtime.base.ValueInfo;
import com.oppo.cloud.portal.util.MessageSourceUtil;
import com.oppo.cloud.portal.util.UnitUtil;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HdfsStuck Service
 */
@Service
public class HdfsStuckService extends RunTimeBaseService<HdfsStuck> {

    @Override
    public String getCategory() {
        return AppCategoryEnum.HDFS_STUCK.getCategory();
    }

    @Override
    public HdfsStuck generateData(DetectorResult detectorResult, DetectorConfig config) throws Exception {
        HdfsStuck hdfsStuck = new HdfsStuck();
        List<HdfsStuckAbnormal> hdfsStuckAbnormalList = new ArrayList<>();
        for (JSONObject data : (List<JSONObject>) detectorResult.getData()) {
            hdfsStuckAbnormalList.add(data.toJavaObject(HdfsStuckAbnormal.class));
        }
        hdfsStuck.setAbnormal(detectorResult.getAbnormal() != null && detectorResult.getAbnormal());
        List<Chart<MetricInfo>> chartList = hdfsStuck.getChartList();
        // Stage chart
        Chart<MetricInfo> chartSummary = new Chart<>();
        buildSummaryChartInfo(chartSummary);
        chartSummary.setDes(MessageSourceUtil.get("HDFS_STUCK_CHART_DESC"));
        List<MetricInfo> metricSummaryList = chartSummary.getDataList();
        List<String> info = new ArrayList<>();
        for (HdfsStuckAbnormal hdfsStuckAbnormal : hdfsStuckAbnormalList) {
            MetricInfo metricSummary = new MetricInfo();
            metricSummary.setXValue(String.valueOf(hdfsStuckAbnormal.getStageId()));
            List<ValueInfo> ySummaryValues = metricSummary.getYValues();
            if (hdfsStuckAbnormal.getAbnormal()) {
                ySummaryValues.add(new ValueInfo(UnitUtil.transferDouble(hdfsStuckAbnormal.getRatio()), "abnormal"));
            } else {
                ySummaryValues.add(new ValueInfo(UnitUtil.transferDouble(hdfsStuckAbnormal.getRatio()), "normal"));
            }
            metricSummaryList.add(metricSummary);
            // Task chart
            if (hdfsStuckAbnormal.getGraphs() != null) {
                chartList.add(buildTaskChart(hdfsStuckAbnormal, info));
            }
        }
        chartList.add(0, chartSummary);
        hdfsStuck.getVars().put("hdfsSlowInfo", String.join(",", info));
        hdfsStuck.getVars().put("threshold", String.format("%.2f", config.getHdfsStuckConfig().getThreshold()));
        return hdfsStuck;
    }

    @Override
    public String generateConclusionDesc(Map<String, String> thresholdMap) {
        return String.format(MessageSourceUtil.get("HDFS_STUCK_CONCLUSION_DESC"),
                thresholdMap.getOrDefault("threshold", "10"));
    }

    @Override
    public String generateItemDesc() {
        return MessageSourceUtil.get("HDFS_STUCK_ANALYSIS");
    }

    @Override
    public String getType() {
        return "chart";
    }

    /**
     * build chart information
     */
    private void buildChartInfo(Chart<MetricInfo> chart) {
        chart.setX("task id");
        chart.setY("inputSize/duration");
        chart.setUnit("MB/s");
        Map<String, Chart.ChartInfo> dataCategory = new HashMap<>(4);
        dataCategory.put("min", new Chart.ChartInfo(MessageSourceUtil.get("HDFS_STUCK_CHART_MIN"), UIUtil.ABNORMAL_COLOR));
        dataCategory.put("median", new Chart.ChartInfo(MessageSourceUtil.get("HDFS_STUCK_CHART_MEDIAN"), UIUtil.KEY_COLOR));
        dataCategory.put("normal", new Chart.ChartInfo(MessageSourceUtil.get("HDFS_STUCK_CHART_NORMAL"), UIUtil.NORMAL_COLOR));

        chart.setDataCategory(dataCategory);
    }

    /**
     * build task chart
     */
    private Chart<MetricInfo> buildTaskChart(HdfsStuckAbnormal hdfsStuckAbnormal, List<String> info) {
        Chart<MetricInfo> chart = new Chart<>();
        buildChartInfo(chart);

        chart.setDes(String.format(MessageSourceUtil.get("HDFS_STUCK_CHART_TASK_DESC"), hdfsStuckAbnormal.getJobId(),
                hdfsStuckAbnormal.getStageId(), chart.getUnit()));
        long taskId = 0;
        long jobId = 0;
        long stageId = 0;
        double min = 0;
        double median = 0;
        List<MetricInfo> metricInfoList = chart.getDataList();
        for (HdfsStuckGraph hdfsStuckGraph : hdfsStuckAbnormal.getGraphs()) {
            double value = UnitUtil.transferDouble(hdfsStuckGraph.getPercent());
            MetricInfo metricInfo = new MetricInfo();
            metricInfo.setXValue(String.valueOf(hdfsStuckGraph.getTaskId()));
            List<ValueInfo> yValues = metricInfo.getYValues();
            ValueInfo yValue = new ValueInfo();
            yValue.setValue(value);
            yValue.setType(hdfsStuckGraph.getGraphType());
            yValues.add(yValue);
            metricInfoList.add(metricInfo);
            if ("min".equals(hdfsStuckGraph.getGraphType())) {
                taskId = hdfsStuckGraph.getTaskId();
                stageId = hdfsStuckAbnormal.getStageId();
                jobId = hdfsStuckAbnormal.getJobId();
                min = hdfsStuckGraph.getPercent();
            }
            if ("median".equals(hdfsStuckGraph.getGraphType())) {
                median = hdfsStuckGraph.getPercent();
            }
        }
        info.add(String.format(MessageSourceUtil.get("HDFS_STUCK_CONCLUSION_INFO"), jobId, stageId, taskId, min, median));
        return chart;
    }

    /**
     * build summary chart information
     */
    private void buildSummaryChartInfo(Chart<MetricInfo> chart) {
        chart.setX("stage id");
        chart.setY("median/min");
        chart.setUnit("");
        Map<String, Chart.ChartInfo> dataCategory = new HashMap<>(2);
        dataCategory.put("normal", new Chart.ChartInfo(MessageSourceUtil.get("HDFS_STUCK_CHART_STAGE_NORMAL"), UIUtil.NORMAL_COLOR));
        dataCategory.put("abnormal", new Chart.ChartInfo(MessageSourceUtil.get("HDFS_STUCK_CHART_STAGE_ABNORMAL"), UIUtil.ABNORMAL_COLOR));

        chart.setDataCategory(dataCategory);
    }
}
