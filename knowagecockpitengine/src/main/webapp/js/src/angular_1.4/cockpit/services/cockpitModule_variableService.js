/*
Knowage, Open Source Business Intelligence suite
Copyright (C) 2019 Engineering Ingegneria Informatica S.p.A.

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
(function() {
	angular.module("cockpitModule")
		.service("cockpitModule_variableService", cockpitModule_variableService)

	function cockpitModule_variableService(sbiModule_translate,sbiModule_user, cockpitModule_template,$q,cockpitModule_analyticalDrivers,cockpitModule_datasetServices, cockpitModule_widgetSelection){
		function formatDataset(dataset){
			tempRows = {};
			dataset.rows.forEach(function(row,i){
				tempRows[row.column_1] = row.column_2;
			})
			return tempRows;
		}

		this.getVariableValue = function(variable){
			return $q(function(resolve, reject) {
//				var selectedVariable;
//				for(var k in cockpitModule_template.configuration.variables){
//					if(cockpitModule_template.configuration.variables[k].name == variableName){
//						selectedVariable = cockpitModule_template.configuration.variables[k];
//						break;
//					}
//				}
				if(variable.type == 'static') resolve( variable.value );
				if(variable.type == 'driver') resolve( cockpitModule_analyticalDrivers[variable.driver] );
				if(variable.type == 'profile') resolve( sbiModule_user.attributes[variable.attribute] );
				if(variable.type == 'dataset') {
					var tempDataset = cockpitModule_datasetServices.getDatasetById(variable.dataset);
					var tempColumn = {content:{columnSelectedOfDataset:[]}}
					for(var j in tempDataset.metadata.fieldsMeta){
						if(variable.column && tempDataset.metadata.fieldsMeta[j].name == variable.column){
							tempColumn.content.columnSelectedOfDataset.push(tempDataset.metadata.fieldsMeta[j]);
							break;
						}else if(!variable.column){
							tempColumn.content.columnSelectedOfDataset.push(tempDataset.metadata.fieldsMeta[j]);
						}
					}
					cockpitModule_datasetServices.loadDatasetRecordsById(variable.dataset, 0, -1,undefined, undefined,tempColumn).then(
						function(response){
							if(variable.column) resolve(response.rows[0].column_1);
							else resolve(formatDataset(response));
						},function(error){
							reject(error)
					})
				}
			})
		}
	}
})();