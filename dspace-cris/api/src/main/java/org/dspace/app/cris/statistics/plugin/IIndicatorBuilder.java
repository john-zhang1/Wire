package org.dspace.app.cris.statistics.plugin;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.solr.common.SolrDocument;
import org.dspace.app.cris.metrics.common.services.MetricsPersistenceService;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.browse.BrowsableDSpaceObject;
import org.dspace.core.Context;

public interface IIndicatorBuilder<ACO extends BrowsableDSpaceObject>
{
    public void computeMetric(Context context, ApplicationService applicationService,
            MetricsPersistenceService pService, Map<String, Integer> mapNumberOfValueComputed,
            Map<String, Double> mapValueComputed, Map<String, List<Double>> mapElementsValueComputed, 
            ACO aco, SolrDocument doc, Integer resourceType, UUID resourceId, String uuid) throws Exception;
    
    public boolean isPersistent();
    public List<String> getInputs();
    
    public String getOutput();
    public String getName();
    
    public void applyAdditional(Context context, ApplicationService applicationService,
            MetricsPersistenceService pService, 
            Map<String, Integer> mapNumberOfValueComputed,
            Map<String, Double> mapValueComputed, Map<String, Double> additionalValueComputed, Map<String, List<Double>> mapElementsValueComputed, 
            ACO aco, SolrDocument doc, Integer resourceType, UUID resourceId, String uuid);

    public List<String> getFields();
    public String getAdditionalField();
}
