<#assign cp = springMacroRequestContext.getContextPath()>

<#macro page title>
<!DOCTYPE html>
<html>
<head>
    <title>${title} :: CollateX</title>
    <meta http-equiv="Content-Type" content="text/html;charset=UTF-8">
    <link rel="stylesheet" type="text/css" href="${cp}/static/yui-3.4.0/cssfonts/fonts-min.css">
    <link rel="stylesheet" type="text/css" href="${cp}/static/yui-3.4.0/cssreset/reset-min.css">
    <link rel="stylesheet" type="text/css" href="${cp}/static/yui-3.4.0/cssgrids/grids-min.css">
    <link rel="stylesheet" type="text/css" href="${cp}/static/yui-3.4.0/cssbase/base-min.css">
    <link rel="stylesheet" type="text/css" href="${cp}/static/interedition.css"/>
    <link rel="stylesheet" type="text/css" href="${cp}/static/collatex.css"/>
    <script type="text/javascript">var cp = "${cp?js_string}";</script>
    <script type="text/javascript" src="${cp}/static/yui-3.4.0/yui/yui-min.js"></script>
</head>
<body class="yui3-skin-sam">
<table id="top-bar">
    <tr>
        <td style="width: 60px"><img src="${cp}/static/collatex_logo_small.png" alt="CollateX"></td>
        <td>
            <div id="main-menu" class="yui3-menu yui3-menu-horizontal">
                <div class="yui3-menu-content">
                    <ul>
                        <li class="yui3-menuitem"><a class="yui3-menuitem-content" href="${cp}/">Collate</a></li>
                        <li class="yui3-menuitem"><a class="yui3-menuitem-content" href="${cp}/tutorial">Documentation</a></li>
                        <li class="yui3-menuitem"><a class="yui3-menuitem-content" href="${cp}/examples/darwin">Darwin</a></li>
                    </ul>
                </div>
            </div>
        </td>
    </tr>
</table>
    <#nested>
<script type="text/javascript">
    YUI().use("node", "node-menunav", function(Y) {
        Y.on("contentready", function() {
            this.plug(Y.Plugin.NodeMenuNav, { autoSubmenuDisplay: true, mouseOutHideDelay: 0 });
        }, "#main-menu");
    });
</script>
</body>
</html>
</#macro>