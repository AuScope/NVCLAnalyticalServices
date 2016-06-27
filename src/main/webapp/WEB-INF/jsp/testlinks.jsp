<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Insert title here</title>
</head>
<body>
	<h2 class="NVCLDSh2">Test Links:</h2>
	
	<p>Note: These links are for developer testing.</p>
	<p><a href="submitNVCLAnalyticalJob.do?serviceurls=http://nvclwebservices.vm.csiro.au/geoserverBH/wfs&email=lingbo.jiang@csiro.au&jobname=test001&logname=Min1%20uTSAS&classification=Muscovite&startdepth=0&enddepth=999999&logicalop=gt&value=50&units=pct&span=1">Link1: logs with the name "Min1 uTSAS" Muscovite greater than 50 percent within 1 metre span.</a></p>
	<p><a href="submitNVCLAnalyticalJob.do?serviceurls=http://geology.data.nt.gov.au/geoserver/wfs&email=lingbo.jiang@csiro.au&jobname=test002&logname=Min1%20sTSAS&classification=Dickite&startdepth=0&enddepth=99999&logicalop=gt&value=10&units=count&span=1">Link2: logs with the name "Min1 sTSAS" dickite greater than 10 sample count within 1 metre span.</a></p>
	<p><a href="submitNVCLAnalyticalJob.do?serviceurls=http://auscope.dpi.nsw.gov.au/geoserver/wfs&email=lingbo.jiang@csiro.au&jobname=test003&logname=Min1%20sTSAS&classification=Dickite&startdepth=0&enddepth=99999&logicalop=gt&value=5&units=count&span=99999">Link3: logs with the name "Min1 sTSAS" dickite greater than 5 sample count within 99999 metre span.</a></p>
	<p><a href="submitNVCLAnalyticalJob.do?serviceurls=http://geology.data.nt.gov.au/geoserver/wfs&email=lingbo.jiang@csiro.au&jobname=test004&classification=GARNET&algorithmoutputid=57,63,69,103&startdepth=0&enddepth=99999&logicalop=gt&value=2&units=pct&span=1">Link4: ALL Thermal TSA group (regardless of version) Garnet over 2% of entire dataset.</a></p>
	<p><a href="checkNVCLAnalyticalJobStatus.do?email=lingbo.jiang@csiro.au">Link5: Check my job status.</a></p>
	<p><a href="getNVCLAnalyticalJobResult.do?jobid=1">Link6: Get a job result</a></p>
	
	<p>Link samples:</p>
	<textarea rows="10" cols="400" style="border:none;">
    Filter sample: <ogc:Filter><PropertyIsEqualTo><PropertyName>gsmlp:nvclCollection</PropertyName><Literal>true</Literal></PropertyIsEqualTo></ogc:Filter>
    Link1: submitNVCLAnalyticalJob.do?serviceurls=http://nvclwebservices.vm.csiro.au/geoserverBH/wfs&email=lingbo.jiang@csiro.au&jobname=test001&logname=Min1%20uTSAS&classification=Muscovite&startdepth=0&enddepth=999999&logicalop=gt&value=50&units=pct&span=1
    Link2: submitNVCLAnalyticalJob.do?serviceurls=http://geology.data.nt.gov.au/geoserver/wfs&email=lingbo.jiang@csiro.au&jobname=test002&logname=Min1%20sTSAS&classification=Dickite&startdepth=0&enddepth=99999&logicalop=gt&value=10&units=count&span=1
    Link3: submitNVCLAnalyticalJob.do?serviceurls=http://auscope.dpi.nsw.gov.au/geoserver/wfs&email=lingbo.jiang@csiro.au&jobname=test003&logname=Min1%20sTSAS&classification=Dickite&startdepth=0&enddepth=99999&logicalop=gt&value=5&units=count&span=99999
    Link4: submitNVCLAnalyticalJob.do?serviceurls=http://geology.data.nt.gov.au/geoserver/wfs&email=lingbo.jiang@csiro.au&jobname=test004&classification=GARNET&algorithmoutputid=57,63,69,103&startdepth=0&enddepth=99999&logicalop=gt&value=2&units=pct&span=1
    Link5: checkNVCLAnalyticalJobStatus.do?email=lingbo.jiang@csiro.au    
    Link6: getNVCLAnalyticalJobResult.do?jobid=1 
    </textarea>
    
    
</body>
</html>