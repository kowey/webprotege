package edu.stanford.bmir.protege.web.client.rpc.data;

import java.io.Serializable;

/**
 * @author Jennifer Vendetti <vendetti@stanford.edu>
 */
public class MetricData implements Serializable {
	private String metricName;
	private String metricValue;

	public String getMetricName() {
		return metricName;
	}

	public String getMetricValue() {
		return metricValue;
	}
}
