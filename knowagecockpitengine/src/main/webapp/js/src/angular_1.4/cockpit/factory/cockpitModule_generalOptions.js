/*
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

angular.module('cockpitModule').factory('cockpitModule_generalOptions',function(sbiModule_config,sbiModule_translate){
	return{
		//Deprecated dimensioning to let the user choose between px,rem and %
		fontSizeDimension: [
			{label:'',value:''},
			{label:'8px',value:'8px'},
			{label:'10px',value:'10px'},
			{label:'12px',value:'12px'},
			{label:'14px',value:'14px'},
			{label:'16px',value:'16px'},
			{label:'20px',value:'20px'},
			{label:'24px',value:'24px'}
		],
		//
		defaultValues : {
				"dateTime": "DD/MM/YYYY HH:mm:ss.SSS",
				"facetDateTime" : "YYYY-MM-DDT00:00:00Z",
			},
		textDecoration:	[
			{label:'',value:''},
			{label:sbiModule_translate.load("sbi.cockpit.style.fontDecoration.overline"),value:'overline'},
			{label:sbiModule_translate.load("sbi.cockpit.style.fontDecoration.underline"),value:'underline'},
			{label:sbiModule_translate.load("sbi.cockpit.style.fontDecoration.lineThrough"),value:'line-through'}
		],
		fontWeight:	[
			{label:'',value:''},
			{label:sbiModule_translate.load("sbi.cockpit.style.fontWeight.regular"),value:'regular'},
			{label:sbiModule_translate.load("sbi.cockpit.style.fontWeight.bold"),value:'bold'},
			{label:sbiModule_translate.load("sbi.cockpit.style.fontWeight.light"),value:'light'}
		],
		fontStyle: [
			{label:'',value:''},
			{label:sbiModule_translate.load("sbi.cockpit.style.fontStyle.normal"),value:'normal'},
			{label:sbiModule_translate.load("sbi.cockpit.style.fontStyle.italic"),value:'italic'},
			{label:sbiModule_translate.load("sbi.cockpit.style.fontStyle.oblique"),value:'oblique'},
		],
		fontFamily: [
			{label:'',value:''},
	        {label:sbiModule_translate.load("sbi.cockpit.style.font.roboto"), value:'roboto'},
	        {label:sbiModule_translate.load("sbi.cockpit.style.font.arial"),value:'arial'},
	        {label:sbiModule_translate.load("sbi.cockpit.style.font.curierNew"),value:'courier-new'},
	        {label:sbiModule_translate.load("sbi.cockpit.style.font.tahoma"),value:'tahoma'},
	        {label:sbiModule_translate.load("sbi.cockpit.style.font.timesNewRoman"),value:'times-new-roman'},
	        {label:sbiModule_translate.load("sbi.cockpit.style.font.verdana"),value:'verdana'}
	     ],
	     aggregationFunctions:[
	    	 {label:sbiModule_translate.load("sbi.qbe.selectgridpanel.aggfunc.name.none"),value:"NONE"},
             {label:sbiModule_translate.load("sbi.qbe.selectgridpanel.aggfunc.name.sum"),value:"SUM"},
             {label:sbiModule_translate.load("sbi.qbe.selectgridpanel.aggfunc.name.avg"),value:"AVG"},
             {label:sbiModule_translate.load("sbi.qbe.selectgridpanel.aggfunc.name.max"),value:"MAX"},
             {label:sbiModule_translate.load("sbi.qbe.selectgridpanel.aggfunc.name.min"),value:"MIN"},
             {label:sbiModule_translate.load("sbi.qbe.selectgridpanel.aggfunc.name.count"),value:"COUNT"},
             {label:sbiModule_translate.load("sbi.qbe.selectgridpanel.aggfunc.name.countdistinct"),value:"COUNT_DISTINCT"}
         ],
         textAlign:	[
			{label:'',value:''},
			{label:sbiModule_translate.load("sbi.cockpit.style.textAlign.left"),value:'left'},
			{label:sbiModule_translate.load("sbi.cockpit.style.textAlign.right"),value:'right'},
			{label:sbiModule_translate.load("sbi.cockpit.style.textAlign.center"),value:'center'}
		 ],
		 textVerticalAlign:	[
			 {label:'',value:''},
			 {label:sbiModule_translate.load("sbi.cockpit.style.textVerticalAlign.top"),value:'top'},
			 {label:sbiModule_translate.load("sbi.cockpit.style.textVerticalAlign.middle"),value:'middle'},
			 {label:sbiModule_translate.load("sbi.cockpit.style.textVerticalAlign.bottom"),value:'bottom'}
		 ],
		 flexJustifyContent:	[
			 {label:'',value:''},
			 {label:sbiModule_translate.load("sbi.cockpit.style.textAlign.left"),value:'flex-start'},
			 {label:sbiModule_translate.load("sbi.cockpit.style.textAlign.center"),value:'center'},
			 {label:sbiModule_translate.load("sbi.cockpit.style.textAlign.right"),value:'flex-end'}
		 ],
		 fieldsTypes:	[
			 {label:'string',value:'java.lang.String'},
			 {label:'integer',value:'java.lang.Integer'},
			 {label:'integer',value:'java.lang.Byte'},
			 {label:'integer',value:'java.lang.Long'},
			 {label:'integer',value:'java.lang.Short'},
			 {label:'integer',value:'java.math.BigInteger'},
			 {label:'float',value:'java.math.BigDecimal'},
			 {label:'float',value:'java.lang.Double'},
			 {label:'float',value:'java.lang.Float'},
			 {label:'date',value:'java.sql.Date'},
			 {label:'date',value:'java.util.Date'},
			 {label:'time',value:'java.sql.Time'},
			 {label:'timestamp',value:'java.sql.Timestamp'},
			 {label:'timestamp',value:'oracle.sql.TIMESTAMP'},
			 {label:'blob',value:'oracle.sql.BLOB'},
			 {label:'clob',value:'oracle.sql.CLOB'}
		 ],

		 typesMap: {
			"java.lang.String": {label:"string",icon:"fa fa-quote-right"},
			"java.lang.Boolean": {label:"boolean",icon:"fa fa-star-half-o"},
			"java.lang.Byte" : {label:"byte",icon:"fa fa-hashtag"},
			"java.lang.Long" : {label:"long",icon:"fa fa-hashtag"},
			"java.lang.Short" : {label:"short",icon:"fa fa-hashtag"},
			"java.lang.Integer": {label:"integer",icon:"fa fa-hashtag"},
			"java.math.BigInteger" :{label:"integer",icon:"fa fa-hashtag"},
			"java.lang.Double": {label:"float",icon:"fa fa-hashtag"},
			"java.lang.Float": {label:"float",icon:"fa fa-hashtag"},
			"java.math.BigDecimal": {label:"float",icon:"fa fa-hashtag"},
			"oracle.sql.TIMESTAMP": {label:'timestamp',icon:'fa fa-calendar'},
			"java.sql.Timestamp": {label:'timestamp',icon:'fa fa-calendar'},
			"java.util.Date": {label:'date',icon:'fa fa-calendar'},
			"java.sql.Date": {label:'date',icon:'fa fa-calendar'},
			"java.sql.Time": {label:'time',icon:'fa fa-clock-o'},
			"oracle.sql.BLOB": {label:'blob',icon:'fa fa-archive'},
			"oracle.sql.CLOB": {label:'clob',icon:'fa fa-archive'}
		},
		 dateFormat: [
			 {label:sbiModule_translate.load("sbi.cockpit.date.format.long"),value:'dd EEEE yyyy HH:mm:ss',hint:'September 03 2010 12:05:08 PM'},
			 {label:sbiModule_translate.load("sbi.cockpit.date.format.medium"),value:'medium',hint:'Sep 3, 2010 12:05:08 PM'},
			 {label:sbiModule_translate.load("sbi.cockpit.date.format.short"),value:'short',hint:'9/3/10 12:05 PM'},
			 {label:sbiModule_translate.load("sbi.cockpit.date.format.fullDate"),value:'fullDate',hint:'Friday, September 3, 2010'},
			 {label:sbiModule_translate.load("sbi.cockpit.date.format.longDate"),value:'longDate',hint:'September 3, 2010'},
			 {label:sbiModule_translate.load("sbi.cockpit.date.format.mediumDate"),value:'mediumDate',hint:'Sep 3, 2010'},
			 {label:sbiModule_translate.load("sbi.cockpit.date.format.shortDate"),value:'shortDate',hint:'09/03/10'},
			 {label:sbiModule_translate.load("sbi.cockpit.date.format.shortDate"),value:'dd/MM/yyyy',hint:'09/03/2010 (dd/mm/yyyy)'},
			 {label:sbiModule_translate.load("sbi.cockpit.date.format.shortDate"),value:'MM/dd/yyyy',hint:'03/09/2010 (mm/dd/yyyy)'},
			 {label:sbiModule_translate.load("sbi.cockpit.date.format.mediumTime"),value:'mediumTime',hint:'12:05:08 PM'},
			 {label:sbiModule_translate.load("sbi.cockpit.date.format.shortTime"),value:'shortTime',hint:'12:05 PM'},
			 {label:sbiModule_translate.load("sbi.cockpit.date.format.shortTime"),value:'yyyy',hint:'2010'}
		 ],
		 momentDateFormat : [
			 {value:'LLLL',hint:moment().locale(sbiModule_config.curr_language).format('LLLL')},
			 {value:'llll',hint:moment().locale(sbiModule_config.curr_language).format('llll')},
			 {value:'LLL',hint:moment().locale(sbiModule_config.curr_language).format('LLL')},
			 {value:'lll',hint:moment().locale(sbiModule_config.curr_language).format('lll')},
			 {value:'DD/MM/YYYY HH:mm:SS',hint:moment().locale(sbiModule_config.curr_language).format('DD/MM/YYYY HH:mm:SS')},
			 {value:'DD/MM/YYYY HH:mm',hint:moment().locale(sbiModule_config.curr_language).format('DD/MM/YYYY HH:mm')},
			 {value:'LL',hint:moment().locale(sbiModule_config.curr_language).format('LL')},
			 {value:'ll',hint:moment().locale(sbiModule_config.curr_language).format('ll')},
			 {value:'L',hint:moment().locale(sbiModule_config.curr_language).format('L')},
			 {value:'l',hint:moment().locale(sbiModule_config.curr_language).format('l')},
			 {value:'LT',hint:moment().locale(sbiModule_config.curr_language).format('LT')},
			 {value:'LTS',hint:moment().locale(sbiModule_config.curr_language).format('LTS')},

		 ]
	}
});