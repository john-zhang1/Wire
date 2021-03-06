package org.dspace.app.webui.servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.tools.ant.filters.StringInputStream;
import org.dspace.app.webui.util.DOIQueryConfigurator;
import org.dspace.app.webui.util.DoiFactoryUtils;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.IndexingService;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.event.Event;
import org.dspace.sort.SortException;
import org.dspace.sort.SortOption;
import org.dspace.utils.DSpace;
import org.hibernate.Session;
import org.xml.sax.InputSource;

public class DoiQueuedServlet extends DSpaceServlet
{
    /** log4j category */
    private static Logger log = Logger.getLogger(DoiQueuedServlet.class);

    public static String TABLE_NAME_DOI2ITEM = "doi2item";

    public static int REQUEST_DELETE = 0;

    public static int REQUEST_DELETE_AND_NODOI = 1;

    public static int REQUEST_NEWDOI = 2;

    public static int REQUEST_CURRENTDOI = 3;

    private static XPathFactory factory = XPathFactory.newInstance();

    DSpace dspace = new DSpace();

    SearchService searcher = dspace.getServiceManager().getServiceByName(
            SearchService.class.getName(), SearchService.class);

    IndexingService indexer = dspace.getServiceManager().getServiceByName(
            IndexingService.class.getName(), IndexingService.class);

    @Override
    protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        doDSGet(context, request, response);
    }

    @Override
    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        int submit = UIUtil.getIntParameter(request, "submit");
        DOIQueryConfigurator doiConfigurator = new DOIQueryConfigurator();
        int sortBy = doiConfigurator.getSortBy(request, "pending");
        String order = doiConfigurator.getOrder(request, "pending");
        String orderfield = sortBy != -1 ? "sort_" + sortBy : null;
        boolean ascending = SortOption.ASCENDING.equalsIgnoreCase(order);
        SortOption sortOption = null;
        if (sortBy > 0)
        {
            try
            {
                sortOption = SortOption.getSortOption(sortBy);
            }
            catch (SortException e)
            {
                log.error(e.getMessage(), e);
            }
        }
        request.setAttribute("order", order);
        request.setAttribute("sortedBy", sortOption);

        if (submit != -1)
        {
            List<UUID> items = UIUtil.getUUIDParameters(request, "pendingdoi");
            List<Item> realItems = DoiFactoryUtils.getItems(context, items);
            if (submit == REQUEST_DELETE)
            {

                try
                {
                    for (Item item : realItems)
                    {
                        deleteRequest(context, item);
                    }
                }
                catch (Exception e)
                {
                    log.error(e.getMessage(), new RuntimeException(e));
                }

            }
            else if (submit == REQUEST_DELETE_AND_NODOI)
            {

                try
                {
                    for (Item item : realItems)
                    {
                        deleteRequestAndNoDoi(context, item);
                    }
                }
                catch (Exception e)
                {
                    log.error(e.getMessage(), new RuntimeException(e));
                }
            }
            else if (submit == REQUEST_NEWDOI)
            {

                try
                {
                    for (Item item : realItems)
                    {
                        String customdoi = request
                                .getParameter("custombuilddoi_" + item.getID());
                        fix(context, item, customdoi);
                    }
                }
                catch (Exception e)
                {
                    log.error(e.getMessage(), new RuntimeException(e));
                }
            }
            else if (submit == REQUEST_CURRENTDOI)
            {

                try
                {
                    for (Item item : realItems)
                    {
                        resolve(context, item);
                    }
                }
                catch (Exception e)
                {
                    log.error(e.getMessage(), new RuntimeException(e));
                }
            }

            context.commit();

        }

        try
        {
            indexer.commit();
        }
        catch (SearchServiceException e)
        {
            log.error(e.getMessage(), new RuntimeException(e));
        }

        QueryResponse rsp = searchPending(orderfield, ascending);

        List<Item> results = DoiFactoryUtils.getItemsFromSolrResult(
                rsp.getResults(), context);
        Item[] realresult = null;
        Map<UUID, List<String>> doi2items = new HashMap<UUID, List<String>>();
        if (results != null && !results.isEmpty())
        {
            realresult = results.toArray(new Item[results.size()]);

            for (Item real : realresult)
            {
            	List<Object[]> rows = getHibernateSession(context).createSQLQuery(
            			"SELECT identifier_doi, note FROM "
                                + DoiFactoryUtils.TABLE_NAME
                                + " where item_id = :par1")
                                .setParameter(0,real.getID()).list();
                
                List<String> rr = new ArrayList<String>();
                for(Object[] row : rows) {
                	rr.add((String)row[0]);
                	String note = (String)row[1];
                	String resultNote = note == null || note.isEmpty() ? ""
                			: extractMessage(note)[1];
                	String typeNote = note == null || note.isEmpty() ? ""
                        : extractMessage(note)[0];
                	rr.add(resultNote);
                	rr.add(typeNote);
                	doi2items.put(real.getID(), rr);
                }
            }
        }

        request.setAttribute("doi2items", doi2items);
        request.setAttribute("results", realresult);
        request.setAttribute("prefixDOI", DoiFactoryUtils.PREFIX_DOI);
        JSPManager.showJSP(request, response, "/doi/checkerDoiQueued.jsp");

    }

    private void fix(Context context, Item item, String customdoi)
            throws SQLException, AuthorizeException
    {
    	getHibernateSession(context).createSQLQuery(
                        "UPDATE "
                                + TABLE_NAME_DOI2ITEM
                                + " SET LAST_MODIFIED = CURRENT_TIMESTAMP, IDENTIFIER_DOI = :par0, RESPONSE_CODE = :par1 WHERE ITEM_ID = :par2")
                                .setParameter(0,customdoi).setParameter(1,
                                        DoiFactoryUtils.CODE_END_CROSSREF_FLOW).setParameter(2, item.getID()).executeUpdate();

        item.getItemService().clearMetadata(context, item, "dc", "utils", "processdoi", Item.ANY);
        item.getItemService().clearMetadata(context, item, "dc", "identifier", "doi", Item.ANY);
        item.getItemService().addMetadata(context, item, "dc", "identifier", "doi", null, customdoi);
        item.getItemService().update(context, item);
        
        context.addEvent(new Event(Event.UPDATE_FORCE, Constants.ITEM, item
                .getID(), item.getHandle()));
    }

    private void resolve(Context context, Item item) throws SQLException,
            AuthorizeException
    {
        getHibernateSession(context).createSQLQuery(
                        "UPDATE "
                                + TABLE_NAME_DOI2ITEM
                                + " SET LAST_MODIFIED = CURRENT_TIMESTAMP, RESPONSE_CODE = :par0 WHERE ITEM_ID = :par1").setParameter(0,
                        DoiFactoryUtils.CODE_END_CROSSREF_FLOW).setParameter(1, item.getID()).executeUpdate();

        item.getItemService().clearMetadata(context, item, "dc", "utils", "processdoi", Item.ANY);
        item.getItemService().clearMetadata(context, item, "dc", "identifier", "doi", Item.ANY);
        item.getItemService().addMetadata(context, item, "dc", "identifier", "doi", null,
                DoiFactoryUtils.getDoiFromDoi2Item(context, item.getID()));
        item.getItemService().update(context, item);
        context.addEvent(new Event(Event.UPDATE_FORCE, Constants.ITEM, item
                .getID(), item.getHandle()));

    }

    private void deleteRequestAndNoDoi(Context context, Item i)
            throws SQLException, AuthorizeException
    {
        deleteRequest(context, i);
        i.getItemService().addMetadata(context, i, "dc", "utils", "nodoi", null, "true");
        i.getItemService().update(context, i);
        context.addEvent(new Event(Event.UPDATE_FORCE, Constants.ITEM, i
                .getID(), i.getHandle()));
    }

    private void deleteRequest(Context context, Item i) throws SQLException,
            AuthorizeException
    {
        getHibernateSession(context).createSQLQuery(
                "DELETE FROM "
                        + DoiFactoryUtils.TABLE_NAME
                        + " WHERE ITEM_ID = :par1").setParameter(0,i.getID()).executeUpdate();
        i.getItemService().clearMetadata(context, i, "dc", "utils", "processdoi", Item.ANY);
        i.getItemService().update(context, i);
        context.addEvent(new Event(Event.UPDATE_FORCE, Constants.ITEM, i
                .getID(), i.getHandle()));
    }

    private String[] extractMessage(String note)
    {
        XPathExpression xpathexp = null;
        String resultSuccess = "";
        String typeSuccess = "Success";
        try
        {
            xpathexp = factory.newXPath().compile(
                    "//record_diagnostic[@status='Warning']");
        }
        catch (XPathExpressionException e)
        {
            log.error(e.getMessage(), e);
        }

        try
        {
            InputSource source = new InputSource(new StringInputStream(note));
            resultSuccess = xpathexp.evaluate(source);
            if (!resultSuccess.isEmpty())
            {
                typeSuccess = "Warning";
            }
        }
        catch (XPathExpressionException e)
        {
            log.error(e.getMessage(), e);
        }

        if (resultSuccess == null || resultSuccess.isEmpty())
        {
            try
            {
                xpathexp = factory.newXPath().compile(
                        "//record_diagnostic[@status='Failure']");
            }
            catch (XPathExpressionException e)
            {
                log.error(e.getMessage(), e);
            }

            try
            {
                InputSource source = new InputSource(
                        new StringInputStream(note));
                resultSuccess = xpathexp.evaluate(source);
                if (!resultSuccess.isEmpty())
                {
                    typeSuccess = "Failure";
                }
            }
            catch (XPathExpressionException e)
            {
                log.error(e.getMessage(), e);
            }

        }

        if (xpathexp == null || resultSuccess == null
                || resultSuccess.isEmpty())
        {
            resultSuccess = note;
        }

        return new String[] { typeSuccess, resultSuccess };
    }

    private QueryResponse searchPending(String orderfield, boolean ascending)
    {
        String query = ConfigurationManager
                .getProperty("doi.pending.infoquery");

        QueryResponse rsp = null;
        try
        {
            SolrQuery solrQuery = new SolrQuery();
            solrQuery.setQuery(query);
            solrQuery.setRows(Integer.MAX_VALUE);
            if (orderfield == null)
            {
                orderfield = "score";
            }
            solrQuery.addSortField(orderfield, ascending ? SolrQuery.ORDER.asc
                    : SolrQuery.ORDER.desc);
            rsp = searcher.search(solrQuery);

        }
        catch (SearchServiceException e)
        {
            log.error(e.getMessage(), e);
        }
        return rsp;
    }

    protected Session getHibernateSession(Context context) throws SQLException {
        return ((Session) context.getDBConnection().getSession());
    }
}
