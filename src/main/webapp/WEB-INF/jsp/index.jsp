<%@ include file="include.jsp"%>
<html>
<head>
<title>NVCL Analytical Services</title>
<link rel="stylesheet" href="style/style.css" type="text/css" />
</head>
<body class="NVCLDSBody">
	<h2 class="NVCLDSh2">NVCL Analytical Services</h2>
	<p>The NVCL Analytical Services provide a bundle of basic
		interfaces for analytical and query from National Virtual Core Library
		datasets. The output of these services is intended to supply a better
		query and analytical tool for use in data access portals</p>
	<p>A few Testcase provided are listed bellow.</p>
	<table class="usageTable">
		<tr>
			<th>To server</th>
			<th>Description</th>
			<th>url</th>
		</tr>

		<tr>
			<td>Submit to nvclwebservice</td>
			<td>logs with the name "Min1 uTSAS" Muscovite greater than 50
				percent within 1 metre span.</td>
			<td><a
				href="submitNVCLAnalyticalJob.do?serviceurls=http://nvclwebservices.vm.csiro.au/geoserverBH/wfs&email=lingbo.jiang@csiro.au&jobname=test001&logname=Min1%20uTSAS&classification=Muscovite&startdepth=0&enddepth=999999&logicalop=gt&value=50&units=pct&span=1">submitNVCLAnalyticalJob.do</a></td>
		</tr>
		<tr>
			<td>Submit to NT</td>
			<td>logs with the name "Min1 sTSAS" dickite greater than 10
				sample count within 1 metre span.</td>
			<td><a
				href="submitNVCLAnalyticalJob.do?serviceurls=http://geology.data.nt.gov.au/geoserver/wfs&email=lingbo.jiang@csiro.au&jobname=test002&logname=Min1%20sTSAS&classification=Dickite&startdepth=0&enddepth=99999&logicalop=gt&value=10&units=count&span=1">submitNVCLAnalyticalJob.do</a></td>
		</tr>
		<tr>
			<td>Submit to NSW</td>
			<td>logs with the name "Min1 sTSAS" dickite greater than 5
				sample count within 99999 metre span.</td>
			<td><a
				href="submitNVCLAnalyticalJob.do?serviceurls=http://auscope.dpi.nsw.gov.au/geoserver/wfs&email=lingbo.jiang@csiro.au&jobname=test003&logname=Min1%20sTSAS&classification=Dickite&startdepth=0&enddepth=99999&logicalop=gt&value=5&units=count&span=99999">submitNVCLAnalyticalJob.do</a></td>
		</tr>
		<tr>
			<td>Submit to NT</td>
			<td>ALL Thermal TSA group (regardless of version) Garnet over 2%
				of entire dataset.</td>
			<td><a
				href="submitNVCLAnalyticalJob.do?serviceurls=http://geology.data.nt.gov.au/geoserver/wfs&email=lingbo.jiang@csiro.au&jobname=test004&classification=GARNET&algorithmoutputid=57,63,69,103&startdepth=0&enddepth=99999&logicalop=gt&value=2&units=pct&span=1">submitNVCLAnalyticalJob.do</a></td>
		</tr>
		<tr>
			<td>Check my job status</td>
			<td>Generate a list of job status in json format.</td>
			<td><a
				href="checkNVCLAnalyticalJobStatus.do?email=lingbo.jiang@csiro.au">checkNVCLAnalyticalJobStatus.do</a></td>
		</tr>
		<tr>
			<td>Gat a job result</td>
			<td>Gat a job result in json format.</td>
			<td><a href="getNVCLAnalyticalJobResult.do?jobid=1">getNVCLAnalyticalJobResult.do</a></td>
		</tr>
		<tr>
			<td>Submit a TSGMod job to nvclWebServices</td>
			<td>Submit a TSGMod job with TsgScript.</td>
			<td><a
				href="submitNVCLTSGModJob.do?serviceurls=http://nvclwebservices.vm.csiro.au/geoserverBH/wfs&email=lingbo.jiang@csiro.au&jobname=test011&logname=Reflectance&classification=Dickite&startdepth=0&enddepth=99999&logicalop=gt&value=10&units=count&span=1">submitNVCLTSGModJobResult.do</a></td>
		</tr>
		<tr>
			<td>Submit a TSGMod job to NT</td>
			<td>Submit a TSGMod job with TsgScript.</td>
			<td><a
				href="submitNVCLTSGModJob.do?serviceurls=http://geology.data.nt.gov.au/geoserver/wfs&email=lingbo.jiang@csiro.au&jobname=test011&logname=Reflectance&classification=Dickite&startdepth=0&enddepth=99999&logicalop=gt&value=10&units=count&span=1">submitNVCLTSGModJobResult.do</a></td>
		</tr>

	</table>


	<p>Click on the urls for more details, including usage instructions
		of each interface.</p>
	<p>
		Click <a
			href="https://twiki.auscope.org/wiki/CoreLibrary/WebServicesDevelopment">here</a>
		for more detail information about NVCL Web Services Development.
	</p>
	<p>Version 0.1.0</p>
	<textarea rows="2" cols="400" style="border: none;">
    Filter sample:
    <ogc:Filter>
			<PropertyIsEqualTo>
			<PropertyName>gsmlp:nvclCollection</PropertyName>
			<Literal>true</Literal></PropertyIsEqualTo>
		</ogc:Filter>
    </textarea>
</body>
</html>

