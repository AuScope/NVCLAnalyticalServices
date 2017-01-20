<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html>
<html>
   <head>
      <link href="https://cdnjs.cloudflare.com/ajax/libs/extjs/6.0.0/classic/theme-classic/resources/theme-classic-all.css" rel="stylesheet" />
      <script type="text/javascript" src="https://cdnjs.cloudflare.com/ajax/libs/extjs/6.0.0/ext-all.js"></script>
      <script type="text/javascript" src="js/NVCLAnalyticsTsgForm.js"></script>

      <script type="text/javascript">      
      
         Ext.onReady(function() {
         Ext.create('NVCLAnalyticsTsgForm',{}).show();
         });
      </script>
   </head>
   <body>
      <div id="helloWorldPanel"></div>
   </body>
</html>