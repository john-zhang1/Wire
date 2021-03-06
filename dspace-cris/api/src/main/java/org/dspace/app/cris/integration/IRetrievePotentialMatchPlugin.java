/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.content.Item;
import org.dspace.core.Context;

public interface IRetrievePotentialMatchPlugin
{
    Set<UUID> retrieve(Context context, Set<UUID> invalidIds, ResearcherPage rp);
    
    Map<NameResearcherPage, List<Item>> retrieveGroupByName(Context context, Map<String, Set<UUID>> mapInvalids, List<ResearcherPage> rps);
}
