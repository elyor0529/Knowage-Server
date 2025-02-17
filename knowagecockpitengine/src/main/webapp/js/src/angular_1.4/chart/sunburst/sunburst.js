/**
Knowage, Open Source Business Intelligence suite
Copyright (C) 2016 Engineering Ingegneria Informatica S.p.A.

Knowage is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

Knowage is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

function renderHCSunburst(chartConf, handleCockpitSelection, handleCrossNavigationTo, exportWebApp,advanced,chartConfMergeService ) {

    chartConf = prepareChartConfForSunburst(chartConf, handleCockpitSelection, handleCrossNavigationTo, exportWebApp);
    chartConfMergeService.addProperty(advanced,chartConf);
    /**
     * Text that will be displayed inside the Back (drillup) button
     * that appears whenever we enter deeper levels of the TREEMAP
     * chart, i.e. whenever we drilldown through categories for
     * the serie user specified. This way we will keep record of the
     * current drill down level.
     *
     * @author Danilo Ristovski (danristo, danilo.ristovski@mht.net)
     */
    (
		function (H) {
			H.wrap
			(
				H.seriesTypes.sunburst.prototype,
				'showDrillUpButton',

				function (proceed)
				{
					arguments[1] = this.nodeMap[this.rootNode].name;
					proceed.apply(this, [].slice.call(arguments, 1));
				}
			);
		}(Highcharts)
	);
    if (exportWebApp){
    	return chartConf;
    }
	var chart = new Highcharts.Chart(chartConf);

	return chart;
}


function prepareChartConfForSunburst(chartConf, handleCockpitSelection, handleCrossNavigationTo, exportWebApp) {

	var colors = [];
	var defaultColors = Highcharts.getOptions().colors;

	if (chartConf.colors.length == Object.keys(chartConf.data[0]).length) {
		colors = chartConf.colors;
	} else if (chartConf.colors.length > Object.keys(chartConf.data[0]).length) {
		chartConf.colors.length = Object.keys(chartConf.data[0]).length;
		colors = chartConf.colors;
	} else {
		for (var i = 0; i < defaultColors.length; i++) {
			if(defaultColors[i] != 'transparent') {
				colors.push(defaultColors[i])
			}
			if(colors.length == Object.keys(chartConf.data[0]).length){
				break;
			}
		}
	}

	/**
	 * Designing the Center circle of Chart. If the Name of Center circle is not defined
	 * in Chart Design Template (Configuration Tab -> EXPLANATION DETAILS, Text field), put
	 * the value of first category as Center circle name, else put the text that User defined.
	 */
	var centerText = '';

	if(chartConf.tip) {
		var firstLevelStyle = chartConf.tip.style;
		if(chartConf.tip.text != '') {
			centerText = chartConf.tip.text;
		} else {
			centerText = chartConf.categories[0].value;
		}
	}

	var points = [];
	var legendCategories = [];

	precision = chartConf.series.precision ?  chartConf.series.precision : "";
	var center = {
		id: '0.0',
		parent: '',
		name: centerText
	}
	points.push(center);

	var counter=0;

	for (var dataset in chartConf.data[0]){
		level = {
				id: "id_" + counter,
				parent: center.id,
				name: dataset,
				color: colors[counter]
		}
		if(chartConf.legend.showLegend == true) {
			var legendCategorie = {
				id: level.id,
				type: 'area',
				name: level.name,
				color: level.color
			};
			legendCategories.push(legendCategorie);
		}
		counter++;
		points.push(level);
		func(chartConf.data[0][dataset],dataset, level, dataset);
	}

	function func(resultData, nameds, dataValue, dataset){
		var counter=0;
		for (var resultRecord in resultData){
			level = {
					id: dataValue.id + "_" + counter,
					parent: dataValue.id,
					name: resultRecord
			}

			if (resultData[resultRecord].value){
				if(precision==''){
					if(typeof resultData[resultRecord].value == "string"){
						level.value = Number(level.value = resultData[resultRecord].value);
					} else {
						level.value = resultData[resultRecord].value;
					}
				} else {
					level.value = Number(Number(resultData[resultRecord].value).toFixed(precision));
				}
				points.push(level);
			}
			else{
				points.push(level);
				func(resultData[resultRecord], resultRecord, level, dataset);
			}
			counter++;
		}
	}

//	var scale = 0;
//	var tickPositions1 = [];
//	var tickPositions = [];
//	tickPositions.push(0);
//	var scaleObject = {};
//
//	for (var i = 0; i < points.length; i++) {
//		if(scaleObject.hasOwnProperty(points[i].parentName)){
//			scaleObject[(points[i].parentName)].push(points[i]);
//		} else {
//			scaleObject[(points[i].parentName)] = []
//			scaleObject[(points[i].parentName)].push(points[i])
//		}
//	}
//
//
//	var sumaForMax = 0
//	for (property in scaleObject) {
//		var suma = 0
//
//		for (var i = 0; i < scaleObject[property].length; i++) {
//			if(scaleObject[property][i].hasOwnProperty("value")){
//				suma = suma + scaleObject[property][i].value;
//			}
//		}
//		if(sumaForMax <= suma){
//			sumaForMax = suma;
//		}
//		for (var i = 0; i < scaleObject[property].length; i++) {
//			if(scaleObject[property][i].hasOwnProperty("value")){
//				scaleObject[property][i].suma = suma
//			}
//		}
//	}
//
//	var divider = sumaForMax / colors.length;
//	tickPositions1.push(0);
//	var next = 0
//	for (var i = 0; i < colors.length; i++) {
//
//		next = next + divider;
//		tickPositions1.push(next);
//	}
//
//	for (property in scaleObject) {
//		for (var i = 0; i < scaleObject[property].length; i++) {
//			if(scaleObject[property][i].hasOwnProperty("value")){
//				scaleObject[property][i].scale = scale
//			}
//		}
//		scale = scale+divider;
//		tickPositions.push(scale)
//	}
//	for (property in scaleObject) {
//		var colorvalue = [];
//
//		for (var i = 0; i < scaleObject[property].length; i++) {
//			if(scaleObject[property][i].hasOwnProperty("value")){
//				colorvalue.push(scaleObject[property][i]);
//				scaleObject[property][i].colorValue = scaleObject[property][i].value + scaleObject[property][i].scale
//			}
//		}
//	}

	// Splice in transparent for the center circle
	Highcharts.getOptions().colors.splice(0, 0, 'transparent');

	var levels = [];

	// Defining the first level - Center circle
	var firstLevel = {
			level: 1,
			levelIsConstant: false,
			dataLabels: {
				enabled: true,
				formatter: function() {
					var val = this.point.value.toFixed(precision);
			        return this.point.name +  ': <b>' +
			        prefix + " " +val + " " + postfix;
				}
			}
		};

	// Design for Center circle
	if(firstLevelStyle != undefined) {
		firstLevel.dataLabels.style = firstLevelStyle;
	}

	levels.push(firstLevel);

	for(var k = 0; k < chartConf.categories.length; k++) {
		if(k == 0) {
			var lvl = {
				level: k + 2,
				colorByPoint: true
			}
			levels.push(lvl);
		} else {
			var level = {
				level: k + 2,
			    colorVariation: {
	                key: 'brightness',
	                to: 0.5
	            }
			}
			levels.push(level);
		}
	}

	var chartObject = null;

	if (chartConf.chart.height==""
		|| chartConf.chart.width=="")
	{
		chartObject =
		{
			//zoomType: 'xy', // Causes problems when zooming out (Zoom reset) (danristo)
			marginTop: chartConf.chart.marginTop ? chartConf.chart.marginTop : undefined,

			/**
			 * Leave enough space for the "Back" button for drill up.
			 * @author Danilo Ristovski (danristo, danilo.ristovski@mht.net)
			 */
			marginBottom: chartConf.chart.marginBottom ? chartConf.chart.marginBottom : undefined,

			style: {
				fontFamily: chartConf.chart.style.fontFamily,
				fontSize: chartConf.chart.style.fontSize,
				fontWeight: chartConf.chart.style.fontWeight,
				fontStyle: chartConf.chart.style.fontStyle ? chartConf.chart.style.fontStyle : "",
						textDecoration: chartConf.chart.style.textDecoration ? chartConf.chart.style.textDecoration : "",
								fontWeight: chartConf.chart.style.fontWeight ? chartConf.chart.style.fontWeight : ""
			}
		};

		if (chartConf.chart.backgroundColor!=undefined && chartConf.chart.backgroundColor!="")
			chartObject.backgroundColor = chartConf.chart.backgroundColor;
	}
	else if (chartConf.chart.height!=""
		&& chartConf.chart.width!="")
	{
		chartObject =
		{
			//zoomType: 'xy', // Causes problems when zooming out (Zoom reset) (danristo)
			marginTop: chartConf.chart.marginTop ? chartConf.chart.marginTop : undefined,

			/**
			 * Leave enough space for the "Back" button for drill up.
			 * @author Danilo Ristovski (danristo, danilo.ristovski@mht.net)
			 */
			marginBottom: chartConf.chart.marginBottom ? chartConf.chart.marginBottom : undefined,

					style: {
						fontFamily: chartConf.chart.style.fontFamily,
						fontSize: chartConf.chart.style.fontSize,
						fontWeight: chartConf.chart.style.fontWeight,
						fontStyle: chartConf.chart.style.fontStyle ? chartConf.chart.style.fontStyle : "",
								textDecoration: chartConf.chart.style.textDecoration ? chartConf.chart.style.textDecoration : "",
										fontWeight: chartConf.chart.style.fontWeight ? chartConf.chart.style.fontWeight : ""
					}
		};
		if(!exportWebApp){
			chartObject =
			{
				height: chartConf.chart.height ? Number(chartConf.chart.height) : undefined,
				width: chartConf.chart.width ? Number(chartConf.chart.width) : undefined,
			};
		}
		if (chartConf.chart.backgroundColor!=undefined && chartConf.chart.backgroundColor!="")
			chartObject.backgroundColor = chartConf.chart.backgroundColor;
	}
	var tooltipObject={};
	prefix = chartConf.series.prefixChar ? chartConf.series.prefixChar : "";
	postfix = chartConf.series.postfixChar ?  chartConf.series.postfixChar : "";

   	tooltipFormatter = function () {
		var val = this.point.value.toFixed(precision);
        return this.point.name +  ': <b>' +
        prefix + " " +val + " " + postfix;
	};

	tooltipObject = {
		formatter: tooltipFormatter,
	};

	var chart = {
		chart: chartObject,
		tooltip: tooltipObject,
		series:
		[
         	{
			type: "sunburst",
			data: points,
			allowDrillToNode: true,
			cursor: 'pointer',
			showInLegend: false,
			dataLabels: {
				enabled: chartConf.labels.showLabels,
				format: '{point.name}',
				style: chartConf.labels.style
			},
			levels: levels,
			events:{
				// TODO: Cross Navigation functionality on Sunburst Chart
			click: function(event){
					if(!exportWebApp){
						if(chartConf.chart.isCockpit){
							handleCockpitSelection(event);
						 } else if(event.point.node.children.length==0){
				            	var params=getCrossParamsForTreemap(event.point,chartConf);
				            	handleCrossNavigationTo(params);
						 }
					}
				}
			}
		}],
		subtitle: {
			text: chartConf.subtitle.text,
			align: chartConf.subtitle.style.align,
			style: {
				color: chartConf.subtitle.style.fontColor,
				fontSize: chartConf.subtitle.style.fontSize,
				fontFamily: chartConf.subtitle.style.fontFamily,
				fontStyle: chartConf.subtitle.style.fontStyle ? chartConf.subtitle.style.fontStyle : "none",
				textDecoration: chartConf.subtitle.style.textDecoration ? chartConf.subtitle.style.textDecoration : "none",
				fontWeight: chartConf.subtitle.style.fontWeight ? chartConf.subtitle.style.fontWeight : "none"
			}
		},
		title: {
			text: chartConf.title.text,
			align: chartConf.title.style.align,
			style: {
				color: chartConf.title.style.fontColor,
				fontWeight: chartConf.title.style.fontWeight,
				fontSize: chartConf.title.style.fontSize,
				fontFamily: chartConf.title.style.fontFamily,
				fontStyle: chartConf.title.style.fontStyle ? chartConf.title.style.fontStyle : "none",
				textDecoration: chartConf.title.style.textDecoration ? chartConf.title.style.textDecoration : "none",
				fontWeight: chartConf.title.style.fontWeight ? chartConf.title.style.fontWeight : "none"
			}
		},
		lang: {
			noData : chartConf.emptymessage.text
		},
		noData: {
			style: {
                color: chartConf.emptymessage.style.fontColor,
                fontSize: chartConf.emptymessage.style.fontSize,
                fontFamily: chartConf.emptymessage.style.fontFamily,
                fontStyle: chartConf.emptymessage.style.fontStyle ? chartConf.emptymessage.style.fontStyle : "none",
				textDecoration: chartConf.emptymessage.style.textDecoration ? chartConf.emptymessage.style.textDecoration : "none",
				fontWeight: chartConf.emptymessage.style.fontWeight ? chartConf.emptymessage.style.fontWeight : "none"
            },
            position: {
            	align:  chartConf.emptymessage.style.textAlign,
    			verticalAlign: 'middle'
            }
		},


		/**
		 * Credits option disabled/enabled for the SUNBURST chart. This option (boolean value)
		 * is defined inside of the VM for the SUNBURST chart. If enabled credits link appears
		 * in the right bottom part of the chart.
		 * @author: danristo (danilo.ristovski@mht.net)
		 */
		credits:
        {
    		enabled: (chartConf.credits.enabled!=undefined) ? chartConf.credits.enabled : false
		}

	};


	if(legendCategories.length > 0) {
		for(var a = 0; a < legendCategories.length; a++) {
			chart.series.push(legendCategories[a]);

			if(a == legendCategories.length - 1) {
				points.map(function(i) {
				  i.visible = true;
				  return i;
				});

				chart.plotOptions = {
					series: {
						events: {
							 legendItemClick: function(e) {
								   var self = this;
							       points.forEach(function(leaf){
							    	   if (leaf.id === self.userOptions.id || leaf.parent === self.userOptions.id) {
							               leaf.visible = !leaf.visible;
							           }
							       });

							       var newData = points.filter(function(leaf){
							    	   return leaf.visible;
							       }).slice();

							       self.chart.series[0].setData(newData, true);
					         }
						}
					}
				};
			}
		}
	}

	return chart;
}



