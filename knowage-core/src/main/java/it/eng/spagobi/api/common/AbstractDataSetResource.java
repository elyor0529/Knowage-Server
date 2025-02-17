/*
 * Knowage, Open Source Business Intelligence suite
 * Copyright (C) 2016 Engineering Ingegneria Informatica S.p.A.

 * Knowage is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * Knowage is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.eng.spagobi.api.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;

import it.eng.qbe.dataset.QbeDataSet;
import it.eng.spago.error.EMFInternalError;
import it.eng.spago.security.IEngUserProfile;
import it.eng.spagobi.analiticalmodel.execution.service.ExecuteAdHocUtility;
import it.eng.spagobi.api.AbstractSpagoBIResource;
import it.eng.spagobi.commons.SingletonConfig;
import it.eng.spagobi.commons.bo.UserProfile;
import it.eng.spagobi.commons.constants.SpagoBIConstants;
import it.eng.spagobi.commons.dao.DAOFactory;
import it.eng.spagobi.commons.serializer.SerializerFactory;
import it.eng.spagobi.commons.utilities.StringUtilities;
import it.eng.spagobi.engines.config.bo.Engine;
import it.eng.spagobi.sdk.datasets.bo.SDKDataSetParameter;
import it.eng.spagobi.tools.dataset.DatasetManagementAPI;
import it.eng.spagobi.tools.dataset.bo.DatasetEvaluationStrategyType;
import it.eng.spagobi.tools.dataset.bo.IDataSet;
import it.eng.spagobi.tools.dataset.bo.SolrDataSet;
import it.eng.spagobi.tools.dataset.bo.VersionedDataSet;
import it.eng.spagobi.tools.dataset.common.datastore.IDataStore;
import it.eng.spagobi.tools.dataset.common.datawriter.IDataWriter;
import it.eng.spagobi.tools.dataset.common.datawriter.JSONDataWriter;
import it.eng.spagobi.tools.dataset.common.query.AggregationFunctions;
import it.eng.spagobi.tools.dataset.common.query.IAggregationFunction;
import it.eng.spagobi.tools.dataset.constants.DataSetConstants;
import it.eng.spagobi.tools.dataset.dao.DataSetFactory;
import it.eng.spagobi.tools.dataset.dao.IDataSetDAO;
import it.eng.spagobi.tools.dataset.exceptions.DatasetInUseException;
import it.eng.spagobi.tools.dataset.exceptions.ParametersNotValorizedException;
import it.eng.spagobi.tools.dataset.metasql.query.item.CoupledProjection;
import it.eng.spagobi.tools.dataset.metasql.query.item.Filter;
import it.eng.spagobi.tools.dataset.metasql.query.item.InFilter;
import it.eng.spagobi.tools.dataset.metasql.query.item.LikeFilter;
import it.eng.spagobi.tools.dataset.metasql.query.item.MultipleProjectionSimpleFilter;
import it.eng.spagobi.tools.dataset.metasql.query.item.Projection;
import it.eng.spagobi.tools.dataset.metasql.query.item.SimpleFilter;
import it.eng.spagobi.tools.dataset.metasql.query.item.Sorting;
import it.eng.spagobi.tools.dataset.metasql.query.item.UnsatisfiedFilter;
import it.eng.spagobi.tools.dataset.utils.DataSetUtilities;
import it.eng.spagobi.tools.dataset.utils.datamart.SpagoBICoreDatamartRetriever;
import it.eng.spagobi.user.UserProfileManager;
import it.eng.spagobi.utilities.assertion.Assert;
import it.eng.spagobi.utilities.exceptions.SpagoBIRuntimeException;
import it.eng.spagobi.utilities.exceptions.SpagoBIServiceException;

public abstract class AbstractDataSetResource extends AbstractSpagoBIResource {

	static protected Logger logger = Logger.getLogger(AbstractDataSetResource.class);
	private static final int SOLR_FACETS_DEFAULT_LIMIT = 10;

	// ===================================================================
	// UTILITY METHODS
	// ===================================================================

	protected DatasetManagementAPI getDatasetManagementAPI() {
		DatasetManagementAPI managementAPI = new DatasetManagementAPI(getUserProfile());
		return managementAPI;
	}

	protected Map<String, Object> getDataSetWriterProperties() throws JSONException {
		Map<String, Object> properties = new HashMap<String, Object>();
		JSONArray fieldOptions = new JSONArray("[{id: 1, options: {measureScaleFactor: 0.5}}]");
		properties.put(JSONDataWriter.PROPERTY_FIELD_OPTION, fieldOptions);
		return properties;
	}

	protected Integer[] getIdsAsIntegers(String ids) {
		Integer[] idArray = null;
		if (ids != null && !ids.isEmpty()) {
			String[] split = ids.split(",");
			idArray = new Integer[split.length];
			for (int i = 0; i < split.length; i++) {
				idArray[i] = Integer.parseInt(split[i]);
			}
		}
		return idArray;
	}

	public String getDataStore(String label, String parameters, Map<String, Object> drivers, String selections, String likeSelections, int maxRowCount,
			String aggregations, String summaryRow, int offset, int fetchSize, boolean isNearRealtime, Set<String> indexes) {
		return getDataStore(label, parameters, drivers, selections, likeSelections, maxRowCount, aggregations, summaryRow, offset, fetchSize, isNearRealtime,
				null, indexes);
	}

	public String getDataStore(String label, String parameters, Map<String, Object> drivers, String selections, String likeSelections, int maxRowCount,
			String aggregations, String summaryRow, int offset, int fetchSize, boolean isNearRealtime, String options, Set<String> indexes) {
		logger.debug("IN");
		Monitor totalTiming = MonitorFactory.start("Knowage.AbstractDataSetResource.getDataStore");
		try {
			Monitor timing = MonitorFactory.start("Knowage.AbstractDataSetResource.getDataStore:validateParams");

			int maxResults = Integer.parseInt(SingletonConfig.getInstance().getConfigValue("SPAGOBI.API.DATASET.MAX_ROWS_NUMBER"));
			logger.debug("Offset [" + offset + "], fetch size [" + fetchSize + "], max results[" + maxResults + "]");

			if (maxResults <= 0) {
				throw new SpagoBIRuntimeException("SPAGOBI.API.DATASET.MAX_ROWS_NUMBER value cannot be a non-positive integer");
			}
			if (offset < 0) {
				logger.debug("Offset size is not valid. Setting it to [0] by default.");
				offset = 0;
			}
			if (fetchSize < -1) {
				logger.debug("Fetch size is not valid. Setting it to [-1] by default.");
				fetchSize = -1;
			}
			if (fetchSize > maxResults) {
				throw new IllegalArgumentException("The page requested is too big. Max page size is equals to [" + maxResults + "]");
			}
			if (maxRowCount > maxResults) {
				throw new IllegalArgumentException("The dataset requested is too big. Max row count is equals to [" + maxResults + "]");
			}

			IDataSetDAO dataSetDao = DAOFactory.getDataSetDAO();
			dataSetDao.setUserProfile(getUserProfile());
			IDataSet dataSet = dataSetDao.loadDataSetByLabel(label);
			Assert.assertNotNull(dataSet, "Unable to load dataset with label [" + label + "]");
			dataSet.setUserProfile(getUserProfile());
			dataSet.setDrivers(drivers);

			timing.stop();
			timing = MonitorFactory.start("Knowage.AbstractDataSetResource.getDataStore:getQueryDetails");

			List<Projection> projections = new ArrayList<Projection>(0);
			List<Projection> groups = new ArrayList<Projection>(0);
			List<Sorting> sortings = new ArrayList<Sorting>(0);
			List<Projection> summaryRowProjections = new ArrayList<Projection>(0);
			Map<String, String> columnAliasToName = new HashMap<String, String>();
			if (aggregations != null && !aggregations.isEmpty()) {
				JSONObject aggregationsObject = new JSONObject(aggregations);
				JSONArray categoriesObject = aggregationsObject.getJSONArray("categories");
				JSONArray measuresObject = aggregationsObject.getJSONArray("measures");

				loadColumnAliasToName(categoriesObject, columnAliasToName);
				loadColumnAliasToName(measuresObject, columnAliasToName);

				Map<String, Object> optionMap;
				if (options != null && !options.isEmpty()) {
					ObjectMapper objectMapper = new ObjectMapper();
					optionMap = new HashMap<>((Map<? extends String, ?>) objectMapper.readValue(options, new TypeReference<Map<String, Object>>() {
					}));
				} else {
					optionMap = new HashMap<>();
				}
				applyOptions(dataSet, optionMap);

				projections.addAll(getProjections(dataSet, categoriesObject, measuresObject, columnAliasToName));
				groups.addAll(getGroups(dataSet, categoriesObject, measuresObject, columnAliasToName, hasSolrFacetPivotOption(dataSet, optionMap)));
				sortings.addAll(getSortings(dataSet, categoriesObject, measuresObject, columnAliasToName));

				if (isSolrDataset(dataSet)) {
					IDataSet dataSetCopy = null;
					if (dataSet instanceof VersionedDataSet) {
						dataSetCopy = ((VersionedDataSet) dataSet).getWrappedDataset();
					}
					SolrDataSet solrDS = (SolrDataSet) dataSetCopy;
					solrDS.setFacetsLimitOption(getSolrFacetLimitOption(optionMap));
					((VersionedDataSet) dataSet).setWrappedDataset(solrDS);
				}

//				if (summaryRow != null && !summaryRow.isEmpty()) {
//					JSONObject summaryRowObject = new JSONObject(summaryRow);
//					JSONArray summaryRowMeasuresObject = summaryRowObject.getJSONArray("measures");
//					summaryRowProjections.addAll(getProjections(dataSet, new JSONArray(), summaryRowMeasuresObject, columnAliasToName));
//				}
			}

			List<Filter> filters = new ArrayList<>(0);
			if (selections != null && !selections.isEmpty()) {
				JSONObject selectionsObject = new JSONObject(selections);
				if (selectionsObject.names() != null) {
					filters.addAll(getFilters(label, selectionsObject, columnAliasToName));
				}
			}

			List<SimpleFilter> likeFilters = new ArrayList<>(0);
			if (likeSelections != null && !likeSelections.equals("")) {
				JSONObject likeSelectionsObject = new JSONObject(likeSelections);
				if (likeSelectionsObject.names() != null) {
					likeFilters.addAll(getLikeFilters(label, likeSelectionsObject, columnAliasToName));
				}
			}

			Monitor timingMinMax = MonitorFactory.start("Knowage.AbstractDataSetResource.getDataStore:calculateMinMax");
			filters = getDatasetManagementAPI().calculateMinMaxFilters(dataSet, isNearRealtime, DataSetUtilities.getParametersMap(parameters), filters,
					likeFilters, indexes);
			timingMinMax.stop();

			Filter where = getDatasetManagementAPI().getWhereFilter(filters, likeFilters);

			timing.stop();

			List<List<Projection>> summaryRowArray = getSummaryRowArray(summaryRow, dataSet, columnAliasToName);

			IDataStore dataStore = getDatasetManagementAPI().getDataStore(dataSet, isNearRealtime, DataSetUtilities.getParametersMap(parameters), projections,
					where, groups, sortings, summaryRowArray, offset, fetchSize, maxRowCount, indexes);
			IDataWriter dataWriter = getDataStoreWriter();

			timing = MonitorFactory.start("Knowage.AbstractDataSetResource.getDataStore:convertToJson");
			Object gridDataFeed = dataWriter.write(dataStore);
			timing.stop();

			String stringFeed = gridDataFeed.toString();
			return stringFeed;
		} catch (ParametersNotValorizedException p) {
			throw p;
		} catch (Throwable t) {
			throw new SpagoBIServiceException(this.request.getPathInfo(), "An unexpected error occured while executing service", t);
		} finally {
			totalTiming.stop();
			logger.debug("OUT");
		}
	}

	@SuppressWarnings("unused")
	private List<List<Projection>> getSummaryRowArray(String summaryJson, IDataSet dataSet, Map<String, String> columnAliasToName) throws JSONException {
		if (summaryJson != null && !summaryJson.isEmpty()) {

			List<List<Projection>> returnList = new ArrayList<List<Projection>>();
			JSONArray summaryRowObject = new JSONArray(summaryJson);

			if(summaryRowObject != null && summaryRowObject.length()==1) {

				  // old way

				JSONObject summaryRowObjectArray = summaryRowObject.getJSONObject(0);
				JSONArray summaryRowMeasuresObject = summaryRowObjectArray.getJSONArray("measures");

				List<Projection> summaryRowProjections = new ArrayList<Projection>(0);

				summaryRowProjections.addAll(getProjections(dataSet, new JSONArray(), summaryRowMeasuresObject, columnAliasToName));

				returnList.add(summaryRowProjections);

				return returnList;

			}
			else {		 // new way


				for (int i = 0; i < summaryRowObject.length(); i++) {

					JSONObject jsonObj = summaryRowObject.getJSONObject(i);

					JSONArray summaryRowMeasuresObject = jsonObj.getJSONArray("measures");

					List<Projection> summaryRowProjections = new ArrayList<Projection>(0);

					summaryRowProjections.addAll(getProjections(dataSet, new JSONArray(), summaryRowMeasuresObject, columnAliasToName));

					returnList.add(summaryRowProjections);

				}
				return returnList;
			}
		}
		return null;
	}

	private void applyOptions(IDataSet dataSet, Map<String, Object> options) {
		if (hasSolrFacetPivotOption(dataSet, options)) {
			applySolrFacetPivotOption(dataSet);
		}
	}

	private void applySolrFacetPivotOption(IDataSet dataSet) {
		if (dataSet instanceof VersionedDataSet) {
			VersionedDataSet versionedDataSet = (VersionedDataSet) dataSet;
			dataSet = versionedDataSet.getWrappedDataset();
		}
		SolrDataSet solrDataSet = (SolrDataSet) dataSet;
		solrDataSet.setEvaluationStrategy(DatasetEvaluationStrategyType.SOLR_FACET_PIVOT);
	}

	private boolean hasSolrFacetPivotOption(IDataSet dataSet, Map<String, Object> options) {
		return isSolrDataset(dataSet) && Boolean.TRUE.equals(options.get("solrFacetPivot"));
	}

	private int getSolrFacetLimitOption(Map<String, Object> options) {
		if (options.get("facetsLimit") != null) {

			return Integer.parseInt(options.get("facetsLimit").toString());
		}
		return SOLR_FACETS_DEFAULT_LIMIT;
	}

	private boolean isSolrDataset(IDataSet dataSet) {
		if (dataSet instanceof VersionedDataSet) {
			dataSet = ((VersionedDataSet) dataSet).getWrappedDataset();
		}
		return dataSet instanceof SolrDataSet;
	}

	protected List<Projection> getProjectionsSummary(IDataSet dataSet, JSONArray categories, JSONArray measures, Map<String, String> columnAliasToName)
			throws JSONException {
		ArrayList<Projection> projections = new ArrayList<Projection>(categories.length() + measures.length());

		for (int i = 0; i < categories.length(); i++) {
			JSONObject category = categories.getJSONObject(i);
			addProjection(dataSet, projections, category, columnAliasToName);

		}

		for (int i = 0; i < measures.length(); i++) {
			JSONObject measure = measures.getJSONObject(i);
			addProjection(dataSet, projections, measure, columnAliasToName);

		}

		return projections;
	}

	protected List<Projection> getProjections(IDataSet dataSet, JSONArray categories, JSONArray measures, Map<String, String> columnAliasToName)
			throws JSONException {
		ArrayList<Projection> projections = new ArrayList<>(categories.length() + measures.length());
		addProjections(dataSet, categories, columnAliasToName, projections);
		addProjections(dataSet, measures, columnAliasToName, projections);
		return projections;
	}

	private void addProjections(IDataSet dataSet, JSONArray categories, Map<String, String> columnAliasToName, ArrayList<Projection> projections)
			throws JSONException {
		for (int i = 0; i < categories.length(); i++) {
			JSONObject category = categories.getJSONObject(i);
			addProjection(dataSet, projections, category, columnAliasToName);
		}
	}

	private void addProjection(IDataSet dataSet, ArrayList<Projection> projections, JSONObject catOrMeasure, Map<String, String> columnAliasToName)
			throws JSONException {

		String functionObj = catOrMeasure.optString("funct");
		// check if it is an array
		if (functionObj.startsWith("[")) {
			// call for each aggregation function
			JSONArray functs = new JSONArray(functionObj);
			for (int j = 0; j < functs.length(); j++) {
				String functName = functs.getString(j);
				Projection projection = getProjectionWithFunct(dataSet, catOrMeasure, columnAliasToName, functName);
				projections.add(projection);
			}
		} else {
			// only one aggregation function
			Projection projection = getProjection(dataSet, catOrMeasure, columnAliasToName);
			projections.add(projection);
		}

	}

	private Projection getProjectionWithFunct(IDataSet dataSet, JSONObject jsonObject, Map<String, String> columnAliasToName, String functName)
			throws JSONException {
		String columnName = getColumnName(jsonObject, columnAliasToName);
		String columnAlias = getColumnAlias(jsonObject, columnAliasToName);
		IAggregationFunction function = AggregationFunctions.get(functName);
		String functionColumnName = jsonObject.optString("functColumn");
		Projection projection;
		if (jsonObject.has("datasetOrTableFlag") && !jsonObject.getBoolean("datasetOrTableFlag"))
			function = AggregationFunctions.get("NONE");
		if (!function.equals(AggregationFunctions.COUNT_FUNCTION) && functionColumnName != null && !functionColumnName.isEmpty()) {
			Projection aggregatedProjection = new Projection(dataSet, functionColumnName);
			projection = new CoupledProjection(function, aggregatedProjection, dataSet, columnName, columnAlias);
		} else {
			projection = new Projection(function, dataSet, columnName, columnAlias);
		}
		return projection;
	}

	private Projection getProjection(IDataSet dataSet, JSONObject jsonObject, Map<String, String> columnAliasToName) throws JSONException {
		return getProjectionWithFunct(dataSet, jsonObject, columnAliasToName, jsonObject.optString("funct")); // caso in cui ci siano facets complesse (coupled
		// proj)
	}

	private String getColumnName(JSONObject jsonObject, Map<String, String> columnAliasToName) throws JSONException {
		if (jsonObject.isNull("id") && jsonObject.isNull("columnName")) {
			return getColumnAlias(jsonObject, columnAliasToName);
		} else {

			if (jsonObject.has("datasetOrTableFlag")) {
				// it is a calculated field
				return jsonObject.getString("columnName");
			}

			String id = jsonObject.getString("id");
			boolean isIdMatching = columnAliasToName.containsKey(id) || columnAliasToName.containsValue(id);

			String columnName = jsonObject.getString("columnName");
			boolean isColumnNameMatching = columnAliasToName.containsKey(columnName) || columnAliasToName.containsValue(columnName);

			Assert.assertTrue(isIdMatching || isColumnNameMatching, "Column name [" + columnName + "] not found in dataset metadata");
			return isColumnNameMatching ? columnName : id;
		}
	}

	private String getColumnAlias(JSONObject jsonObject, Map<String, String> columnAliasToName) throws JSONException {
		String columnAlias = jsonObject.getString("alias");
		Assert.assertTrue(columnAliasToName.containsKey(columnAlias) || columnAliasToName.containsValue(columnAlias),
				"Column alias [" + columnAlias + "] not found in dataset metadata");
		return columnAlias;
	}

	protected List<Projection> getGroups(IDataSet dataSet, JSONArray categories, JSONArray measures, Map<String, String> columnAliasToName, boolean forceGroups)
			throws JSONException {
		ArrayList<Projection> groups = new ArrayList<>(0);

		// hasAggregationInCategory se categoria di aggregazione del for ha una funzione di aggregazione

		boolean hasAggregatedMeasures = hasAggregations(measures);

		for (int i = 0; i < categories.length(); i++) {
			JSONObject category = categories.getJSONObject(i);
			String functionName = category.optString("funct");
			if (forceGroups || hasAggregatedMeasures || hasAggregationInCategory(category) || hasCountAggregation(functionName)) {
				Projection projection = getProjection(dataSet, category, columnAliasToName);
				groups.add(projection);
			}
		}

		if (forceGroups) {
			for (int i = 0; i < measures.length(); i++) {
				JSONObject measure = measures.getJSONObject(i);
				String functionName = measure.optString("funct");
				if (hasNoneAggregation(functionName)) {
					Projection projection = getProjection(dataSet, measure, columnAliasToName);
					groups.add(projection);
				}
			}
		}

		return groups;
	}

	private boolean hasCountAggregation(String functionName) { // caso in cui arrivano facets semplici
		return AggregationFunctions.get(functionName).getName().equals(AggregationFunctions.COUNT)
				|| AggregationFunctions.get(functionName).getName().equals(AggregationFunctions.COUNT_DISTINCT);
	}

	private boolean hasNoneAggregation(String functionName) {
		return AggregationFunctions.get(functionName).getName().equals(AggregationFunctions.NONE);
	}

	private boolean hasAggregations(JSONArray fields) throws JSONException {
		for (int i = 0; i < fields.length(); i++) {
			JSONObject field = fields.getJSONObject(i);
			String functionName = field.optString("funct");
			if (!AggregationFunctions.get(functionName).getName().equals(AggregationFunctions.NONE)) {
				return true;
			}
		}
		return false;
	}

	private boolean hasAggregationInCategory(JSONObject field) throws JSONException {
		String functionName = field.optString("funct");
		if (!AggregationFunctions.get(functionName).getName().equals(AggregationFunctions.NONE)) {
			return true;
		}

		return false;
	}

	protected List<Sorting> getSortings(IDataSet dataSet, JSONArray categories, JSONArray measures, Map<String, String> columnAliasToName)
			throws JSONException {
		ArrayList<Sorting> sortings = new ArrayList<Sorting>(0);

		for (int i = 0; i < categories.length(); i++) {
			JSONObject categoryObject = categories.getJSONObject(i);
			Sorting sorting = getSorting(dataSet, categoryObject, columnAliasToName);
			if (sorting != null) {
				sortings.add(sorting);
			}
		}

		for (int i = 0; i < measures.length(); i++) {
			JSONObject measure = measures.getJSONObject(i);
			Sorting sorting = getSorting(dataSet, measure, columnAliasToName);
			if (sorting != null) {
				sortings.add(sorting);
			}
		}

		return sortings;
	}

	private Sorting getSorting(IDataSet dataSet, JSONObject jsonObject, Map<String, String> columnAliasToName) throws JSONException {
		Sorting sorting = null;

		String orderType = (String) jsonObject.opt("orderType");
		if (orderType != null && !orderType.isEmpty() && ("ASC".equalsIgnoreCase(orderType) || "DESC".equalsIgnoreCase(orderType))) {
			IAggregationFunction function = AggregationFunctions.get(jsonObject.optString("funct"));
			String orderColumn = (String) jsonObject.opt("orderColumn");

			Projection projection;
			if (orderColumn != null && !orderColumn.isEmpty() && !orderType.isEmpty()) {
				String alias = jsonObject.optString("alias");
				projection = new Projection(function, dataSet, orderColumn, alias);
			} else {
				String columnName = getColumnName(jsonObject, columnAliasToName);
				projection = new Projection(function, dataSet, columnName, orderColumn);
			}

			boolean isAscending = "ASC".equalsIgnoreCase(orderType);

			sorting = new Sorting(projection, isAscending);
		}

		return sorting;
	}

	protected List<Filter> getFilters(String datasetLabel, JSONObject selectionsObject, Map<String, String> columnAliasToColumnName) throws JSONException {
		List<Filter> filters = new ArrayList<>(0);

		if (selectionsObject.has(datasetLabel)) {
			JSONObject datasetSelectionObject = selectionsObject.getJSONObject(datasetLabel);
			Iterator<String> it = datasetSelectionObject.keys();

			IDataSet dataSet = getDataSetDAO().loadDataSetByLabel(datasetLabel);

			boolean isAnEmptySelection = false;

			while (!isAnEmptySelection && it.hasNext()) {
				String columnsString = it.next();

				JSONArray valuesJsonArray = datasetSelectionObject.getJSONArray(columnsString);
				if (valuesJsonArray.length() == 0) {
					isAnEmptySelection = true;
					break;
				}

				List<String> columnsList = getColumnList(columnsString, dataSet, columnAliasToColumnName);
				List<Projection> projections = new ArrayList<>(columnsList.size());
				for (String columnName : columnsList) {
					projections.add(new Projection(dataSet, columnName));
				}

				List<Object> valueObjects = new ArrayList<>(0);
				for (int i = 0; i < valuesJsonArray.length(); i++) {
					String[] valuesArray = StringUtilities.splitBetween(valuesJsonArray.getString(i), "'", "','", "'");
					for (int j = 0; j < valuesArray.length; j++) {
						Projection projection = projections.get(j % projections.size());
						valueObjects.add(DataSetUtilities.getValue(valuesArray[j], projection.getType()));
					}
				}

				MultipleProjectionSimpleFilter inFilter = new InFilter(projections, valueObjects);
				filters.add(inFilter);
			}

			if (isAnEmptySelection) {
				filters.clear();
				filters.add(new UnsatisfiedFilter());
			}
		}

		return filters;
	}

	protected List<SimpleFilter> getLikeFilters(String datasetLabel, JSONObject likeSelectionsObject, Map<String, String> columnAliasToColumnName)
			throws JSONException {
		List<SimpleFilter> likeFilters = new ArrayList<>(0);

		if (likeSelectionsObject.has(datasetLabel)) {
			IDataSet dataSet = getDataSetDAO().loadDataSetByLabel(datasetLabel);
			boolean isAnEmptySelection = false;

			JSONObject datasetSelectionObject = likeSelectionsObject.getJSONObject(datasetLabel);
			Iterator<String> it = datasetSelectionObject.keys();
			while (!isAnEmptySelection && it.hasNext()) {
				String columns = it.next();
				String value = datasetSelectionObject.getString(columns);
				if (value == null || value.isEmpty()) {
					isAnEmptySelection = true;
					break;
				}

				List<String> columnsList = getColumnList(columns, dataSet, columnAliasToColumnName);
				List<Projection> projections = new ArrayList<>(columnsList.size());
				for (String columnName : columnsList) {
					projections.add(new Projection(dataSet, columnName));
				}

				for (Projection projection : projections) {
					SimpleFilter filter = new LikeFilter(projection, value, LikeFilter.TYPE.SIMPLE);
					likeFilters.add(filter);
				}
			}

			if (isAnEmptySelection) {
				likeFilters.clear();
				likeFilters.add(new UnsatisfiedFilter());
			}
		}

		return likeFilters;
	}

	protected List<String> getColumnList(String columns, IDataSet dataSet, Map<String, String> columnAliasToColumnName) {
		List<String> columnList = new ArrayList<>(Arrays.asList(columns.trim().split("\\s*,\\s*"))); // trim spaces while splitting

		// transform QBE columns
		for (int i = 0; i < columnList.size(); i++) {
			String column = columnList.get(i);
			if (column.contains(":")) {
				QbeDataSet qbeDataSet = (QbeDataSet) dataSet;
				columnList.set(i, qbeDataSet.getColumn(column));
			}
		}

		// transform aliases
		if (columnAliasToColumnName != null) {
			Set<String> aliases = columnAliasToColumnName.keySet();
			if (aliases.size() > 0) {
				for (int i = 0; i < columnList.size(); i++) {
					String column = columnList.get(i);
					if (aliases.contains(column)) {
						columnList.set(i, columnAliasToColumnName.get(column));
					}
				}
			}
		}

		return columnList;
	}

	protected IDataWriter getDataStoreWriter() throws JSONException {
		JSONDataWriter dataWriter = new JSONDataWriter(getDataSetWriterProperties());
		dataWriter.setLocale(buildLocaleFromSession());
		return dataWriter;
	}

	protected void loadColumnAliasToName(JSONArray jsonArray, Map<String, String> columnAliasToName) throws JSONException {
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject category = jsonArray.getJSONObject(i);
			String alias = category.optString("alias");
			if (alias != null && !alias.isEmpty()) {
				String id = category.optString("id");
				if (id != null && !id.isEmpty()) {
					columnAliasToName.put(alias, id);
				}

				String columnName = category.optString("columnName");
				if (columnName != null && !columnName.isEmpty()) {
					columnAliasToName.put(alias, columnName);
				}
			}
		}
	}

	protected String serializeDataSet(IDataSet dataSet, String typeDocWizard) throws JSONException {
		try {
			JSONObject datasetsJSONObject = (JSONObject) SerializerFactory.getSerializer("application/json").serialize(dataSet, null);
			JSONArray datasetsJSONArray = new JSONArray();
			datasetsJSONArray.put(datasetsJSONObject);
			JSONArray datasetsJSONReturn = putActions(getUserProfile(), datasetsJSONArray, typeDocWizard);
			return datasetsJSONReturn.toString();
		} catch (Throwable t) {
			throw new RuntimeException("An unexpected error occured while serializing results", t);
		}
	}

	/**
	 * @param profile
	 * @param datasetsJSONArray
	 * @param typeDocWizard     Usato dalla my analysis per visualizzare solo i dataset su cui è possi bile costruire un certo tipo di analisi selfservice. Al
	 *                          momento filtra la lista dei dataset solo nel caso del GEO in cui vengono eliminati tutti i dataset che non contengono un
	 *                          riferimento alla dimensione spaziale. Ovviamente il fatto che un metodo che si chiama putActions filtri in modo silente la lista
	 *                          dei dataset è una follia che andrebbe rifattorizzata al più presto.
	 * @return
	 * @throws JSONException
	 * @throws EMFInternalError
	 */
	protected JSONArray putActions(IEngUserProfile profile, JSONArray datasetsJSONArray, String typeDocWizard) throws JSONException, EMFInternalError {

		Engine qbeEngine = null;
		try {
			qbeEngine = ExecuteAdHocUtility.getQbeEngine();
		} catch (SpagoBIRuntimeException r) {
			// the qbe engine is not found
			logger.info("Engine not found. ", r);
		}

		Engine geoEngine = null;
		try {
			geoEngine = ExecuteAdHocUtility.getGeoreportEngine();
		} catch (SpagoBIRuntimeException r) {
			// the geo engine is not found
			logger.info("Engine not found. ", r);
		}

		JSONObject detailAction = new JSONObject();
		detailAction.put("name", "detaildataset");
		detailAction.put("description", "Dataset detail");

		JSONObject deleteAction = new JSONObject();
		deleteAction.put("name", "delete");
		deleteAction.put("description", "Delete dataset");

		JSONObject georeportAction = new JSONObject();
		georeportAction.put("name", "georeport");
		georeportAction.put("description", "Show Map");

		JSONObject qbeAction = new JSONObject();
		qbeAction.put("name", "qbe");
		qbeAction.put("description", "Show Qbe");

		JSONArray datasetsJSONReturn = new JSONArray();
		for (int i = 0; i < datasetsJSONArray.length(); i++) {
			JSONObject datasetJSON = datasetsJSONArray.getJSONObject(i);
			JSONArray actions = new JSONArray();

			if (typeDocWizard == null) {
				actions.put(detailAction);
				if (((UserProfile) profile).getUserId().toString().equals(datasetJSON.get("owner"))) {
					// the delete action is able only for private dataset
					actions.put(deleteAction);
				}
			}

			boolean isGeoDataset = false;

			try {
				// String meta = datasetJSON.getString("meta"); // [A]
				// isGeoDataset = ExecuteAdHocUtility.hasGeoHierarchy(meta); //
				// [A]

				String meta = datasetJSON.optString("meta");

				if (meta != null && !meta.equals(""))
					isGeoDataset = ExecuteAdHocUtility.hasGeoHierarchy(meta);

			} catch (Exception e) {
				logger.error("Error during check of Geo spatial column", e);
			}

			if (isGeoDataset && geoEngine != null) {
				actions.put(georeportAction);
			}

			String dsType = datasetJSON.optString(DataSetConstants.DS_TYPE_CD);
			if (dsType == null || !dsType.equals(DataSetFactory.FEDERATED_DS_TYPE)) {
				if (qbeEngine != null && (typeDocWizard == null || typeDocWizard.equalsIgnoreCase("REPORT"))) {
					if (profile.getFunctionalities() != null && profile.getFunctionalities().contains(SpagoBIConstants.BUILD_QBE_QUERIES_FUNCTIONALITY)) {
						actions.put(qbeAction);
					}
				}
			}

			datasetJSON.put("actions", actions);

			if ("GEO".equalsIgnoreCase(typeDocWizard)) {
				// if is caming from myAnalysis - create Geo Document - must
				// shows only ds geospatial --> isGeoDataset == true
				if (geoEngine != null && isGeoDataset) {
					datasetsJSONReturn.put(datasetJSON);
				}
			} else {
				datasetsJSONReturn.put(datasetJSON);
			}

		}
		return datasetsJSONReturn;
	}

	public String getDataSet(String label) {
		logger.debug("IN");
		try {
			IDataSet dataSet = getDatasetManagementAPI().getDataSet(label);
			return serializeDataSet(dataSet, null);
		} catch (Throwable t) {
			throw new SpagoBIServiceException(this.request.getPathInfo(), "An unexpected error occured while executing service", t);
		} finally {
			logger.debug("OUT");
		}
	}

	public Response deleteDataset(String label) {
		IDataSetDAO datasetDao = DAOFactory.getDataSetDAO();

		IDataSet dataset = getDatasetManagementAPI().getDataSet(label);

		try {
			datasetDao.deleteDataSet(dataset.getId());
		} catch (Exception e) {
			String message = null;
			if (e instanceof DatasetInUseException) {
				DatasetInUseException dui = (DatasetInUseException) e;
				message = dui.getMessage() + "Used by: objects" + dui.getObjectsLabel() + " federations: " + dui.getFederationsLabel();
			} else {
				message = "Error while deleting the specified dataset";
			}

			logger.error("Error while deleting the specified dataset", e);
			throw new SpagoBIRuntimeException(message, e);
		}

		return Response.ok().build();
	}

	public Response execute(String label, String body) {
		SDKDataSetParameter[] parameters = null;

		if (request.getParameterMap() != null && request.getParameterMap().size() > 0) {

			parameters = new SDKDataSetParameter[request.getParameterMap().size()];

			int i = 0;
			for (Iterator iterator = request.getParameterMap().keySet().iterator(); iterator.hasNext();) {
				String key = (String) iterator.next();
				String[] values = request.getParameterMap().get(key);
				SDKDataSetParameter sdkDataSetParameter = new SDKDataSetParameter();
				sdkDataSetParameter.setName(key);
				sdkDataSetParameter.setValues(values);
				parameters[i] = sdkDataSetParameter;
				i++;
			}
		}
		return Response.ok(executeDataSet(label, parameters)).build();
	}

	protected String executeDataSet(String label, SDKDataSetParameter[] params) {
		logger.debug("IN: label in input = " + label);

		try {
			if (label == null) {
				logger.warn("DataSet identifier in input is null!");
				return null;
			}
			IDataSet dataSet = DAOFactory.getDataSetDAO().loadDataSetByLabel(label);
			if (dataSet == null) {
				logger.warn("DataSet with label [" + label + "] not existing.");
				return null;
			}
			if (params != null && params.length > 0) {
				HashMap parametersFilled = new HashMap();
				for (int i = 0; i < params.length; i++) {
					SDKDataSetParameter par = params[i];
					parametersFilled.put(par.getName(), par.getValues()[0]);
					logger.debug("Add parameter: " + par.getName() + "/" + par.getValues()[0]);
				}
				dataSet.setParamsMap(parametersFilled);
			}

			// add the jar retriver in case of a Qbe DataSet
			if (dataSet instanceof QbeDataSet
					|| (dataSet instanceof VersionedDataSet && ((VersionedDataSet) dataSet).getWrappedDataset() instanceof QbeDataSet)) {
				SpagoBICoreDatamartRetriever retriever = new SpagoBICoreDatamartRetriever();
				Map parameters = dataSet.getParamsMap();
				if (parameters == null) {
					parameters = new HashMap();
					dataSet.setParamsMap(parameters);
				}
				dataSet.getParamsMap().put(SpagoBIConstants.DATAMART_RETRIEVER, retriever);
			}
			// get user profile's attributes
			UserProfile userProfile = UserProfileManager.getProfile();
			if (userProfile != null) {
				Map attributes = userProfile.getUserAttributes();
				dataSet.setUserProfileAttributes(attributes);
			}
			dataSet.loadData();

			JSONDataWriter writer = new JSONDataWriter();
			return (writer.write(dataSet.getDataStore())).toString();
		} catch (Exception e) {
			logger.error("Error while executing dataset", e);
			throw new SpagoBIRuntimeException("Error while executing dataset", e);
		}
	}

	protected IDataSetDAO getDataSetDAO() {
		IDataSetDAO dsDAO = DAOFactory.getDataSetDAO();
		return dsDAO;
	}
}
